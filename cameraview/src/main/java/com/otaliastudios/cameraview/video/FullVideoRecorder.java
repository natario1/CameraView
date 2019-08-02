package com.otaliastudios.cameraview.video;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaRecorder} APIs.
 *
 * When started, the media recorder will be prepared in {@link #onPrepareMediaRecorder(VideoResult.Stub, MediaRecorder)}.
 * Subclasses should override this method and, before calling super(), do two things:
 * - set the media recorder VideoSource
 * - define {@link #mProfile}
 *
 * Subclasses can also call {@link #prepareMediaRecorder(VideoResult.Stub)} before start happens,
 * in which case it will not be prepared twice. This can be used for example to test some
 * configurations.
 */
public abstract class FullVideoRecorder extends VideoRecorder {

    private static final String TAG = FullVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressWarnings("WeakerAccess") protected MediaRecorder mMediaRecorder;
    @SuppressWarnings("WeakerAccess") protected CamcorderProfile mProfile;
    private boolean mMediaRecorderPrepared;


    FullVideoRecorder(@Nullable VideoResultListener listener) {
        super(listener);
    }

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    protected boolean prepareMediaRecorder(@NonNull VideoResult.Stub stub) {
        if (mMediaRecorderPrepared) return true;
        return onPrepareMediaRecorder(stub, new MediaRecorder());
    }

    protected boolean onPrepareMediaRecorder(@NonNull VideoResult.Stub stub, @NonNull MediaRecorder mediaRecorder) {
        mMediaRecorder = mediaRecorder;
        Size size = stub.rotation % 180 != 0 ? stub.size.flip() : stub.size;
        if (stub.audio == Audio.ON || stub.audio == Audio.MONO || stub.audio == Audio.STEREO) {
            // Must be called before setOutputFormat.
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }

        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        if (stub.videoFrameRate <= 0) {
            mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
            stub.videoFrameRate = mProfile.videoFrameRate;
        } else {
            mMediaRecorder.setVideoFrameRate(stub.videoFrameRate);
        }
        mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
        switch (stub.videoCodec) {
            case H_263: mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263); break;
            case H_264: mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); break;
            case DEVICE_DEFAULT: mMediaRecorder.setVideoEncoder(mProfile.videoCodec); break;
        }
        if (stub.videoBitRate <= 0) {
            mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
            stub.videoBitRate = mProfile.videoBitRate;
        } else {
            mMediaRecorder.setVideoEncodingBitRate(stub.videoBitRate);
        }
        if (stub.audio == Audio.ON || stub.audio == Audio.MONO || stub.audio == Audio.STEREO) {
            if (stub.audio == Audio.ON) {
                mMediaRecorder.setAudioChannels(mProfile.audioChannels);
            } else if (stub.audio == Audio.MONO) {
                mMediaRecorder.setAudioChannels(1);
            } else //noinspection ConstantConditions
                if (stub.audio == Audio.STEREO) {
                mMediaRecorder.setAudioChannels(2);
            }
            mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(mProfile.audioCodec);
            if (stub.audioBitRate <= 0) {
                mMediaRecorder.setAudioEncodingBitRate(mProfile.audioBitRate);
                stub.audioBitRate = mProfile.audioBitRate;
            } else {
                mMediaRecorder.setAudioEncodingBitRate(stub.audioBitRate);
            }
        }
        if (stub.location != null) {
            mMediaRecorder.setLocation(
                    (float) stub.location.getLatitude(),
                    (float) stub.location.getLongitude());
        }
        mMediaRecorder.setOutputFile(stub.file.getAbsolutePath());
        mMediaRecorder.setOrientationHint(stub.rotation);
        mMediaRecorder.setMaxFileSize(stub.maxSize);
        mMediaRecorder.setMaxDuration(stub.maxDuration);
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                        mResult.endReason = VideoResult.REASON_MAX_DURATION_REACHED;
                        stop(false);
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        mResult.endReason = VideoResult.REASON_MAX_SIZE_REACHED;
                        stop(false);
                        break;
                }
            }
        });

        try {
            mMediaRecorder.prepare();
            mMediaRecorderPrepared = true;
            mError = null;
            return true;
        } catch (Exception e) {
            LOG.w("prepareMediaRecorder:", "Error while preparing media recorder.", e);
            mMediaRecorderPrepared = false;
            mError = e;
            return false;
        }
    }

    @Override
    protected void onStart() {
        if (!prepareMediaRecorder(mResult)) {
            mResult = null;
            stop(false);
            return;
        }

        try {
            mMediaRecorder.start();
            dispatchVideoRecordingStart();
        } catch (Exception e) {
            LOG.w("start:", "Error while starting media recorder.", e);
            mResult = null;
            mError = e;
            stop(false);
        }
    }

    @Override
    protected void onStop(boolean isCameraShutdown) {
        if (mMediaRecorder != null) {
            dispatchVideoRecordingEnd();
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                LOG.w("stop:", "Error while closing media recorder.", e);
                // This can happen if stopVideo() is called right after takeVideo() (in which case we don't care)
                // Or when prepare()/start() have failed for some reason and we are not allowed to call stop.
                // Make sure we don't override the error if one exists already.
                mResult = null;
                if (mError == null) mError = e;
            }
            mMediaRecorder.release();
        }
        mProfile = null;
        mMediaRecorder = null;
        mMediaRecorderPrepared = false;
        dispatchResult();
    }

}

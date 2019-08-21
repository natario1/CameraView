package com.otaliastudios.cameraview.video;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.VideoCodec;
import com.otaliastudios.cameraview.internal.DeviceEncoders;
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
        boolean hasAudio = stub.audio == Audio.ON
                || stub.audio == Audio.MONO
                || stub.audio == Audio.STEREO;
        if (hasAudio) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }
        mMediaRecorder.setOutputFormat(mProfile.fileFormat);

        // Get the audio mime type
        // https://android.googlesource.com/platform/frameworks/av/+/master/media/libmediaplayerservice/StagefrightRecorder.cpp#1096
        // https://github.com/MrAlex94/Waterfox-Old/blob/master/media/libstagefright/frameworks/av/media/libstagefright/MediaDefs.cpp
        String audioType;
        switch (mProfile.audioCodec) {
            case MediaRecorder.AudioEncoder.AMR_NB: audioType = "audio/3gpp"; break;
            case MediaRecorder.AudioEncoder.AMR_WB: audioType = "audio/amr-wb"; break;
            case MediaRecorder.AudioEncoder.AAC:
            case MediaRecorder.AudioEncoder.HE_AAC:
            case MediaRecorder.AudioEncoder.AAC_ELD: audioType = "audio/mp4a-latm"; break;
            case MediaRecorder.AudioEncoder.VORBIS: audioType = "audio/vorbis"; break;
            case MediaRecorder.AudioEncoder.DEFAULT:
            default: audioType = "audio/3gpp";
        }

        // Get the video mime type
        // https://android.googlesource.com/platform/frameworks/av/+/master/media/libmediaplayerservice/StagefrightRecorder.cpp#1650
        // https://github.com/MrAlex94/Waterfox-Old/blob/master/media/libstagefright/frameworks/av/media/libstagefright/MediaDefs.cpp
        String videoType;
        if (stub.videoCodec == VideoCodec.H_264) mProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
        if (stub.videoCodec == VideoCodec.H_263) mProfile.videoCodec = MediaRecorder.VideoEncoder.H263;
        switch (mProfile.videoCodec) {
            case MediaRecorder.VideoEncoder.H263: videoType = "video/3gpp"; break;
            case MediaRecorder.VideoEncoder.H264: videoType = "video/avc"; break;
            case MediaRecorder.VideoEncoder.MPEG_4_SP: videoType = "video/mp4v-es"; break;
            case MediaRecorder.VideoEncoder.VP8: videoType = "video/x-vnd.on2.vp8"; break;
            case MediaRecorder.VideoEncoder.HEVC: videoType = "video/hevc"; break;
            case MediaRecorder.VideoEncoder.DEFAULT:
            default: videoType = "video/avc";
        }

        // Merge stub and profile
        stub.videoFrameRate = stub.videoFrameRate > 0 ? stub.videoFrameRate : mProfile.videoFrameRate;
        stub.videoBitRate = stub.videoBitRate > 0 ? stub.videoBitRate : mProfile.videoBitRate;
        if (hasAudio) {
            stub.audioBitRate = stub.audioBitRate > 0 ? stub.audioBitRate : mProfile.audioBitRate;
        }

        // Check DeviceEncoders support
        DeviceEncoders encoders = new DeviceEncoders(videoType, audioType, DeviceEncoders.MODE_TAKE_FIRST);
        boolean flip = stub.rotation % 180 != 0;
        if (flip) stub.size = stub.size.flip();
        stub.size = encoders.getSupportedVideoSize(stub.size);
        stub.videoBitRate = encoders.getSupportedVideoBitRate(stub.videoBitRate);
        stub.audioBitRate = encoders.getSupportedAudioBitRate(stub.audioBitRate);
        stub.videoFrameRate = encoders.getSupportedVideoFrameRate(stub.size, stub.videoFrameRate);
        if (flip) stub.size = stub.size.flip();

        // Set video params
        mMediaRecorder.setVideoSize(
                flip ? stub.size.getHeight() : stub.size.getWidth(),
                flip ? stub.size.getWidth() : stub.size.getHeight());
        mMediaRecorder.setVideoFrameRate(stub.videoFrameRate);
        mMediaRecorder.setVideoEncoder(mProfile.videoCodec);
        mMediaRecorder.setVideoEncodingBitRate(stub.videoBitRate);

        // Set audio params
        if (hasAudio) {
            if (stub.audio == Audio.ON) {
                mMediaRecorder.setAudioChannels(mProfile.audioChannels);
            } else if (stub.audio == Audio.MONO) {
                mMediaRecorder.setAudioChannels(1);
            } else if (stub.audio == Audio.STEREO) {
                mMediaRecorder.setAudioChannels(2);
            }
            mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(mProfile.audioCodec);
            mMediaRecorder.setAudioEncodingBitRate(stub.audioBitRate);
        }

        // Set other params
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

        // Prepare the Recorder
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

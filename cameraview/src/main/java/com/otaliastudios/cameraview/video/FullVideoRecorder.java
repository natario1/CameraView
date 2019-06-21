package com.otaliastudios.cameraview.video;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.VideoCodec;
import com.otaliastudios.cameraview.engine.Camera1Engine;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.utils.CamcorderProfiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaRecorder} APIs.
 */
public class FullVideoRecorder extends VideoRecorder {

    private static final String TAG = FullVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private MediaRecorder mMediaRecorder;
    private CamcorderProfile mProfile;
    private Camera1Engine mController;
    private Camera mCamera;
    private Size mSize;

    public FullVideoRecorder(@NonNull VideoResult.Stub stub, @Nullable VideoResultListener listener,
                      @NonNull Camera1Engine controller, @NonNull Camera camera, int cameraId) {
        super(stub, listener);
        mCamera = camera;
        mController = controller;
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setCamera(camera);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Get a profile of quality compatible with the chosen size.
        mSize = mResult.getRotation() % 180 != 0 ? mResult.getSize().flip() : mResult.getSize();
        mProfile = CamcorderProfiles.get(cameraId, mSize);
    }

    // Camera2 constructor here...

    @Override
    void start() {
        if (mResult.getAudio() == Audio.ON) {
            // Must be called before setOutputFormat.
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }

        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        if (mResult.videoFrameRate <= 0) {
            mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
            mResult.videoFrameRate = mProfile.videoFrameRate;
        } else {
            mMediaRecorder.setVideoFrameRate(mResult.videoFrameRate);
        }
        mMediaRecorder.setVideoSize(mSize.getWidth(), mSize.getHeight());
        switch (mResult.getVideoCodec()) {
            case VideoCodec.H_263: mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263); break;
            case VideoCodec.H_264: mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); break;
            case VideoCodec.DEVICE_DEFAULT: mMediaRecorder.setVideoEncoder(mProfile.videoCodec); break;
        }
        if (mResult.videoBitRate <= 0) {
            mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
            mResult.videoBitRate = mProfile.videoBitRate;
        } else {
            mMediaRecorder.setVideoEncodingBitRate(mResult.videoBitRate);
        }
        if (mResult.getAudio() == Audio.ON) {
            mMediaRecorder.setAudioChannels(mProfile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(mProfile.audioCodec);
            if (mResult.audioBitRate <= 0) {
                mMediaRecorder.setAudioEncodingBitRate(mProfile.audioBitRate);
                mResult.audioBitRate = mProfile.audioBitRate;
            } else {
                mMediaRecorder.setAudioEncodingBitRate(mResult.audioBitRate);
            }
        }
        if (mResult.getLocation() != null) {
            mMediaRecorder.setLocation(
                    (float) mResult.getLocation().getLatitude(),
                    (float) mResult.getLocation().getLongitude());
        }
        mMediaRecorder.setOutputFile(mResult.getFile().getAbsolutePath());
        mMediaRecorder.setOrientationHint(mResult.getRotation());
        mMediaRecorder.setMaxFileSize(mResult.getMaxSize());
        mMediaRecorder.setMaxDuration(mResult.getMaxDuration());
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                        mResult.endReason = VideoResult.REASON_MAX_DURATION_REACHED;
                        stop();
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        mResult.endReason = VideoResult.REASON_MAX_SIZE_REACHED;
                        stop();
                        break;
                }
            }
        });
        // Not needed. mMediaRecorder.setPreviewDisplay(mPreview.getSurface());

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (Exception e) {
            LOG.w("stop:", "Error while starting media recorder.", e);
            mResult = null;
            mError = e;
            stop();
        }
    }

    @Override
    void stop() {
        if (mMediaRecorder != null) {
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
            if (mController != null) {
                // Restore frame processing.
                mCamera.setPreviewCallbackWithBuffer(mController);
            }
        }
        mProfile = null;
        mMediaRecorder = null;
        mCamera = null;
        mController = null;
        dispatchResult();
    }
}
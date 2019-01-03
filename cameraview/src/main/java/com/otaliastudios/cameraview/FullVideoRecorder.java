package com.otaliastudios.cameraview;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaRecorder} APIs.
 */
class FullVideoRecorder extends VideoRecorder {

    private static final String TAG = FullVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private MediaRecorder mMediaRecorder;
    private CamcorderProfile mProfile;
    private Camera1 mController;
    private Camera mCamera;

    FullVideoRecorder(@NonNull VideoResult stub, @Nullable VideoResultListener listener,
                      @NonNull Camera1 controller, @NonNull Camera camera, int cameraId) {
        super(stub, listener);
        mCamera = camera;
        mController = controller;
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setCamera(camera);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        // TODO: should get a profile of a quality compatible with the chosen size.
        // Might do this by inspecting mResult.getSize(). However, it is not super important.
        // We are only bound to respect the video size passed by the VideoSizeSelector, and
        // we are doing that below.
    }

    // Camera2 constructor here...

    @Override
    void start() {
        if (mResult.getAudio() == Audio.ON) {
            // Must be called before setOutputFormat.
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }

        Size size = mResult.getRotation() % 180 != 0 ? mResult.getSize().flip() : mResult.getSize();
        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        if (mResult.videoFrameRate <= 0) {
            mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
            mResult.videoFrameRate = mProfile.videoFrameRate;
        } else {
            mMediaRecorder.setVideoFrameRate(mResult.videoFrameRate);
        }
        mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
        switch (mResult.getVideoCodec()) {
            case H_263: mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263); break;
            case H_264: mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); break;
            case DEVICE_DEFAULT: mMediaRecorder.setVideoEncoder(mProfile.videoCodec); break;
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
            mResult = null;
            stop();
        }
    }

    @Override
    void stop() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                // This can happen if stopVideo() is called right after takeVideo(). We don't care.
                mResult = null;
                LOG.w("stop:", "Error while closing media recorder. Swallowing", e);
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

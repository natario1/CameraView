package com.otaliastudios.cameraview;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaCodec} APIs.
 */
class MediaCodecVideoRecorder extends VideoRecorder {

    private static final String TAG = MediaCodecVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    MediaCodecVideoRecorder(VideoResult stub, VideoResultListener listener, Camera camera, int cameraId) {
        super(stub, listener);
    }

    @Override
    void start() {
    }

    @Override
    void close() {

    }
}

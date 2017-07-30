package com.flurgle.camerakit;

import android.support.annotation.Nullable;

abstract class CameraImpl {

    protected final CameraListener mCameraListener;
    protected final PreviewImpl mPreview;

    CameraImpl(CameraListener callback, PreviewImpl preview) {
        mCameraListener = callback;
        mPreview = preview;
    }

    abstract void start();
    abstract void stop();

    abstract void onDisplayOffset(int displayOrientation);
    abstract void onDeviceOrientation(int deviceOrientation);

    abstract void setFacing(@Facing int facing);
    abstract void setFlash(@Flash int flash);
    abstract void setFocus(@Focus int focus);
    abstract void setMethod(@Method int method);
    abstract void setZoom(@ZoomMode int zoom);
    abstract void setVideoQuality(@VideoQuality int videoQuality);
    abstract void setLocation(double latitude, double longitude);

    abstract void captureImage();
    abstract void startVideo();
    abstract void endVideo();

    abstract Size getCaptureSize();
    abstract Size getPreviewSize();
    abstract boolean shouldFlipSizes(); // Wheter the Sizes should be flipped to match the view orientation.
    abstract boolean isCameraOpened();

    @Nullable
    abstract CameraProperties getCameraProperties();

}

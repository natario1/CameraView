package com.flurgle.camerakit;

import android.support.annotation.Nullable;

abstract class CameraImpl {

    protected final CameraView.CameraListenerWrapper mCameraListener;
    protected final PreviewImpl mPreview;

    CameraImpl(CameraView.CameraListenerWrapper callback, PreviewImpl preview) {
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
    abstract void setZoom(@ZoomMode int zoom);
    abstract void setVideoQuality(@VideoQuality int videoQuality);
    abstract void setWhiteBalance(@WhiteBalance int whiteBalance);
    abstract void setSessionType(@SessionType int sessionType);
    abstract void setLocation(double latitude, double longitude);

    abstract void captureImage();
    abstract void startVideo();
    abstract void endVideo();

    abstract Size getCaptureSize();
    abstract Size getPreviewSize();
    abstract boolean shouldFlipSizes(); // Wheter the Sizes should be flipped to match the view orientation.
    abstract boolean isCameraOpened();

    @Nullable
    abstract ExtraProperties getExtraProperties();

}

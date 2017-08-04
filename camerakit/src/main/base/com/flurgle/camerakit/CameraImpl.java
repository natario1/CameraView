package com.flurgle.camerakit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import java.io.File;

abstract class CameraImpl implements PreviewImpl.SurfaceCallback {

    protected final CameraView.CameraCallbacks mCameraListener;
    protected final PreviewImpl mPreview;

    CameraImpl(CameraView.CameraCallbacks callback, PreviewImpl preview) {
        mCameraListener = callback;
        mPreview = preview;
        mPreview.setSurfaceCallback(this);
    }

    abstract void start();
    abstract void stop();

    abstract void onDisplayOffset(int displayOrientation);
    abstract void onDeviceOrientation(int deviceOrientation);

    abstract void setFacing(@Facing int facing);
    abstract void setFlash(@Flash int flash);
    abstract void setFocus(@Focus int focus);
    abstract void setZoomMode(@ZoomMode int zoom);
    abstract void setVideoQuality(@VideoQuality int videoQuality);
    abstract void setWhiteBalance(@WhiteBalance int whiteBalance);
    abstract void setSessionType(@SessionType int sessionType);
    abstract void setLocation(double latitude, double longitude);

    abstract void captureImage();
    abstract void captureSnapshot();
    abstract void startVideo(@NonNull File file);
    abstract void endVideo();

    abstract Size getCaptureSize();
    abstract Size getPreviewSize();
    abstract boolean shouldFlipSizes(); // Wheter the Sizes should be flipped to match the view orientation.
    abstract boolean isCameraOpened();

    abstract void onTouchEvent(MotionEvent event);

    @Nullable
    abstract ExtraProperties getExtraProperties();

}

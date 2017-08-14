package com.otaliastudios.cameraview;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

abstract class CameraController implements Preview.SurfaceCallback {

    protected final CameraView.CameraCallbacks mCameraCallbacks;
    protected final Preview mPreview;

    protected Facing mFacing;
    protected Flash mFlash;
    protected WhiteBalance mWhiteBalance;
    protected VideoQuality mVideoQuality;
    protected SessionType mSessionType;

    CameraController(CameraView.CameraCallbacks callback, Preview preview) {
        mCameraCallbacks = callback;
        mPreview = preview;
        mPreview.setSurfaceCallback(this);
    }

    abstract void start();
    abstract void stop();

    abstract void onDisplayOffset(int displayOrientation);
    abstract void onDeviceOrientation(int deviceOrientation);

    abstract boolean setZoom(float zoom);
    abstract boolean setExposureCorrection(float EVvalue);
    abstract void setFacing(Facing facing);
    abstract void setFlash(Flash flash);
    abstract void setWhiteBalance(WhiteBalance whiteBalance);
    abstract void setVideoQuality(VideoQuality videoQuality);
    abstract void setSessionType(SessionType sessionType);
    abstract void setLocation(double latitude, double longitude);

    abstract boolean capturePicture();
    abstract boolean captureSnapshot();
    abstract boolean startVideo(@NonNull File file);
    abstract boolean endVideo();

    abstract Size getCaptureSize();
    abstract Size getPreviewSize();
    abstract boolean shouldFlipSizes(); // Wheter the Sizes should be flipped to match the view orientation.
    abstract boolean isCameraOpened();

    abstract boolean startAutoFocus(@Nullable Gesture gesture, PointF point);

    @Nullable abstract ExtraProperties getExtraProperties();
    @Nullable abstract CameraOptions getCameraOptions();

    final Facing getFacing() { return mFacing; }
    final Flash getFlash() { return mFlash; }
    final WhiteBalance getWhiteBalance() { return mWhiteBalance; }
    final VideoQuality getVideoQuality() { return mVideoQuality; }
    final SessionType getSessionType() { return mSessionType; }
}

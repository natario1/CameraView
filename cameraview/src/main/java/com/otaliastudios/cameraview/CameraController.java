package com.otaliastudios.cameraview;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

abstract class CameraController implements Preview.SurfaceCallback {

    protected final CameraView.CameraCallbacks mCameraCallbacks;
    protected final Preview mPreview;

    @Facing protected int mFacing;
    @Flash protected int mFlash;
    @Focus protected int mFocus;
    @VideoQuality protected int mVideoQuality;
    @WhiteBalance protected int mWhiteBalance;
    @SessionType protected int mSessionType;

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
    abstract void setFacing(@Facing int facing);
    abstract void setFlash(@Flash int flash);
    abstract void setFocus(@Focus int focus);
    abstract void setVideoQuality(@VideoQuality int videoQuality);
    abstract void setWhiteBalance(@WhiteBalance int whiteBalance);
    abstract void setSessionType(@SessionType int sessionType);
    abstract void setLocation(double latitude, double longitude);

    abstract void capturePicture();
    abstract void captureSnapshot();
    abstract boolean startVideo(@NonNull File file);
    abstract void endVideo();

    abstract Size getCaptureSize();
    abstract Size getPreviewSize();
    abstract boolean shouldFlipSizes(); // Wheter the Sizes should be flipped to match the view orientation.
    abstract boolean isCameraOpened();

    abstract void startFocus(float x, float y);

    @Nullable abstract ExtraProperties getExtraProperties();
    @Nullable abstract CameraOptions getCameraOptions();

    @Facing
    final int getFacing() {
        return mFacing;
    }

    @Flash
    final int getFlash() {
        return mFlash;
    }

    @Focus
    final int getFocus() {
        return mFocus;
    }

    @WhiteBalance
    final int getWhiteBalance() {
        return mWhiteBalance;
    }

    @VideoQuality
    final int getVideoQuality() {
        return mVideoQuality;
    }

    @SessionType
    final int getSessionType() {
        return mSessionType;
    }
}

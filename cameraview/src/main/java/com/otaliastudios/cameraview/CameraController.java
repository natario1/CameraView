package com.otaliastudios.cameraview;

import android.graphics.PointF;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.io.File;

abstract class CameraController implements Preview.SurfaceCallback {

    protected final CameraView.CameraCallbacks mCameraCallbacks;
    protected final Preview mPreview;

    protected Facing mFacing;
    protected Flash mFlash;
    protected WhiteBalance mWhiteBalance;
    protected VideoQuality mVideoQuality;
    protected SessionType mSessionType;
    protected Hdr mHdr;

    protected Size mCaptureSize;
    protected Size mPreviewSize;

    protected ExtraProperties mExtraProperties;
    protected CameraOptions mOptions;

    protected int mDisplayOffset;
    protected int mDeviceOrientation;

    protected WorkerHandler mHandler;

    CameraController(CameraView.CameraCallbacks callback, Preview preview) {
        mCameraCallbacks = callback;
        mPreview = preview;
        mPreview.setSurfaceCallback(this);
        mHandler = new WorkerHandler("CameraViewController");
    }

    //region Start&Stop

    // Starts the preview asynchronously.
    final void start() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onStart();
            }
        });
    }

    // Stops the preview asynchronously.
    final void stop() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onStop();
            }
        });
    }

    // Starts the preview.
    @WorkerThread
    abstract void onStart();

    // Stops the preview.
    @WorkerThread
    abstract void onStop();

    //endregion

    //region Rotation callbacks

    void onDisplayOffset(int displayOrientation) {
        // I doubt this will ever change.
        mDisplayOffset = displayOrientation;
    }

    void onDeviceOrientation(int deviceOrientation) {
        mDeviceOrientation = deviceOrientation;
    }

    //endregion

    //region Abstract setParameters

    abstract boolean setZoom(float zoom);

    abstract boolean setExposureCorrection(float EVvalue);

    abstract void setFacing(Facing facing);

    abstract void setFlash(Flash flash);

    abstract void setWhiteBalance(WhiteBalance whiteBalance);

    abstract void setVideoQuality(VideoQuality videoQuality);

    abstract void setSessionType(SessionType sessionType);

    abstract void setHdr(Hdr hdr);

    abstract void setLocation(Location location);

    //endregion

    //region APIs

    abstract boolean capturePicture();

    abstract boolean captureSnapshot();

    abstract boolean startVideo(@NonNull File file);

    abstract boolean endVideo();


    abstract boolean shouldFlipSizes(); // Wheter the Sizes should be flipped to match the view orientation.

    abstract boolean isCameraOpened();

    abstract boolean startAutoFocus(@Nullable Gesture gesture, PointF point);

    //endregion

    //region final getters

    @Nullable
    final ExtraProperties getExtraProperties() {
        return mExtraProperties;
    }

    @Nullable
    final CameraOptions getCameraOptions() {
        return mOptions;
    }

    final Facing getFacing() {
        return mFacing;
    }

    final Flash getFlash() {
        return mFlash;
    }

    final WhiteBalance getWhiteBalance() {
        return mWhiteBalance;
    }

    final VideoQuality getVideoQuality() {
        return mVideoQuality;
    }

    final SessionType getSessionType() {
        return mSessionType;
    }

    final Hdr getHdr() {
        return mHdr;
    }

    final Size getCaptureSize() {
        return mCaptureSize;
    }

    final Size getPreviewSize() {
        return mPreviewSize;
    }

    //endregion
}

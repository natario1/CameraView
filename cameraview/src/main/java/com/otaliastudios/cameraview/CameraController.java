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

    private WorkerHandler mHandler;

    CameraController(CameraView.CameraCallbacks callback, Preview preview) {
        mCameraCallbacks = callback;
        mPreview = preview;
        mPreview.setSurfaceCallback(this);
        mHandler = new WorkerHandler("CameraViewController");
    }

    protected final void post(Runnable runnable) {
        mHandler.post(runnable);
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

    abstract void onDisplayOffset(int displayOrientation);
    abstract void onDeviceOrientation(int deviceOrientation);

    abstract boolean setZoom(float zoom);
    abstract boolean setExposureCorrection(float EVvalue);
    abstract void setFacing(Facing facing);
    abstract void setFlash(Flash flash);
    abstract void setWhiteBalance(WhiteBalance whiteBalance);
    abstract void setVideoQuality(VideoQuality videoQuality);
    abstract void setSessionType(SessionType sessionType);
    abstract void setHdr(Hdr hdr);
    abstract void setLocation(Location location);

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
    final Hdr getHdr() { return mHdr; }
}

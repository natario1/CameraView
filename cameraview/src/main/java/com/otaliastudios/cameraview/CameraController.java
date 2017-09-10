package com.otaliastudios.cameraview;

import android.graphics.PointF;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.io.File;

abstract class CameraController implements Preview.SurfaceCallback {

    private static final String TAG = CameraController.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    static final int STATE_STOPPING = -1; // Camera is about to be stopped.
    static final int STATE_STOPPED = 0; // Camera is stopped.
    static final int STATE_STARTING = 1; // Camera is about to start.
    static final int STATE_STARTED = 2; // Camera is available and we can set parameters.

    protected final CameraView.CameraCallbacks mCameraCallbacks;
    protected final Preview mPreview;

    protected Facing mFacing;
    protected Flash mFlash;
    protected WhiteBalance mWhiteBalance;
    protected VideoQuality mVideoQuality;
    protected SessionType mSessionType;
    protected Hdr mHdr;
    protected Location mLocation;

    protected Size mCaptureSize;
    protected Size mPreviewSize;

    protected ExtraProperties mExtraProperties;
    protected CameraOptions mOptions;

    protected int mDisplayOffset;
    protected int mDeviceOrientation;
    protected int mState = STATE_STOPPED;

    protected WorkerHandler mHandler;

    CameraController(CameraView.CameraCallbacks callback, Preview preview) {
        mCameraCallbacks = callback;
        mPreview = preview;
        mPreview.setSurfaceCallback(this);
        mHandler = WorkerHandler.get("CameraViewController");
        mHandler.getThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                // Something went wrong. Thread is terminated (about to?).
                // Move to other thread and stop resources.
                WorkerHandler.clearCache();
                mHandler = WorkerHandler.get("CameraViewController");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopImmediately();
                    }
                });
            }
        });
    }

    //region Start&Stop

    // Starts the preview asynchronously.
    final void start() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    int a = mState;
                    if (mState >= STATE_STARTING) return;
                    mState = STATE_STARTING;
                    onStart();
                    mState = STATE_STARTED;
                    mCameraCallbacks.dispatchOnCameraOpened(mOptions);

                } catch (Exception e) {
                    LOG.e("Error while starting the camera engine.", e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    // Stops the preview asynchronously.
    final void stop() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    int a = mState;
                    if (mState <= STATE_STOPPED) return;
                    mState = STATE_STOPPING;
                    onStop();
                    mState = STATE_STOPPED;
                    mCameraCallbacks.dispatchOnCameraClosed();

                } catch (Exception e) {
                    LOG.e("Error while stopping the camera engine.", e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    // Stops the preview synchronously, ensuring no exceptions are thrown.
    void stopImmediately() {
        try {
            // Don't check, try stop again.
            mState = STATE_STOPPING;
            onStop();
            mState = STATE_STOPPED;
        } catch (Exception e) {
            // Do nothing.
            mState = STATE_STOPPED;
        }
    }

    // Forces a restart.
    protected final void restart() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // Don't stop if stopped.
                    if (mState > STATE_STOPPED) {
                        mState = STATE_STOPPING;
                        onStop();
                        mState = STATE_STOPPED;
                        mCameraCallbacks.dispatchOnCameraClosed();
                    }
                    mState = STATE_STARTING;
                    onStart();
                    mState = STATE_STARTED;
                    mCameraCallbacks.dispatchOnCameraOpened(mOptions);

                } catch (Exception e) {
                    LOG.e("Error while restarting the camera engine.", e);
                    throw new RuntimeException(e);

                }
            }
        });
    }

    // Starts the preview.
    // At the end of this method camera must be available, e.g. for setting parameters.
    @WorkerThread
    abstract void onStart() throws Exception;

    // Stops the preview.
    @WorkerThread
    abstract void onStop() throws Exception;

    // Returns current state.
    final int getState() {
        return mState;
    }


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

    // Should restart the session if active.
    abstract void setSessionType(SessionType sessionType);

    // Should restart the session if active.
    abstract void setFacing(Facing facing);

    // If opened and supported, apply and return true.
    abstract boolean setZoom(float zoom);

    // If opened and supported, apply and return true.
    abstract boolean setExposureCorrection(float EVvalue);

    // If closed, keep. If opened, check supported and apply.
    abstract void setFlash(Flash flash);

    // If closed, keep. If opened, check supported and apply.
    abstract void setWhiteBalance(WhiteBalance whiteBalance);

    // If closed, keep. If opened, check supported and apply.
    abstract void setHdr(Hdr hdr);

    // If closed, keep. If opened, check supported and apply.
    abstract void setLocation(Location location);

    // Throw if capturing. If in video session, recompute capture size, and, if needed, preview size.
    abstract void setVideoQuality(VideoQuality videoQuality);


    //endregion

    //region APIs

    abstract boolean capturePicture();

    abstract boolean captureSnapshot();

    abstract boolean startVideo(@NonNull File file);

    abstract boolean endVideo();

    abstract boolean shouldFlipSizes(); // Wheter the Sizes should be flipped to match the view orientation.

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

    final Location getLocation() {
        return mLocation;
    }

    final Size getCaptureSize() {
        return mCaptureSize;
    }

    final Size getPreviewSize() {
        return mPreviewSize;
    }

    //endregion
}

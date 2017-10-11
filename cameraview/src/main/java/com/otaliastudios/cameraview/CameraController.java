package com.otaliastudios.cameraview;

import android.graphics.PointF;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.io.File;

abstract class CameraController implements CameraPreview.SurfaceCallback {

    private static final String TAG = CameraController.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    static final int STATE_STOPPING = -1; // Camera is about to be stopped.
    static final int STATE_STOPPED = 0; // Camera is stopped.
    static final int STATE_STARTING = 1; // Camera is about to start.
    static final int STATE_STARTED = 2; // Camera is available and we can set parameters.

    protected final CameraView.CameraCallbacks mCameraCallbacks;
    protected CameraPreview mPreview;

    protected Facing mFacing;
    protected Flash mFlash;
    protected WhiteBalance mWhiteBalance;
    protected VideoQuality mVideoQuality;
    protected SessionType mSessionType;
    protected Hdr mHdr;
    protected Location mLocation;
    protected Audio mAudio;

    protected Size mCaptureSize;
    protected Size mPreviewSize;

    protected ExtraProperties mExtraProperties;
    protected CameraOptions mOptions;

    protected int mDisplayOffset;
    protected int mDeviceOrientation;

    protected boolean mScheduledForStart = false;
    protected boolean mScheduledForStop = false;
    protected boolean mScheduledForRestart = false;
    protected int mState = STATE_STOPPED;

    protected WorkerHandler mHandler;

    CameraController(CameraView.CameraCallbacks callback) {
        mCameraCallbacks = callback;
        mHandler = WorkerHandler.get("CameraViewController");
        mHandler.getThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                // Something went wrong. Thread is terminated (about to?).
                // Move to other thread and stop resources.
                LOG.w("Interrupting thread, due to exception.", throwable);
                thread.interrupt();
                LOG.w("Interrupted thread. Posting a stopImmediately.", ss());
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

    void setPreview(CameraPreview cameraPreview) {
        mPreview = cameraPreview;
        mPreview.setSurfaceCallback(this);
    }

    //region Start&Stop

    private String ss() {
        switch (mState) {
            case STATE_STOPPING: return "STATE_STOPPING";
            case STATE_STOPPED: return "STATE_STOPPED";
            case STATE_STARTING: return "STATE_STARTING";
            case STATE_STARTED: return "STATE_STARTED";
        }
        return "null";
    }

    // Starts the preview asynchronously.
    final void start() {
        LOG.i("Start:", "posting runnable. State:", ss());
        mScheduledForStart = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.i("Start:", "executing. State:", ss());
                    mScheduledForStart = false;
                    if (mState >= STATE_STARTING) return;
                    mState = STATE_STARTING;
                    LOG.i("Start:", "about to call onStart()", ss());
                    onStart();
                    LOG.i("Start:", "returned from onStart().", "Dispatching.", ss());
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
        LOG.i("Stop:", "posting runnable. State:", ss());
        mScheduledForStop = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.i("Stop:", "executing. State:", ss());
                    mScheduledForStop = false;
                    if (mState <= STATE_STOPPED) return;
                    mState = STATE_STOPPING;
                    LOG.i("Stop:", "about to call onStop()");
                    onStop();
                    LOG.i("Stop:", "returned from onStop().", "Dispatching.");
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
            LOG.i("Stop immediately. State was:", ss());
            mState = STATE_STOPPING;
            onStop();
            mState = STATE_STOPPED;
            LOG.i("Stop immediately. Stopped. State is:", ss());
        } catch (Exception e) {
            // Do nothing.
            LOG.i("Stop immediately. Exception while stopping.", e);
            mState = STATE_STOPPED;
        }
    }

    // Forces a restart.
    protected final void restart() {
        LOG.i("Restart:", "posting runnable");
        mScheduledForRestart = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.i("Restart:", "executing. Needs stopping:", mState > STATE_STOPPED, ss());
                    mScheduledForRestart = false;
                    // Don't stop if stopped.
                    if (mState > STATE_STOPPED) {
                        mState = STATE_STOPPING;
                        onStop();
                        mState = STATE_STOPPED;
                        LOG.i("Restart:", "stopped. Dispatching.", ss());
                        mCameraCallbacks.dispatchOnCameraClosed();
                    }

                    LOG.i("Restart: about to start. State:", ss());
                    mState = STATE_STARTING;
                    onStart();
                    mState = STATE_STARTED;
                    LOG.i("Restart: returned from start. Dispatching. State:", ss());
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

    // Just set.
    abstract void setAudio(Audio audio);

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

    final Audio getAudio() {
        return mAudio;
    }

    final Size getCaptureSize() {
        return mCaptureSize;
    }

    final Size getPreviewSize() {
        return mPreviewSize;
    }

    //endregion
}

package com.otaliastudios.cameraview;

import android.graphics.PointF;
import android.location.Location;


import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.io.File;
import java.util.List;

abstract class CameraController implements
        CameraPreview.SurfaceCallback,
        FrameManager.BufferCallback,
        Thread.UncaughtExceptionHandler {

    private static final String TAG = CameraController.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    static final int STATE_STOPPING = -1; // Camera is about to be stopped.
    static final int STATE_STOPPED = 0; // Camera is stopped.
    static final int STATE_STARTING = 1; // Camera is about to start.
    static final int STATE_STARTED = 2; // Camera is available and we can set parameters.

    protected final CameraView.CameraCallbacks mCameraCallbacks;
    protected CameraPreview mPreview;
    protected WorkerHandler mHandler;
    /* for tests */ Handler mCrashHandler;
    protected int mCameraId;

    protected Facing mFacing;
    protected Flash mFlash;
    protected WhiteBalance mWhiteBalance;
    protected VideoQuality mVideoQuality;
    protected SessionType mSessionType;
    protected Hdr mHdr;
    protected Location mLocation;
    protected Audio mAudio;

    protected float mZoomValue;
    protected float mExposureCorrectionValue;

    protected Size mPictureSize;
    protected Size mPreviewSize;
    protected int mPreviewFormat;

    protected ExtraProperties mExtraProperties;
    protected CameraOptions mOptions;
    protected FrameManager mFrameManager;
    protected SizeSelector mPictureSizeSelector;

    protected int mDisplayOffset;
    protected int mDeviceOrientation;
    protected int mState = STATE_STOPPED;

    // Used for testing.
    Task<Void> mZoomTask = new Task<>();
    Task<Void> mExposureCorrectionTask = new Task<>();
    Task<Void> mFlashTask = new Task<>();
    Task<Void> mWhiteBalanceTask = new Task<>();
    Task<Void> mHdrTask = new Task<>();
    Task<Void> mLocationTask = new Task<>();
    Task<Void> mVideoQualityTask = new Task<>();
    Task<Void> mStartVideoTask = new Task<>();

    CameraController(CameraView.CameraCallbacks callback) {
        mCameraCallbacks = callback;
        mCrashHandler = new Handler(Looper.getMainLooper());
        mHandler = WorkerHandler.get("CameraViewController");
        mHandler.getThread().setUncaughtExceptionHandler(this);
        mFrameManager = new FrameManager(2, this);
    }

    void setPreview(CameraPreview cameraPreview) {
        mPreview = cameraPreview;
        mPreview.setSurfaceCallback(this);
    }

    //region Error handling

    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        // Something went wrong. Thread is terminated (about to?).
        // Move to other thread and release resources.
        if (!(throwable instanceof CameraException)) {
            // This is unexpected, either a bug or something the developer should know.
            // Release and crash the UI thread so we get bug reports.
            LOG.e("uncaughtException:", "Unexpected exception:", throwable);
            destroy();
            mCrashHandler.post(new Runnable() {
                @Override
                public void run() {
                    RuntimeException exception;
                    if (throwable instanceof RuntimeException) {
                        exception = (RuntimeException) throwable;
                    } else {
                        exception = new RuntimeException(throwable);
                    }
                    throw exception;
                }
            });
        } else {
            // At the moment all CameraExceptions are unrecoverable, there was something
            // wrong when starting, stopping, or binding the camera to the preview.
            final CameraException error = (CameraException) throwable;
            LOG.e("uncaughtException:", "Interrupting thread with state:", ss(), "due to CameraException:", error);
            thread.interrupt();
            mHandler = WorkerHandler.get("CameraViewController");
            mHandler.getThread().setUncaughtExceptionHandler(this);
            LOG.i("uncaughtException:", "Calling stopImmediately and notifying.");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopImmediately();
                    mCameraCallbacks.dispatchError(error);
                }
            });
        }
    }

    final void destroy() {
        LOG.i("destroy:", "state:", ss());
        // Prevent CameraController leaks.
        mHandler.getThread().setUncaughtExceptionHandler(null);
        // Stop if needed.
        stopImmediately();
    }

    //endregion

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
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                LOG.i("Start:", "executing. State:", ss());
                if (mState >= STATE_STARTING) return;
                mState = STATE_STARTING;
                LOG.i("Start:", "about to call onStart()", ss());
                onStart();
                LOG.i("Start:", "returned from onStart().", "Dispatching.", ss());
                mState = STATE_STARTED;
                mCameraCallbacks.dispatchOnCameraOpened(mOptions);
            }
        });
    }

    // Stops the preview asynchronously.
    final void stop() {
        LOG.i("Stop:", "posting runnable. State:", ss());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                LOG.i("Stop:", "executing. State:", ss());
                if (mState <= STATE_STOPPED) return;
                mState = STATE_STOPPING;
                LOG.i("Stop:", "about to call onStop()");
                onStop();
                LOG.i("Stop:", "returned from onStop().", "Dispatching.");
                mState = STATE_STOPPED;
                mCameraCallbacks.dispatchOnCameraClosed();
            }
        });
    }

    // Stops the preview synchronously, ensuring no exceptions are thrown.
    final void stopImmediately() {
        try {
            // Don't check, try stop again.
            LOG.i("stopImmediately:", "State was:", ss());
            if (mState == STATE_STOPPED) return;
            mState = STATE_STOPPING;
            onStop();
            mState = STATE_STOPPED;
            LOG.i("stopImmediately:", "Stopped. State is:", ss());
        } catch (Exception e) {
            // Do nothing.
            LOG.i("stopImmediately:", "Swallowing exception while stopping.", e);
            mState = STATE_STOPPED;
        }
    }

    // Forces a restart.
    protected final void restart() {
        LOG.i("Restart:", "posting runnable");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                LOG.i("Restart:", "executing. Needs stopping:", mState > STATE_STOPPED, ss());
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
            }
        });
    }

    // Starts the preview.
    // At the end of this method camera must be available, e.g. for setting parameters.
    @WorkerThread
    abstract void onStart();

    // Stops the preview.
    @WorkerThread
    abstract void onStop();

    // Returns current state.
    final int getState() {
        return mState;
    }

    //endregion

    //region Simple setters

    void onDisplayOffset(int displayOrientation) {
        // I doubt this will ever change.
        mDisplayOffset = displayOrientation;
    }

    void onDeviceOrientation(int deviceOrientation) {
        mDeviceOrientation = deviceOrientation;
    }

    void setPictureSizeSelector(SizeSelector selector) {
        mPictureSizeSelector = selector;
    }

    //endregion

    //region Abstract setters

    // Should restart the session if active.
    abstract void setSessionType(SessionType sessionType);

    // Should restart the session if active.
    abstract void setFacing(Facing facing);

    // If closed, no-op. If opened, check supported and apply.
    abstract void setZoom(float zoom, PointF[] points, boolean notify);

    // If closed, no-op. If opened, check supported and apply.
    abstract void setExposureCorrection(float EVvalue, float[] bounds, PointF[] points, boolean notify);

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

    abstract void capturePicture();

    abstract void captureSnapshot();

    abstract void startVideo(@NonNull File file);

    abstract void endVideo();

    abstract boolean shouldFlipSizes(); // Wheter the Sizes should be flipped to match the view orientation.

    abstract void startAutoFocus(@Nullable Gesture gesture, PointF point);

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

    final SizeSelector getPictureSizeSelector() {
        return mPictureSizeSelector;
    }

    final Size getPictureSize() {
        return mPictureSize;
    }

    final float getZoomValue() {
        return mZoomValue;
    }

    final float getExposureCorrectionValue() {
        return mExposureCorrectionValue;
    }

    final Size getPreviewSize() {
        return mPreviewSize;
    }

    //endregion

    //region Size utils

    /**
     * This is called either on cameraView.start(), or when the underlying surface changes.
     * It is possible that in the first call the preview surface has not already computed its
     * dimensions.
     * But when it does, the {@link CameraPreview.SurfaceCallback} should be called,
     * and this should be refreshed.
     */
    protected Size computePictureSize(List<Size> captureSizes) {
        SizeSelector selector;

        // The external selector is expecting stuff in the view world, not in the sensor world.
        // Flip before starting, and then flip again.
        boolean flip = shouldFlipSizes();
        LOG.i("computePictureSize:", "flip:", flip);
        if (flip) {
            for (Size size : captureSizes) {
                size.flip();
            }
        }

        if (mSessionType == SessionType.PICTURE) {
            selector = SizeSelectors.or(
                    mPictureSizeSelector,
                    SizeSelectors.biggest() // Fallback to biggest.
            );
        } else {
            // The Camcorder internally checks for cameraParameters.getSupportedVideoSizes() etc.
            // And we want the picture size to be the biggest picture consistent with the video aspect ratio.
            // -> Use the external picture selector, but enforce the ratio constraint.
            CamcorderProfile profile = getCamcorderProfile();
            AspectRatio targetRatio = AspectRatio.of(profile.videoFrameWidth, profile.videoFrameHeight);
            if (flip) targetRatio = targetRatio.inverse();
            LOG.i("size:", "computeCaptureSize:", "videoQuality:", mVideoQuality, "targetRatio:", targetRatio);
            SizeSelector matchRatio = SizeSelectors.aspectRatio(targetRatio, 0);
            selector = SizeSelectors.or(
                    SizeSelectors.and(matchRatio, mPictureSizeSelector),
                    SizeSelectors.and(matchRatio),
                    mPictureSizeSelector
            );
        }

        Size result = selector.select(captureSizes).get(0);
        LOG.i("computePictureSize:", "result:", result);
        if (flip) result.flip();
        return result;
    }

    protected Size computePreviewSize(List<Size> previewSizes) {
        AspectRatio targetRatio = AspectRatio.of(mPictureSize.getWidth(), mPictureSize.getHeight());
        Size targetMinSize = mPreview.getSurfaceSize();
        LOG.i("size:", "computePreviewSize:", "targetRatio:", targetRatio, "targetMinSize:", targetMinSize);
        SizeSelector matchRatio = SizeSelectors.aspectRatio(targetRatio, 0);
        SizeSelector matchSize = SizeSelectors.and(
                SizeSelectors.minHeight(targetMinSize.getHeight()),
                SizeSelectors.minWidth(targetMinSize.getWidth()));
        SizeSelector matchAll = SizeSelectors.or(
                SizeSelectors.and(matchRatio, matchSize),
                matchRatio, // If couldn't match both, match ratio.
                SizeSelectors.biggest() // If couldn't match any, take the biggest.
        );
        return matchAll.select(previewSizes).get(0);
    }

    @NonNull
    protected CamcorderProfile getCamcorderProfile() {
        switch (mVideoQuality) {
            case HIGHEST:
                return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_HIGH);

            case MAX_2160P:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                        CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_2160P)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_2160P);
                }
                // Don't break.

            case MAX_1080P:
                if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_1080P)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_1080P);
                }
                // Don't break.

            case MAX_720P:
                if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_720P)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_720P);
                }
                // Don't break.

            case MAX_480P:
                if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_480P);
                }
                // Don't break.

            case MAX_QVGA:
                if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_QVGA)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_QVGA);
                }
                // Don't break.

            case LOWEST:
            default:
                // Fallback to lowest.
                return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_LOW);
        }
    }

    //endregion
}

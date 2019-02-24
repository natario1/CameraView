package com.otaliastudios.cameraview;

import android.graphics.PointF;
import android.location.Location;


import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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

    static final int REF_SENSOR = 0;
    static final int REF_VIEW = 1;
    static final int REF_OUTPUT = 2;

    protected final CameraView.CameraCallbacks mCameraCallbacks;
    protected CameraPreview mPreview;
    protected WorkerHandler mHandler;
    /* for tests */ Handler mCrashHandler;

    protected Facing mFacing;
    protected Flash mFlash;
    protected WhiteBalance mWhiteBalance;
    protected VideoCodec mVideoCodec;
    protected Mode mMode;
    protected Hdr mHdr;
    protected Location mLocation;
    protected Audio mAudio;
    protected float mZoomValue;
    protected float mExposureCorrectionValue;
    protected boolean mPlaySounds;

    @Nullable private SizeSelector mPreviewStreamSizeSelector;
    private SizeSelector mPictureSizeSelector;
    private SizeSelector mVideoSizeSelector;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    int mSnapshotMaxWidth = Integer.MAX_VALUE; // in REF_VIEW for consistency with SizeSelectors
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    int mSnapshotMaxHeight = Integer.MAX_VALUE; // in REF_VIEW for consistency with SizeSelectors

    protected int mCameraId;
    protected CameraOptions mCameraOptions;
    protected Mapper mMapper;
    protected FrameManager mFrameManager;
    protected PictureRecorder mPictureRecorder;
    protected VideoRecorder mVideoRecorder;
    protected long mVideoMaxSize;
    protected int mVideoMaxDuration;
    protected int mVideoBitRate;
    protected int mAudioBitRate;
    protected Size mCaptureSize;
    protected Size mPreviewStreamSize;
    protected int mPreviewFormat;

    protected int mSensorOffset;
    private int mDisplayOffset;
    private int mDeviceOrientation;

    protected int mState = STATE_STOPPED;

    // Used for testing.
    Task<Void> mZoomTask = new Task<>();
    Task<Void> mExposureCorrectionTask = new Task<>();
    Task<Void> mFlashTask = new Task<>();
    Task<Void> mWhiteBalanceTask = new Task<>();
    Task<Void> mHdrTask = new Task<>();
    Task<Void> mLocationTask = new Task<>();
    Task<Void> mStartVideoTask = new Task<>();
    Task<Void> mPlaySoundsTask = new Task<>();

    CameraController(CameraView.CameraCallbacks callback) {
        mCameraCallbacks = callback;
        mCrashHandler = new Handler(Looper.getMainLooper());
        mHandler = WorkerHandler.get("CameraViewController");
        mHandler.getThread().setUncaughtExceptionHandler(this);
        mFrameManager = new FrameManager(2, this);
    }

    void setPreview(@NonNull CameraPreview cameraPreview) {
        mPreview = cameraPreview;
        mPreview.setSurfaceCallback(this);
    }

    //region Error handling

    private static class NoOpExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            // No-op.
        }
    }

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

    // Public & not final so we can verify with mockito in CameraViewTest
    public void destroy() {
        LOG.i("destroy:", "state:", ss());
        // Prevent CameraController leaks. Don't set to null, or exceptions
        // inside the standard stop() method might crash the main thread.
        mHandler.getThread().setUncaughtExceptionHandler(new NoOpExceptionHandler());
        // Stop if needed.
        stopImmediately();
    }

    //endregion

    //region Start&Stop

    @NonNull
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
                mCameraCallbacks.dispatchOnCameraOpened(mCameraOptions);
            }
        });
    }

    // Stops the preview asynchronously.
    // Public & not final so we can verify with mockito in CameraViewTest
    public void stop() {
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
    @SuppressWarnings("WeakerAccess")
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
                mCameraCallbacks.dispatchOnCameraOpened(mCameraOptions);
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

    // This is called before start() and never again.
    final void setDisplayOffset(int displayOffset) {
        mDisplayOffset = displayOffset;
    }

    // This can be called multiple times.
    final void setDeviceOrientation(int deviceOrientation) {
        mDeviceOrientation = deviceOrientation;
    }

    final void setPreviewStreamSizeSelector(@Nullable SizeSelector selector) {
        mPreviewStreamSizeSelector = selector;
    }

    final void setPictureSizeSelector(@NonNull SizeSelector selector) {
        mPictureSizeSelector = selector;
    }

    final void setVideoSizeSelector(@NonNull SizeSelector selector) {
        mVideoSizeSelector = selector;
    }

    final void setVideoMaxSize(long videoMaxSizeBytes) {
        mVideoMaxSize = videoMaxSizeBytes;
    }

    final void setVideoMaxDuration(int videoMaxDurationMillis) {
        mVideoMaxDuration = videoMaxDurationMillis;
    }

    final void setVideoCodec(@NonNull VideoCodec codec) {
        mVideoCodec = codec;
    }

    final void setVideoBitRate(int videoBitRate) {
        mVideoBitRate = videoBitRate;
    }

    final void setAudioBitRate(int audioBitRate) {
        mAudioBitRate = audioBitRate;
    }

    final void setSnapshotMaxWidth(int maxWidth) {
        mSnapshotMaxWidth = maxWidth;
    }

    final void setSnapshotMaxHeight(int maxHeight) {
        mSnapshotMaxHeight = maxHeight;
    }

    //endregion

    //region Abstract setters and APIs

    // Should restart the session if active.
    abstract void setMode(@NonNull Mode mode);

    // Should restart the session if active.
    abstract void setFacing(@NonNull Facing facing);

    // If closed, no-op. If opened, check supported and apply.
    abstract void setZoom(float zoom, @Nullable PointF[] points, boolean notify);

    // If closed, no-op. If opened, check supported and apply.
    abstract void setExposureCorrection(float EVvalue, @NonNull float[] bounds, @Nullable PointF[] points, boolean notify);

    // If closed, keep. If opened, check supported and apply.
    abstract void setFlash(@NonNull Flash flash);

    // If closed, keep. If opened, check supported and apply.
    abstract void setWhiteBalance(@NonNull WhiteBalance whiteBalance);

    // If closed, keep. If opened, check supported and apply.
    abstract void setHdr(@NonNull Hdr hdr);

    // If closed, keep. If opened, check supported and apply.
    abstract void setLocation(@Nullable Location location);

    // Just set.
    abstract void setAudio(@NonNull Audio audio);

    abstract void takePicture();

    abstract void takePictureSnapshot(@NonNull AspectRatio viewAspectRatio);

    abstract void takeVideo(@NonNull File file);

    abstract void takeVideoSnapshot(@NonNull File file, @NonNull AspectRatio viewAspectRatio);

    abstract void stopVideo();

    abstract void startAutoFocus(@Nullable Gesture gesture, @NonNull PointF point);

    abstract void setPlaySounds(boolean playSounds);

    //endregion

    //region final getters

    @Nullable
    final CameraOptions getCameraOptions() {
        return mCameraOptions;
    }

    @NonNull
    final Facing getFacing() {
        return mFacing;
    }

    @NonNull
    final Flash getFlash() {
        return mFlash;
    }

    @NonNull
    final WhiteBalance getWhiteBalance() {
        return mWhiteBalance;
    }

    final VideoCodec getVideoCodec() {
        return mVideoCodec;
    }

    final int getVideoBitRate() {
        return mVideoBitRate;
    }

    final long getVideoMaxSize() {
        return mVideoMaxSize;
    }

    final int getVideoMaxDuration() {
        return mVideoMaxDuration;
    }

    @NonNull
    final Mode getMode() {
        return mMode;
    }

    @NonNull
    final Hdr getHdr() {
        return mHdr;
    }

    @Nullable
    final Location getLocation() {
        return mLocation;
    }

    @NonNull
    final Audio getAudio() {
        return mAudio;
    }

    final int getAudioBitRate() {
        return mAudioBitRate;
    }

    @Nullable
    /* for tests */ final SizeSelector getPreviewStreamSizeSelector() {
        return mPreviewStreamSizeSelector;
    }

    @NonNull
    /* for tests */ final SizeSelector getPictureSizeSelector() {
        return mPictureSizeSelector;
    }

    @NonNull
    /* for tests */ final SizeSelector getVideoSizeSelector() {
        return mVideoSizeSelector;
    }

    final float getZoomValue() {
        return mZoomValue;
    }

    final float getExposureCorrectionValue() {
        return mExposureCorrectionValue;
    }

    final boolean isTakingVideo() {
        return mVideoRecorder != null;
    }

    final boolean isTakingPicture() {
        return mPictureRecorder != null;
    }

    //endregion

    //region Orientation utils

    private int computeSensorToViewOffset() {
        if (mFacing == Facing.FRONT) {
            return (360 - ((mSensorOffset + mDisplayOffset) % 360)) % 360;
        } else {
            return (mSensorOffset - mDisplayOffset + 360) % 360;
        }
    }

    private int computeSensorToOutputOffset() {
        if (mFacing == Facing.FRONT) {
            return (mSensorOffset - mDeviceOrientation + 360) % 360;
        } else {
            return (mSensorOffset + mDeviceOrientation) % 360;
        }
    }

    // Returns the offset between two reference systems.
    final int offset(int fromReference, int toReference) {
        if (fromReference == toReference) return 0;
        // We only know how to compute offsets with respect to REF_SENSOR.
        // That's why we separate the two cases.
        if (fromReference == REF_SENSOR) {
            return toReference == REF_VIEW ?
                    computeSensorToViewOffset() :
                    computeSensorToOutputOffset();
        }
        // Maybe the sensor is the other.
        if (toReference == REF_SENSOR) {
            return (-offset(toReference, fromReference) + 360) % 360;
        }
        // None of them is the sensor. Use a difference.
        return (offset(REF_SENSOR, toReference) - offset(REF_SENSOR, fromReference) + 360) % 360;
    }

    final boolean flip(int reference1, int reference2) {
        return offset(reference1, reference2) % 180 != 0;
    }

    @Nullable
    final Size getPictureSize(@SuppressWarnings("SameParameterValue") int reference) {
        if (mCaptureSize == null || mMode == Mode.VIDEO) return null;
        return flip(REF_SENSOR, reference) ? mCaptureSize.flip() : mCaptureSize;
    }

    @Nullable
    final Size getVideoSize(@SuppressWarnings("SameParameterValue") int reference) {
        if (mCaptureSize == null || mMode == Mode.PICTURE) return null;
        return flip(REF_SENSOR, reference) ? mCaptureSize.flip() : mCaptureSize;
    }

    @Nullable
    final Size getPreviewStreamSize(int reference) {
        if (mPreviewStreamSize == null) return null;
        return flip(REF_SENSOR, reference) ? mPreviewStreamSize.flip() : mPreviewStreamSize;
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable
    final Size getPreviewSurfaceSize(int reference) {
        if (mPreview == null) return null;
        return flip(REF_VIEW, reference) ? mPreview.getSurfaceSize().flip() : mPreview.getSurfaceSize();
    }

    /**
     * Returns the snapshot size, but not cropped with the view dimensions, which
     * is what we will do before creating the snapshot. However, cropping is done at various
     * levels so we don't want to perform the op here.
     *
     * The base snapshot size is based on PreviewStreamSize (later cropped with view ratio). Why?
     * One might be tempted to say that it is the SurfaceSize (which already matches the view ratio).
     *
     * The camera sensor will capture preview frames with PreviewStreamSize and that's it. Then they
     * are hardware-scaled by the preview surface, but this does not affect the snapshot, as the
     * snapshot recorder simply creates another surface.
     *
     * Done tests to ensure that this is true, by using
     * 1. small SurfaceSize and biggest() PreviewStreamSize: output is not low quality
     * 2. big SurfaceSize and smallest() PreviewStreamSize: output is low quality
     * In both cases the result.size here was set to the biggest of the two.
     *
     * I could not find the same evidence for videos, but I would say that the same things should
     * apply, despite the capturing mechanism being different.
     */
    @Nullable
    final Size getUncroppedSnapshotSize(int reference) {
        Size baseSize = getPreviewStreamSize(reference);
        if (baseSize == null) return null;
        boolean flip = flip(reference, REF_VIEW);
        int maxWidth = flip ? mSnapshotMaxHeight : mSnapshotMaxWidth;
        int maxHeight = flip ? mSnapshotMaxWidth : mSnapshotMaxHeight;
        float baseRatio = AspectRatio.of(baseSize).toFloat();
        float maxValuesRatio = AspectRatio.of(maxWidth, maxHeight).toFloat();
        if (maxValuesRatio >= baseRatio) {
            // Height is the real constraint.
            int outHeight = Math.min(baseSize.getHeight(), maxHeight);
            int outWidth = (int) Math.floor((float) outHeight * baseRatio);
            return new Size(outWidth, outHeight);
        } else {
            // Width is the real constraint.
            int outWidth = Math.min(baseSize.getWidth(), maxWidth);
            int outHeight = (int) Math.floor((float) outWidth / baseRatio);
            return new Size(outWidth, outHeight);
        }
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
    @NonNull
    @SuppressWarnings("WeakerAccess")
    protected final Size computeCaptureSize() {
        return computeCaptureSize(mMode);
    }

    @SuppressWarnings("WeakerAccess")
    protected final Size computeCaptureSize(Mode mode) {
        // We want to pass stuff into the REF_VIEW reference, not the sensor one.
        // This is already managed by CameraOptions, so we just flip again at the end.
        boolean flip = flip(REF_SENSOR, REF_VIEW);
        SizeSelector selector;
        Collection<Size> sizes;
        if (mode == Mode.PICTURE) {
            selector = mPictureSizeSelector;
            sizes = mCameraOptions.getSupportedPictureSizes();
        } else {
            selector = mVideoSizeSelector;
            sizes = mCameraOptions.getSupportedVideoSizes();
        }
        selector = SizeSelectors.or(selector, SizeSelectors.biggest());
        List<Size> list = new ArrayList<>(sizes);
        Size result = selector.select(list).get(0);
        LOG.i("computeCaptureSize:", "result:", result, "flip:", flip, "mode:", mode);
        if (flip) result = result.flip(); // Go back to REF_SENSOR
        return result;
    }

    @NonNull
    @SuppressWarnings("WeakerAccess")
    protected final Size computePreviewStreamSize(@NonNull List<Size> previewSizes) {
        // These sizes come in REF_SENSOR. Since there is an external selector involved,
        // we must convert all of them to REF_VIEW, then flip back when returning.
        boolean flip = flip(REF_SENSOR, REF_VIEW);
        List<Size> sizes = new ArrayList<>(previewSizes.size());
        for (Size size : previewSizes) {
            sizes.add(flip ? size.flip() : size);
        }

        // Create our own default selector, which will be used if the external mPreviewStreamSizeSelector
        // is null, or if it fails in finding a size.
        Size targetMinSize = getPreviewSurfaceSize(REF_VIEW);
        AspectRatio targetRatio = AspectRatio.of(mCaptureSize.getWidth(), mCaptureSize.getHeight());
        if (flip) targetRatio = targetRatio.inverse();
        LOG.i("size:", "computePreviewStreamSize:", "targetRatio:", targetRatio, "targetMinSize:", targetMinSize);
        SizeSelector matchRatio = SizeSelectors.and( // Match this aspect ratio and sort by biggest
                SizeSelectors.aspectRatio(targetRatio, 0),
                SizeSelectors.biggest());
        SizeSelector matchSize = SizeSelectors.and( // Bigger than this size, and sort by smallest
                SizeSelectors.minHeight(targetMinSize.getHeight()),
                SizeSelectors.minWidth(targetMinSize.getWidth()),
                SizeSelectors.smallest());
        SizeSelector matchAll = SizeSelectors.or(
                SizeSelectors.and(matchRatio, matchSize), // Try to respect both constraints.
                matchSize, // If couldn't match aspect ratio, at least respect the size
                matchRatio, // If couldn't respect size, at least match aspect ratio
                SizeSelectors.biggest() // If couldn't match any, take the biggest.
        );

        // Apply the external selector with this as a fallback,
        // and return a size in REF_SENSOR reference.
        SizeSelector selector;
        if (mPreviewStreamSizeSelector != null) {
            selector = SizeSelectors.or(mPreviewStreamSizeSelector, matchAll);
        } else {
            selector = matchAll;
        }
        Size result = selector.select(sizes).get(0);
        if (flip) result = result.flip();
        LOG.i("computePreviewStreamSize:", "result:", result, "flip:", flip);
        return result;
    }

    //endregion
}

package com.otaliastudios.cameraview.engine;

import android.content.Context;
import android.graphics.PointF;
import android.location.Location;


import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.PictureFormat;
import com.otaliastudios.cameraview.engine.orchestrator.CameraOrchestrator;
import com.otaliastudios.cameraview.engine.orchestrator.CameraState;
import com.otaliastudios.cameraview.engine.orchestrator.CameraStateOrchestrator;
import com.otaliastudios.cameraview.overlay.Overlay;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.engine.offset.Angles;
import com.otaliastudios.cameraview.engine.offset.Reference;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameManager;
import com.otaliastudios.cameraview.internal.utils.WorkerHandler;
import com.otaliastudios.cameraview.picture.PictureRecorder;
import com.otaliastudios.cameraview.preview.CameraPreview;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.VideoCodec;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.size.SizeSelector;
import com.otaliastudios.cameraview.size.SizeSelectors;
import com.otaliastudios.cameraview.video.VideoRecorder;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * PROCESS
 * Setting up the Camera is usually a 4 steps process:
 * 1. Setting up the Surface. Done by {@link CameraPreview}.
 * 2. Starting the camera. Done by us. See {@link #startEngine()}, {@link #onStartEngine()}.
 * 3. Binding the camera to the surface. Done by us. See {@link #startBind()},
 *    {@link #onStartBind()}
 * 4. Streaming the camera preview. Done by us. See {@link #startPreview()},
 *    {@link #onStartPreview()}
 *
 * The first two steps can actually happen at the same time, anyway
 * the order is not guaranteed, we just get a callback from the Preview when 1 happens.
 * So at the end of both step 1 and 2, the engine should check if both have
 * been performed and trigger the steps 3 and 4.
 *
 * STATE
 * We only expose generic {@link #start()} and {@link #stop(boolean)} calls to the outside.
 * The external users of this class are most likely interested in whether we have completed step 2
 * or not, since that tells us if we can act on the camera or not, rather than knowing about
 * steps 3 and 4.
 *
 * So in the {@link CameraEngine} notation,
 * - {@link #start()}: ASYNC - starts the engine (S2). When possible, at a later time,
 *                     S3 and S4 are also performed.
 * - {@link #stop(boolean)}: ASYNC - stops everything: undoes S4, then S3, then S2.
 * - {@link #restart()}: ASYNC - completes a stop then a start.
 * - {@link #destroy(boolean)}: SYNC - performs a {@link #stop(boolean)} that will go on no matter
 *                              what, without throwing. Makes the engine unusable and clears
 *                              resources.
 *
 * THREADING
 * Subclasses should always execute code on the thread given by {@link #mHandler}.
 * For convenience, all the setup and tear down methods are called on this engine thread:
 * {@link #onStartEngine()}, {@link #onStartBind()}, {@link #onStartPreview()} to setup and
 * {@link #onStopEngine()}, {@link #onStopBind()}, {@link #onStopPreview()} to tear down.
 * However, these methods are not forced to be synchronous and then can simply return a Google's
 * {@link Task}.
 *
 * Other setters are executed on the callers thread so subclasses should make sure they post
 * to the engine handler before acting on themselves.
 *
 *
 * ERROR HANDLING
 * THe {@link #mHandler} thread has a special {@link Thread.UncaughtExceptionHandler} that handles
 * exceptions and dispatches error to the callback (instead of crashing the app).
 * This lets subclasses run code safely and directly throw {@link CameraException}s when needed.
 *
 * For convenience, the two main method {@link #onStartEngine()} and {@link #onStopEngine()}
 * are already called on the engine thread, but they can still be asynchronous by returning a
 * Google's {@link com.google.android.gms.tasks.Task}.
 */
public abstract class CameraEngine implements
        CameraPreview.SurfaceCallback,
        PictureRecorder.PictureResultListener,
        VideoRecorder.VideoResultListener {

    public interface Callback {
        @NonNull Context getContext();
        void dispatchOnCameraOpened(@NonNull CameraOptions options);
        void dispatchOnCameraClosed();
        void onCameraPreviewStreamSizeChanged();
        void onShutter(boolean shouldPlaySound);
        void dispatchOnVideoTaken(@NonNull VideoResult.Stub stub);
        void dispatchOnPictureTaken(@NonNull PictureResult.Stub stub);
        void dispatchOnFocusStart(@Nullable Gesture trigger, @NonNull PointF where);
        void dispatchOnFocusEnd(@Nullable Gesture trigger, boolean success, @NonNull PointF where);
        void dispatchOnZoomChanged(final float newValue, @Nullable final PointF[] fingers);
        void dispatchOnExposureCorrectionChanged(float newValue, @NonNull float[] bounds,
                                                 @Nullable PointF[] fingers);
        void dispatchFrame(@NonNull Frame frame);
        void dispatchError(CameraException exception);
        void dispatchOnVideoRecordingStart();
        void dispatchOnVideoRecordingEnd();
    }

    protected static final String TAG = CameraEngine.class.getSimpleName();
    protected static final CameraLogger LOG = CameraLogger.create(TAG);

    // Need to be protected
    @SuppressWarnings("WeakerAccess") protected final Callback mCallback;
    @SuppressWarnings("WeakerAccess") protected CameraPreview mPreview;
    @SuppressWarnings("WeakerAccess") protected CameraOptions mCameraOptions;
    @SuppressWarnings("WeakerAccess") protected PictureRecorder mPictureRecorder;
    @SuppressWarnings("WeakerAccess") protected VideoRecorder mVideoRecorder;
    @SuppressWarnings("WeakerAccess") protected Size mCaptureSize;
    @SuppressWarnings("WeakerAccess") protected Size mPreviewStreamSize;
    @SuppressWarnings("WeakerAccess") protected Flash mFlash;
    @SuppressWarnings("WeakerAccess") protected WhiteBalance mWhiteBalance;
    @SuppressWarnings("WeakerAccess") protected VideoCodec mVideoCodec;
    @SuppressWarnings("WeakerAccess") protected Hdr mHdr;
    @SuppressWarnings("WeakerAccess") protected PictureFormat mPictureFormat;
    @SuppressWarnings("WeakerAccess") protected Location mLocation;
    @SuppressWarnings("WeakerAccess") protected float mZoomValue;
    @SuppressWarnings("WeakerAccess") protected float mExposureCorrectionValue;
    @SuppressWarnings("WeakerAccess") protected boolean mPlaySounds;
    @SuppressWarnings("WeakerAccess") protected boolean mPictureMetering;
    @SuppressWarnings("WeakerAccess") protected boolean mPictureSnapshotMetering;
    @SuppressWarnings("WeakerAccess") protected float mPreviewFrameRate;

    private WorkerHandler mHandler;
    @VisibleForTesting Handler mCrashHandler;
    private final FrameManager mFrameManager;
    private final Angles mAngles;
    @Nullable private SizeSelector mPreviewStreamSizeSelector;
    private SizeSelector mPictureSizeSelector;
    private SizeSelector mVideoSizeSelector;
    private Facing mFacing;
    private Mode mMode;
    private Audio mAudio;
    private long mVideoMaxSize;
    private int mVideoMaxDuration;
    private int mVideoBitRate;
    private int mAudioBitRate;
    private boolean mHasFrameProcessors;
    private long mAutoFocusResetDelayMillis;
    private int mSnapshotMaxWidth = Integer.MAX_VALUE; // in REF_VIEW like SizeSelectors
    private int mSnapshotMaxHeight = Integer.MAX_VALUE; // in REF_VIEW like SizeSelectors
    private Overlay overlay;

    @SuppressWarnings("WeakerAccess")
    protected final CameraStateOrchestrator mOrchestrator
            = new CameraStateOrchestrator(new CameraOrchestrator.Callback() {
        @Override
        @NonNull
        public WorkerHandler getJobWorker(@NonNull String job) {
            return mHandler;
        }

        @Override
        public void handleJobException(@NonNull String job, @NonNull Exception exception) {
            handleException(Thread.currentThread(), exception, false);
        }
    });

    // Ops used for testing.
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) Task<Void> mZoomTask
            = Tasks.forResult(null);
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) Task<Void> mExposureCorrectionTask
            = Tasks.forResult(null);
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) Task<Void> mFlashTask
            = Tasks.forResult(null);
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) Task<Void> mWhiteBalanceTask
            = Tasks.forResult(null);
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) Task<Void> mHdrTask
            = Tasks.forResult(null);
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) Task<Void> mLocationTask
            = Tasks.forResult(null);
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) Task<Void> mPlaySoundsTask
            = Tasks.forResult(null);
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) Task<Void> mPreviewFrameRateTask
            = Tasks.forResult(null);

    protected CameraEngine(@NonNull Callback callback) {
        mCallback = callback;
        mCrashHandler = new Handler(Looper.getMainLooper());
        mFrameManager = instantiateFrameManager();
        mAngles = new Angles();
        recreateHandler(false);
    }

    public void setPreview(@NonNull CameraPreview cameraPreview) {
        if (mPreview != null) mPreview.setSurfaceCallback(null);
        mPreview = cameraPreview;
        mPreview.setSurfaceCallback(this);
    }

    @NonNull
    public CameraPreview getPreview() {
        return mPreview;
    }

    //region Error handling

    /**
     * The base exception handler, which inspects the exception and
     * decides what to do.
     */
    private class CrashExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
            handleException(thread, throwable, true);
        }
    }

    /**
     * A static exception handler used during destruction to avoid leaks,
     * since the default handler is not static and the thread might survive the engine.
     */
    private static class NoOpExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
            LOG.w("EXCEPTION:", "In the NoOpExceptionHandler, probably while destroying.",
                    "Thread:", thread, "Error:", throwable);
        }
    }

    /**
     * Handles exceptions coming from either runtime errors on the {@link #mHandler} code that is
     * not caught (using the {@link CrashExceptionHandler}), as might happen during standard
     * mHandler.post() operations that subclasses might do, OR for errors caught by tasks and
     * continuations that we launch here.
     *
     * In the first case, the thread is about to be terminated. In the second case,
     * we can actually keep using it.
     *
     * @param thread the thread
     * @param throwable the throwable
     * @param fromExceptionHandler true if coming from exception handler
     */
    private void handleException(@NonNull final Thread thread,
                                 final @NonNull Throwable throwable,
                                 final boolean fromExceptionHandler) {
        // 1. If this comes from the exception handler, the thread has crashed. Replace it.
        // Most actions are wrapped into Tasks so don't go here, but some callbacks do
        // (at least in Camera1, e.g. onError).
        if (fromExceptionHandler) {
            LOG.e("EXCEPTION:", "Handler thread is gone. Replacing.");
            recreateHandler(false);
        }

        // 2. Depending on the exception, we must destroy(false|true) to release resources, and
        // notify the outside, either with the callback or by crashing the app.
        LOG.e("EXCEPTION:", "Scheduling on the crash handler...");
        mCrashHandler.post(new Runnable() {
            @Override
            public void run() {
                if (throwable instanceof CameraException) {
                    CameraException exception = (CameraException) throwable;
                    if (exception.isUnrecoverable()) {
                        LOG.e("EXCEPTION:", "Got CameraException. " +
                                "Since it is unrecoverable, executing destroy(false).");
                        destroy(false);
                    }
                    LOG.e("EXCEPTION:", "Got CameraException. Dispatching to callback.");
                    mCallback.dispatchError(exception);
                } else {
                    LOG.e("EXCEPTION:", "Unexpected error! Executing destroy(true).");
                    destroy(true);
                    LOG.e("EXCEPTION:", "Unexpected error! Throwing.");
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    } else {
                        throw new RuntimeException(throwable);
                    }
                }
            }
        });
    }

    /**
     * Recreates the handler, to ensure we use a fresh one from now on.
     * If we suspect that handler is currently stuck, the orchestrator should be reset
     * because it hosts a chain of tasks and the last one will never complete.
     * @param resetOrchestrator true to reset
     */
    private void recreateHandler(boolean resetOrchestrator) {
        if (mHandler != null) mHandler.destroy();
        mHandler = WorkerHandler.get("CameraViewEngine");
        mHandler.getThread().setUncaughtExceptionHandler(new CrashExceptionHandler());
    }

    //endregion

    //region State management

    @NonNull
    public final CameraState getState() {
        return mOrchestrator.getCurrentState();
    }

    @NonNull
    public final CameraState getTargetState() {
        return mOrchestrator.getTargetState();
    }

    public boolean isChangingState() {
        return mOrchestrator.hasPendingStateChange();
    }

    /**
     * Calls {@link #stop(boolean)} and waits for it.
     * Not final due to mockito requirements.
     *
     * If unrecoverably is true, this also releases resources and the engine will not be in a
     * functional state after. If forever is false, this really is just a synchronous stop.
     *
     * NOTE: Should not be called on the orchestrator thread! This would cause deadlocks due to us
     * awaiting for {@link #stop(boolean)} to return.
     */
    public void destroy(boolean unrecoverably) {
        LOG.i("DESTROY:", "state:", getState(),
                "thread:", Thread.currentThread(),
                "unrecoverably:", unrecoverably);
        if (unrecoverably) {
            // Prevent CameraEngine leaks. Don't set to null, or exceptions
            // inside the standard stop() method might crash the main thread.
            mHandler.getThread().setUncaughtExceptionHandler(new NoOpExceptionHandler());
        }
        // Cannot use Tasks.await() because we might be on the UI thread.
        final CountDownLatch latch = new CountDownLatch(1);
        stop(true).addOnCompleteListener(
                mHandler.getExecutor(),
                new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                latch.countDown();
            }
        });
        try {
            boolean success = latch.await(6, TimeUnit.SECONDS);
            if (!success) {
                // This thread is likely stuck. The reason might be deadlock issues in the internal
                // camera implementation, at least in emulators: see Camera1Engine and Camera2Engine
                // onStopEngine() implementation and comments.
                LOG.e("DESTROY: Could not destroy synchronously after 6 seconds.",
                        "Current thread:", Thread.currentThread(),
                        "Handler thread:", mHandler.getThread());
                recreateHandler(true);
                LOG.e("DESTROY: Could not destroy synchronously after 6 seconds.",
                        "Trying again on thread:", mHandler.getThread());
                destroy(unrecoverably);
            }
        } catch (InterruptedException ignore) {}
    }

    @SuppressWarnings("WeakerAccess")
    public void restart() {
        LOG.i("RESTART:", "scheduled. State:", getState());
        stop(false);
        start();
    }

    @NonNull
    public Task<Void> start() {
        LOG.i("START:", "scheduled. State:", getState());
        Task<Void> engine = startEngine();
        startBind();
        startPreview();
        return engine;
    }

    @NonNull
    public Task<Void> stop(final boolean swallowExceptions) {
        LOG.i("STOP:", "scheduled. State:", getState());
        stopPreview(swallowExceptions);
        stopBind(swallowExceptions);
        return stopEngine(swallowExceptions);
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected Task<Void> restartBind() {
        LOG.i("RESTART BIND:", "scheduled. State:", getState());
        stopPreview(false);
        stopBind(false);
        startBind();
        return startPreview();
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected Task<Void> restartPreview() {
        LOG.i("RESTART PREVIEW:", "scheduled. State:", getState());
        stopPreview(false);
        return startPreview();
    }

    //endregion

    //region Start & Stop the engine

    @NonNull
    @EngineThread
    private Task<Void> startEngine() {
        return mOrchestrator.scheduleStateChange(CameraState.OFF, CameraState.ENGINE,
                true,
                new Callable<Task<CameraOptions>>() {
            @Override
            public Task<CameraOptions> call() {
                if (!collectCameraInfo(mFacing)) {
                    LOG.e("onStartEngine:", "No camera available for facing", mFacing);
                    throw new CameraException(CameraException.REASON_NO_CAMERA);
                }
                return onStartEngine();
            }
        }).onSuccessTask(new SuccessContinuation<CameraOptions, Void>() {
            @NonNull
            @Override
            public Task<Void> then(@Nullable CameraOptions cameraOptions) {
                // Put this on the outer task so we're sure it's called after getState() is changed.
                // This was breaking some tests on rare occasions.
                if (cameraOptions == null) throw new RuntimeException("Null options!");
                mCallback.dispatchOnCameraOpened(cameraOptions);
                return Tasks.forResult(null);
            }
        });
    }

    @NonNull
    @EngineThread
    private Task<Void> stopEngine(boolean swallowExceptions) {
        return mOrchestrator.scheduleStateChange(CameraState.ENGINE, CameraState.OFF,
                !swallowExceptions,
                new Callable<Task<Void>>() {
            @Override
            public Task<Void> call() {
                return onStopEngine();
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Put this on the outer task so we're sure it's called after getState() is OFF.
                // This was breaking some tests on rare occasions.
                mCallback.dispatchOnCameraClosed();
            }
        });
    }

    /**
     * Starts the engine.
     * @return a task
     */
    @NonNull
    @EngineThread
    protected abstract Task<CameraOptions> onStartEngine();

    /**
     * Stops the engine.
     * Stop events should generally not throw exceptions. We
     * want to release resources either way.
     * @return a task
     */
    @NonNull
    @EngineThread
    protected abstract Task<Void> onStopEngine();

    //endregion

    //region Start & Stop binding

    @NonNull
    @EngineThread
    private Task<Void> startBind() {
        return mOrchestrator.scheduleStateChange(CameraState.ENGINE, CameraState.BIND,
                true,
                new Callable<Task<Void>>() {
            @Override
            public Task<Void> call() {
                if (mPreview != null && mPreview.hasSurface()) {
                    return onStartBind();
                } else {
                    return Tasks.forCanceled();
                }
            }
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    @EngineThread
    private Task<Void> stopBind(boolean swallowExceptions) {
        return mOrchestrator.scheduleStateChange(CameraState.BIND, CameraState.ENGINE,
                !swallowExceptions,
                new Callable<Task<Void>>() {
            @Override
            public Task<Void> call() {
                return onStopBind();
            }
        });
    }

    /**
     * Starts the binding process.
     * @return a task
     */
    @NonNull
    @EngineThread
    protected abstract Task<Void> onStartBind();

    /**
     * Stops the binding process.
     * Stop events should generally not throw exceptions. We
     * want to release resources either way.
     * @return a task
     */
    @NonNull
    @EngineThread
    protected abstract Task<Void> onStopBind();

    //endregion

    //region Start & Stop preview

    @NonNull
    @EngineThread
    private Task<Void> startPreview() {
        return mOrchestrator.scheduleStateChange(CameraState.BIND, CameraState.PREVIEW,
                true,
                new Callable<Task<Void>>() {
            @Override
            public Task<Void> call() {
                return onStartPreview();
            }
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    @EngineThread
    private Task<Void> stopPreview(boolean swallowExceptions) {
        return mOrchestrator.scheduleStateChange(CameraState.PREVIEW, CameraState.BIND,
                !swallowExceptions,
                new Callable<Task<Void>>() {
            @Override
            public Task<Void> call() {
                return onStopPreview();
            }
        });
    }

    /**
     * Starts the preview streaming.
     * @return a task
     */
    @NonNull
    @EngineThread
    protected abstract Task<Void> onStartPreview();

    /**
     * Stops the preview streaming.
     * Stop events should generally not throw exceptions. We
     * want to release resources either way.
     * @return a task
     */
    @NonNull
    @EngineThread
    protected abstract Task<Void> onStopPreview();

    //endregion

    //region Surface callbacks

    /**
     * The surface is now available, which means that step 1 has completed.
     * If we have also completed step 2, go on with binding and streaming.
     */
    @Override
    public final void onSurfaceAvailable() {
        LOG.i("onSurfaceAvailable:", "Size is", getPreviewSurfaceSize(Reference.VIEW));
        startBind();
        startPreview();
    }

    @Override
    public final void onSurfaceDestroyed() {
        LOG.i("onSurfaceDestroyed");
        stopPreview(false);
        stopBind(false);
    }

    @Override
    public final void onSurfaceChanged() {
        LOG.i("onSurfaceChanged:", "Size is", getPreviewSurfaceSize(Reference.VIEW));
        mOrchestrator.scheduleStateful("surface changed", CameraState.BIND,
                new Runnable() {
            @Override
            public void run() {
                // Compute a new camera preview size and apply.
                Size newSize = computePreviewStreamSize();
                if (newSize.equals(mPreviewStreamSize)) {
                    LOG.i("onSurfaceChanged:",
                            "The computed preview size is identical. No op.");
                } else {
                    LOG.i("onSurfaceChanged:",
                            "Computed a new preview size. Calling onPreviewStreamSizeChanged().");
                    mPreviewStreamSize = newSize;
                    onPreviewStreamSizeChanged();
                }
            }
        });
    }

    /**
     * The preview stream size has changed. At this point, some engine might want to
     * simply call {@link #restartPreview()}, others to {@link #restartBind()}.
     *
     * It basically depends on the step at which the preview stream size is actually used.
     */
    @EngineThread
    protected abstract void onPreviewStreamSizeChanged();

    //endregion

    //region Final setters and getters

    public final void setOverlay(@Nullable Overlay overlay) {
        this.overlay = overlay;
    }

    @Nullable
    public final Overlay getOverlay() {
        return overlay;
    }

    public final Angles getAngles() {
        return mAngles;
    }

    public final void setPreviewStreamSizeSelector(@Nullable SizeSelector selector) {
        mPreviewStreamSizeSelector = selector;
    }

    @Nullable
    public final SizeSelector getPreviewStreamSizeSelector() {
        return mPreviewStreamSizeSelector;
    }

    public final void setPictureSizeSelector(@NonNull SizeSelector selector) {
        mPictureSizeSelector = selector;
    }

    @NonNull
    public final SizeSelector getPictureSizeSelector() {
        return mPictureSizeSelector;
    }

    public final void setVideoSizeSelector(@NonNull SizeSelector selector) {
        mVideoSizeSelector = selector;
    }

    @NonNull
    public final SizeSelector getVideoSizeSelector() {
        return mVideoSizeSelector;
    }

    public final void setVideoMaxSize(long videoMaxSizeBytes) {
        mVideoMaxSize = videoMaxSizeBytes;
    }

    public final long getVideoMaxSize() {
        return mVideoMaxSize;
    }

    public final void setVideoMaxDuration(int videoMaxDurationMillis) {
        mVideoMaxDuration = videoMaxDurationMillis;
    }

    public final int getVideoMaxDuration() {
        return mVideoMaxDuration;
    }

    public final void setVideoCodec(@NonNull VideoCodec codec) {
        mVideoCodec = codec;
    }

    public final VideoCodec getVideoCodec() {
        return mVideoCodec;
    }

    public final void setVideoBitRate(int videoBitRate) {
        mVideoBitRate = videoBitRate;
    }

    public final int getVideoBitRate() {
        return mVideoBitRate;
    }

    public final void setAudioBitRate(int audioBitRate) {
        mAudioBitRate = audioBitRate;
    }

    public final int getAudioBitRate() {
        return mAudioBitRate;
    }

    public final void setSnapshotMaxWidth(int maxWidth) {
        mSnapshotMaxWidth = maxWidth;
    }

    public int getSnapshotMaxWidth() {
        return mSnapshotMaxWidth;
    }

    public final void setSnapshotMaxHeight(int maxHeight) {
        mSnapshotMaxHeight = maxHeight;
    }

    public int getSnapshotMaxHeight() {
        return mSnapshotMaxHeight;
    }

    public final void setAutoFocusResetDelay(long delayMillis) {
        mAutoFocusResetDelayMillis = delayMillis;
    }

    public final long getAutoFocusResetDelay() {
        return mAutoFocusResetDelayMillis;
    }

    /**
     * Sets a new facing value. This will restart the engine session (if there's any)
     * so that we can open the new facing camera.
     * @param facing facing
     */
    public final void setFacing(final @NonNull Facing facing) {
        final Facing old = mFacing;
        if (facing != old) {
            mFacing = facing;
            mOrchestrator.scheduleStateful("facing", CameraState.ENGINE,
                    new Runnable() {
                @Override
                public void run() {
                    if (collectCameraInfo(facing)) {
                        restart();
                    } else {
                        mFacing = old;
                    }
                }
            });
        }
    }

    @NonNull
    public final Facing getFacing() {
        return mFacing;
    }

    /**
     * Sets a new audio value that will be used for video recordings.
     * @param audio desired audio
     */
    public final void setAudio(@NonNull Audio audio) {
        if (mAudio != audio) {
            if (isTakingVideo()) {
                LOG.w("Audio setting was changed while recording. " +
                        "Changes will take place starting from next video");
            }
            mAudio = audio;
        }
    }

    @NonNull
    public final Audio getAudio() {
        return mAudio;
    }

    /**
     * Sets the desired mode (either picture or video).
     * @param mode desired mode.
     */
    public final void setMode(@NonNull Mode mode) {
        if (mode != mMode) {
            mMode = mode;
            mOrchestrator.scheduleStateful("mode", CameraState.ENGINE,
                    new Runnable() {
                @Override
                public void run() {
                    restart();
                }
            });
        }
    }

    @NonNull
    public final Mode getMode() {
        return mMode;
    }

    @NonNull
    public final FrameManager getFrameManager() {
        return mFrameManager;
    }

    @Nullable
    public final CameraOptions getCameraOptions() {
        return mCameraOptions;
    }

    @NonNull
    public final Flash getFlash() {
        return mFlash;
    }

    @NonNull
    public final WhiteBalance getWhiteBalance() {
        return mWhiteBalance;
    }

    @NonNull
    public final Hdr getHdr() {
        return mHdr;
    }

    @Nullable
    public final Location getLocation() {
        return mLocation;
    }

    @NonNull
    public final PictureFormat getPictureFormat() {
        return mPictureFormat;
    }

    public final float getZoomValue() {
        return mZoomValue;
    }

    public final float getExposureCorrectionValue() {
        return mExposureCorrectionValue;
    }

    public final float getPreviewFrameRate() {
        return mPreviewFrameRate;
    }

    @CallSuper
    public void setHasFrameProcessors(boolean hasFrameProcessors) {
        mHasFrameProcessors = hasFrameProcessors;
    }

    @SuppressWarnings("WeakerAccess")
    public final boolean hasFrameProcessors() {
        return mHasFrameProcessors;
    }

    @SuppressWarnings("WeakerAccess")
    protected final boolean shouldResetAutoFocus() {
        return mAutoFocusResetDelayMillis > 0 && mAutoFocusResetDelayMillis != Long.MAX_VALUE;
    }

    public final void setPictureMetering(boolean enable) {
        mPictureMetering = enable;
    }

    public final boolean getPictureMetering() {
        return mPictureMetering;
    }

    public final void setPictureSnapshotMetering(boolean enable) {
        mPictureSnapshotMetering = enable;
    }

    public final boolean getPictureSnapshotMetering() {
        return mPictureSnapshotMetering;
    }

    //endregion

    //region Abstract setters and APIs

    /**
     * Camera is about to be opened. Implementors should look into available cameras
     * and see if anyone matches the given {@link Facing value}.
     *
     * If so, implementors should set {@link Angles#setSensorOffset(Facing, int)}
     * and any other information (like camera ID) needed to start the engine.
     *
     * @param facing the facing value
     * @return true if we have one
     */
    @EngineThread
    protected abstract boolean collectCameraInfo(@NonNull Facing facing);

    /**
     * Called at construction time to get a frame manager that can later be
     * accessed through {@link #getFrameManager()}.
     * @return a frame manager
     */
    @NonNull
    protected abstract FrameManager instantiateFrameManager();

    // If closed, no-op. If opened, check supported and apply.
    public abstract void setZoom(float zoom, @Nullable PointF[] points, boolean notify);

    // If closed, no-op. If opened, check supported and apply.
    public abstract void setExposureCorrection(float EVvalue,
                                               @NonNull float[] bounds,
                                               @Nullable PointF[] points,
                                               boolean notify);

    // If closed, keep. If opened, check supported and apply.
    public abstract void setFlash(@NonNull Flash flash);

    // If closed, keep. If opened, check supported and apply.
    public abstract void setWhiteBalance(@NonNull WhiteBalance whiteBalance);

    // If closed, keep. If opened, check supported and apply.
    public abstract void setHdr(@NonNull Hdr hdr);

    // If closed, keep. If opened, check supported and apply.
    public abstract void setLocation(@Nullable Location location);

    // If closed, keep. If opened, check supported and apply.
    public abstract void setPictureFormat(@NonNull PictureFormat pictureFormat);

    public abstract void startAutoFocus(@Nullable Gesture gesture, @NonNull PointF point);

    public abstract void setPlaySounds(boolean playSounds);

    public abstract void setPreviewFrameRate(float previewFrameRate);

    //endregion

    //region picture and video control

    public final boolean isTakingPicture() {
        return mPictureRecorder != null;
    }

    /* not final for tests */
    public void takePicture(final @NonNull PictureResult.Stub stub) {
        // Save boolean before scheduling! See how Camera2Engine calls this with a temp value.
        final boolean metering = mPictureMetering;
        mOrchestrator.scheduleStateful("take picture", CameraState.BIND,
                new Runnable() {
            @Override
            public void run() {
                LOG.i("takePicture:", "running. isTakingPicture:", isTakingPicture());
                if (isTakingPicture()) return;
                if (mMode == Mode.VIDEO) {
                    throw new IllegalStateException("Can't take hq pictures while in VIDEO mode");
                }
                stub.isSnapshot = false;
                stub.location = mLocation;
                stub.facing = mFacing;
                stub.format = mPictureFormat;
                onTakePicture(stub, metering);
            }
        });
    }

    /**
     * The snapshot size is the {@link #getPreviewStreamSize(Reference)}, but cropped based on the
     * view/surface aspect ratio.
     * @param stub a picture stub
     */
    public final void takePictureSnapshot(final @NonNull PictureResult.Stub stub) {
        // Save boolean before scheduling! See how Camera2Engine calls this with a temp value.
        final boolean metering = mPictureSnapshotMetering;
        mOrchestrator.scheduleStateful("take picture snapshot", CameraState.BIND,
                new Runnable() {
            @Override
            public void run() {
                LOG.i("takePictureSnapshot:", "running. isTakingPicture:", isTakingPicture());
                if (isTakingPicture()) return;
                stub.location = mLocation;
                stub.isSnapshot = true;
                stub.facing = mFacing;
                stub.format = PictureFormat.JPEG;
                // Leave the other parameters to subclasses.
                //noinspection ConstantConditions
                AspectRatio ratio = AspectRatio.of(getPreviewSurfaceSize(Reference.OUTPUT));
                onTakePictureSnapshot(stub, ratio, metering);
            }
        });
    }

    @Override
    public void onPictureShutter(boolean didPlaySound) {
        mCallback.onShutter(!didPlaySound);
    }

    @Override
    public void onPictureResult(@Nullable PictureResult.Stub result, @Nullable Exception error) {
        mPictureRecorder = null;
        if (result != null) {
            mCallback.dispatchOnPictureTaken(result);
        } else {
            LOG.e("onPictureResult", "result is null: something went wrong.", error);
            mCallback.dispatchError(new CameraException(error,
                    CameraException.REASON_PICTURE_FAILED));
        }
    }

    public final boolean isTakingVideo() {
        return mVideoRecorder != null && mVideoRecorder.isRecording();
    }

    public final void takeVideo(final @NonNull VideoResult.Stub stub, final @NonNull File file) {
        mOrchestrator.scheduleStateful("take video", CameraState.BIND, new Runnable() {
            @Override
            public void run() {
                LOG.i("takeVideo:", "running. isTakingVideo:", isTakingVideo());
                if (isTakingVideo()) return;
                if (mMode == Mode.PICTURE) {
                    throw new IllegalStateException("Can't record video while in PICTURE mode");
                }
                stub.file = file;
                stub.isSnapshot = false;
                stub.videoCodec = mVideoCodec;
                stub.location = mLocation;
                stub.facing = mFacing;
                stub.audio = mAudio;
                stub.maxSize = mVideoMaxSize;
                stub.maxDuration = mVideoMaxDuration;
                stub.videoBitRate = mVideoBitRate;
                stub.audioBitRate = mAudioBitRate;
                onTakeVideo(stub);
            }
        });
    }

    /**
     * @param stub a video stub
     * @param file the output file
     */
    public final void takeVideoSnapshot(@NonNull final VideoResult.Stub stub,
                                        @NonNull final File file) {
        mOrchestrator.scheduleStateful("take video snapshot", CameraState.BIND,
                new Runnable() {
            @Override
            public void run() {
                LOG.i("takeVideoSnapshot:", "running. isTakingVideo:", isTakingVideo());
                stub.file = file;
                stub.isSnapshot = true;
                stub.videoCodec = mVideoCodec;
                stub.location = mLocation;
                stub.facing = mFacing;
                stub.videoBitRate = mVideoBitRate;
                stub.audioBitRate = mAudioBitRate;
                stub.audio = mAudio;
                stub.maxSize = mVideoMaxSize;
                stub.maxDuration = mVideoMaxDuration;
                //noinspection ConstantConditions
                AspectRatio ratio = AspectRatio.of(getPreviewSurfaceSize(Reference.OUTPUT));
                onTakeVideoSnapshot(stub, ratio);
            }
        });
    }

    public final void stopVideo() {
        mOrchestrator.schedule("stop video", true, new Runnable() {
            @Override
            public void run() {
                LOG.i("stopVideo", "running. isTakingVideo?", isTakingVideo());
                onStopVideo();
            }
        });
    }

    @EngineThread
    @SuppressWarnings("WeakerAccess")
    protected void onStopVideo() {
        if (mVideoRecorder != null) {
            mVideoRecorder.stop(false);
            // Do not null this, so we respond correctly to isTakingVideo(),
            // which checks for recorder presence and recorder.isRecording().
            // It will be nulled in onVideoResult.
        }
    }

    @CallSuper
    @Override
    public void onVideoResult(@Nullable VideoResult.Stub result, @Nullable Exception exception) {
        mVideoRecorder = null;
        if (result != null) {
            mCallback.dispatchOnVideoTaken(result);
        } else {
            LOG.e("onVideoResult", "result is null: something went wrong.", exception);
            mCallback.dispatchError(new CameraException(exception,
                    CameraException.REASON_VIDEO_FAILED));
        }
    }

    @Override
    public void onVideoRecordingStart() {
        mCallback.dispatchOnVideoRecordingStart();
    }

    @Override
    public void onVideoRecordingEnd() {
        mCallback.dispatchOnVideoRecordingEnd();
    }

    @EngineThread
    protected abstract void onTakePicture(@NonNull PictureResult.Stub stub, boolean doMetering);

    @EngineThread
    protected abstract void onTakePictureSnapshot(@NonNull PictureResult.Stub stub,
                                                  @NonNull AspectRatio outputRatio,
                                                  boolean doMetering);

    @EngineThread
    protected abstract void onTakeVideoSnapshot(@NonNull VideoResult.Stub stub,
                                                @NonNull AspectRatio outputRatio);

    @EngineThread
    protected abstract void onTakeVideo(@NonNull VideoResult.Stub stub);

    //endregion

    //region Size utilities

    @Nullable
    public final Size getPictureSize(@SuppressWarnings("SameParameterValue") @NonNull Reference reference) {
        Size size = mCaptureSize;
        if (size == null || mMode == Mode.VIDEO) return null;
        return getAngles().flip(Reference.SENSOR, reference) ? size.flip() : size;
    }

    @Nullable
    public final Size getVideoSize(@SuppressWarnings("SameParameterValue") @NonNull Reference reference) {
        Size size = mCaptureSize;
        if (size == null || mMode == Mode.PICTURE) return null;
        return getAngles().flip(Reference.SENSOR, reference) ? size.flip() : size;
    }

    @Nullable
    public final Size getPreviewStreamSize(@NonNull Reference reference) {
        Size size = mPreviewStreamSize;
        if (size == null) return null;
        return getAngles().flip(Reference.SENSOR, reference) ? size.flip() : size;
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable
    private Size getPreviewSurfaceSize(@NonNull Reference reference) {
        CameraPreview preview = mPreview;
        if (preview == null) return null;
        return getAngles().flip(Reference.VIEW, reference) ? preview.getSurfaceSize().flip()
                : preview.getSurfaceSize();
    }

    /**
     * Returns the snapshot size, but not cropped with the view dimensions, which
     * is what we will do before creating the snapshot. However, cropping is done at various
     * levels so we don't want to perform the op here.
     *
     * The base snapshot size is based on PreviewStreamSize (later cropped with view ratio). Why?
     * One might be tempted to say that it's the SurfaceSize (which already matches the view ratio).
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
     *
     * @param reference the reference system
     * @return the uncropped snapshot size
     */
    @Nullable
    public final Size getUncroppedSnapshotSize(@NonNull Reference reference) {
        Size baseSize = getPreviewStreamSize(reference);
        if (baseSize == null) return null;
        boolean flip = getAngles().flip(reference, Reference.VIEW);
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

    /**
     * This is called either on cameraView.start(), or when the underlying surface changes.
     * It is possible that in the first call the preview surface has not already computed its
     * dimensions.
     * But when it does, the {@link CameraPreview.SurfaceCallback} should be called,
     * and this should be refreshed.
     *
     * @return the capture size
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    protected final Size computeCaptureSize() {
        return computeCaptureSize(mMode);
    }

    @SuppressWarnings("WeakerAccess")
    protected final Size computeCaptureSize(@NonNull Mode mode) {
        // We want to pass stuff into the REF_VIEW reference, not the sensor one.
        // This is already managed by CameraOptions, so we just flip again at the end.
        boolean flip = getAngles().flip(Reference.SENSOR, Reference.VIEW);
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
        if (!list.contains(result)) {
            throw new RuntimeException("SizeSelectors must not return Sizes other than " +
                    "those in the input list.");
        }
        LOG.i("computeCaptureSize:", "result:", result, "flip:", flip, "mode:", mode);
        if (flip) result = result.flip(); // Go back to REF_SENSOR
        return result;
    }

    /**
     * This is called anytime {@link #computePreviewStreamSize()} is called.
     * This means that it should be called during the binding process, when
     * we can be sure that the camera is available (engineState == STARTED).
     * @return a list of available sizes for preview
     */
    @EngineThread
    @NonNull
    protected abstract List<Size> getPreviewStreamAvailableSizes();

    @EngineThread
    @NonNull
    @SuppressWarnings("WeakerAccess")
    protected final Size computePreviewStreamSize() {
        @NonNull List<Size> previewSizes = getPreviewStreamAvailableSizes();
        // These sizes come in REF_SENSOR. Since there is an external selector involved,
        // we must convert all of them to REF_VIEW, then flip back when returning.
        boolean flip = getAngles().flip(Reference.SENSOR, Reference.VIEW);
        List<Size> sizes = new ArrayList<>(previewSizes.size());
        for (Size size : previewSizes) {
            sizes.add(flip ? size.flip() : size);
        }

        // Create our own default selector, which will be used if the external
        // mPreviewStreamSizeSelector is null, or if it fails in finding a size.
        Size targetMinSize = getPreviewSurfaceSize(Reference.VIEW);
        if (targetMinSize == null) {
            throw new IllegalStateException("targetMinSize should not be null here.");
        }
        AspectRatio targetRatio = AspectRatio.of(mCaptureSize.getWidth(), mCaptureSize.getHeight());
        if (flip) targetRatio = targetRatio.flip();
        LOG.i("computePreviewStreamSize:",
                "targetRatio:", targetRatio,
                "targetMinSize:", targetMinSize);
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
        if (!sizes.contains(result)) {
            throw new RuntimeException("SizeSelectors must not return Sizes other than " +
                    "those in the input list.");
        }
        if (flip) result = result.flip();
        LOG.i("computePreviewStreamSize:", "result:", result, "flip:", flip);
        return result;
    }

    //endregion
}

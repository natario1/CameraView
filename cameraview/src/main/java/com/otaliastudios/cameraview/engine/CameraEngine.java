package com.otaliastudios.cameraview.engine;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.PointF;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Location;


import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Task;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameManager;
import com.otaliastudios.cameraview.internal.utils.Op;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;


/**
 * PROCESS
 * Setting up the Camera is usually a 4 steps process:
 * 1. Setting up the Surface.            Done by {@link CameraPreview}.
 * 2. Starting the camera.               Done by us. See {@link #startEngine()}, {@link #onStartEngine()}.
 * 3. Binding the camera to the surface. Done by us. See {@link #startBind()}, {@link #onStartBind()}
 * 4. Streaming the camera preview.      Done by us. See {@link #startPreview()}, {@link #onStartPreview()}
 *
 * The first two steps can actually happen at the same time, anyway
 * the order is not guaranteed, we just get a callback from the Preview when 1 happens.
 * So at the end of both step 1 and 2, the engine should check if both have
 * been performed and trigger the steps 3 and 4.
 *
 * We use an abstraction for each step called {@link CameraEngineStep} that manages the state of
 * each step and ensures that start and stop operations, for each step, are never called if the
 * previous one has not ended.
 *
 *
 * STATE
 * We only expose generic {@link #start()} and {@link #stop()} calls to the outside.
 * The external users of this class are most likely interested in whether we have completed step 2
 * or not, since that tells us if we can act on the camera or not, rather than knowing about steps 3 and 4.
 *
 * So in the {@link CameraEngine} notation,
 * - {@link #start()}: ASYNC - starts the engine (S2). When possible, at a later time, S3 and S4 are also performed.
 * - {@link #stop()}: ASYNC - stops everything: undoes S4, then S3, then S2.
 * - {@link #restart()}: ASYNC - completes a stop then a start.
 * - {@link #destroy()}: ASYNC - performs a {@link #stop()} that will go on no matter the exceptions, without throwing.
 *                               Makes the engine unusable and clears resources.
 *
 * For example, we expose the engine (S2) state through {@link #getEngineState()}. It will be:
 * - {@link #STATE_STARTING} if we're into step 2
 * - {@link #STATE_STARTED} if we've completed step 2. No clue about 3 or 4.
 * - {@link #STATE_STOPPING} if we're undoing steps 4, 3 and 2.
 * - {@link #STATE_STOPPED} if we have undone steps 4, 3 and 2 (or they never started at all).
 *
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
 * THe {@link #mHandler} thread has a special {@link Thread.UncaughtExceptionHandler} that handles exceptions
 * and dispatches error to the callback (instead of crashing the app). This lets subclasses run code
 * safely and directly throw {@link CameraException}s when needed.
 *
 * For convenience, the two main method {@link #onStartEngine()} and {@link #onStopEngine()} are already
 * called on the engine thread, but they can still be asynchronous by returning a Google's
 * {@link com.google.android.gms.tasks.Task}.
 */
public abstract class CameraEngine implements
        CameraPreview.SurfaceCallback,
        FrameManager.BufferCallback,
        PictureRecorder.PictureResultListener,
        VideoRecorder.VideoResultListener {

    public interface Callback {
        @NonNull Context getContext();
        void dispatchOnCameraOpened(CameraOptions options);
        void dispatchOnCameraClosed();
        void onCameraPreviewStreamSizeChanged();
        void onShutter(boolean shouldPlaySound);
        void dispatchOnVideoTaken(VideoResult.Stub stub);
        void dispatchOnPictureTaken(PictureResult.Stub stub);
        void dispatchOnFocusStart(@Nullable Gesture trigger, @NonNull PointF where);
        void dispatchOnFocusEnd(@Nullable Gesture trigger, boolean success, @NonNull PointF where);
        void dispatchOnZoomChanged(final float newValue, @Nullable final PointF[] fingers);
        void dispatchOnExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers);
        void dispatchFrame(Frame frame);
        void dispatchError(CameraException exception);
    }

    private static final String TAG = CameraEngine.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressWarnings("WeakerAccess")
    public static final int STATE_STOPPING = CameraEngineStep.STATE_STOPPING;
    public static final int STATE_STOPPED = CameraEngineStep.STATE_STOPPED;
    @SuppressWarnings("WeakerAccess")
    public static final int STATE_STARTING = CameraEngineStep.STATE_STARTING;
    public static final int STATE_STARTED = CameraEngineStep.STATE_STARTED;

    public static final int REF_SENSOR = 0;
    public static final int REF_VIEW = 1;
    public static final int REF_OUTPUT = 2;

    protected final Callback mCallback;
    private final FrameManager mFrameManager;
    protected CameraPreview mPreview;
    protected WorkerHandler mHandler;
    @VisibleForTesting Handler mCrashHandler;

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

    @VisibleForTesting int mSnapshotMaxWidth = Integer.MAX_VALUE; // in REF_VIEW for consistency with SizeSelectors
    @VisibleForTesting int mSnapshotMaxHeight = Integer.MAX_VALUE; // in REF_VIEW for consistency with SizeSelectors

    protected CameraOptions mCameraOptions;
    protected Mapper mMapper;
    protected PictureRecorder mPictureRecorder;
    protected VideoRecorder mVideoRecorder;
    protected long mVideoMaxSize;
    protected int mVideoMaxDuration;
    protected int mVideoBitRate;
    protected int mAudioBitRate;
    protected Size mCaptureSize;
    protected Size mPreviewStreamSize;
    protected long mAutoFocusResetDelayMillis;

    protected int mSensorOffset;
    protected int mDisplayOffset;
    protected int mDeviceOrientation;

    private final CameraEngineStep.Callback mStepCallback = new CameraEngineStep.Callback() {
        @Override @NonNull public Executor getExecutor() { return mHandler.getExecutor(); }
        @Override public void handleException(@NonNull Exception exception) {
            CameraEngine.this.handleException(Thread.currentThread(), exception, false);
        }
    };
    @VisibleForTesting CameraEngineStep mEngineStep = new CameraEngineStep("engine", mStepCallback);
    private CameraEngineStep mBindStep = new CameraEngineStep("bind", mStepCallback);
    private CameraEngineStep mPreviewStep = new CameraEngineStep("preview", mStepCallback);
    private CameraEngineStep mAllStep = new CameraEngineStep("all", mStepCallback);

    // Used for testing.
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    Op<Void> mZoomOp = new Op<>();
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    Op<Void> mExposureCorrectionOp = new Op<>();
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    Op<Void> mFlashOp = new Op<>();
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    Op<Void> mWhiteBalanceOp = new Op<>();
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    Op<Void> mHdrOp = new Op<>();
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    Op<Void> mLocationOp = new Op<>();
    @VisibleForTesting Op<Void> mStartVideoOp = new Op<>();
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    Op<Void> mPlaySoundsOp = new Op<>();

    protected CameraEngine(Callback callback) {
        mCallback = callback;
        mCrashHandler = new Handler(Looper.getMainLooper());
        mHandler = WorkerHandler.get("CameraViewEngine");
        mHandler.getThread().setUncaughtExceptionHandler(new CrashExceptionHandler());
        mFrameManager = new FrameManager(2, this);
    }

    public void setPreview(@NonNull CameraPreview cameraPreview) {
        if (mPreview != null) mPreview.setSurfaceCallback(null);
        mPreview = cameraPreview;
        mPreview.setSurfaceCallback(this);
    }

    //region Error handling

    /**
     * The base exception handler, which inspects the exception and
     * decides what to do.
     */
    private class CrashExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(final Thread thread, final Throwable throwable) {
            handleException(thread, throwable, true);
        }
    }

    /**
     * A static exception handler used during destruction to avoid leaks,
     * since the default handler is not static and the thread might survive the engine.
     */
    private static class NoOpExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            // No-op.
        }
    }

    /**
     * Handles exceptions coming from either runtime errors on the {@link #mHandler} code that is
     * not caught (using the {@link CrashExceptionHandler}), as might happen during standard mHandler.post()
     * operations that subclasses might do, OR for errors caught by tasks and continuations that
     * we launch here.
     *
     * In the first case, the thread is about to be terminated. In the second case,
     * we can actually keep using it.
     *
     * @param thread the thread
     * @param throwable the throwable
     * @param fromExceptionHandler true if coming from exception handler
     */
    private void handleException(@NonNull Thread thread, final @NonNull Throwable throwable, final boolean fromExceptionHandler) {
        if (!(throwable instanceof CameraException)) {
            // This is unexpected, either a bug or something the developer should know.
            // Release and crash the UI thread so we get bug reports.
            LOG.e("uncaughtException:", "Unexpected exception:", throwable);
            mCrashHandler.post(new Runnable() {
                @Override
                public void run() {
                    destroy();
                    // Throws an unchecked exception without unnecessary wrapping.
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    } else {
                        throw new RuntimeException(throwable);
                    }
                }
            });
            return;
        }

        final CameraException cameraException = (CameraException) throwable;
        LOG.e("uncaughtException:", "Got CameraException:", cameraException, "on engine state:", getEngineStateName());
        if (fromExceptionHandler) {
            // Got to restart the handler.
            thread.interrupt();
            mHandler = WorkerHandler.get("CameraViewEngine");
            mHandler.getThread().setUncaughtExceptionHandler(new CrashExceptionHandler());
        }

        mCallback.dispatchError(cameraException);
        if (cameraException.isUnrecoverable()) {
            // Stop everything (if needed) without notifying teardown errors.
            stop(true);
        }
    }

    //endregion

    //region states and steps

    public final int getEngineState() {
        return mEngineStep.getState();
    }

    @SuppressWarnings("WeakerAccess")
    public final int getBindState() {
        return mBindStep.getState();
    }

    @SuppressWarnings("unused")
    public final int getPreviewState() {
        return mPreviewStep.getState();
    }

    @NonNull
    private String getEngineStateName() {
        return mEngineStep.getStateName();
    }

    private boolean canStartEngine() {
        return mEngineStep.isStoppingOrStopped();
    }

    private boolean needsStopEngine() {
        return mEngineStep.isStartedOrStarting();
    }

    private boolean canStartBind() {
        return mEngineStep.isStarted()
                && mPreview != null
                && mPreview.hasSurface()
                && mBindStep.isStoppingOrStopped();
    }

    private boolean needsStopBind() {
        return mBindStep.isStartedOrStarting();
    }

    private boolean canStartPreview() {
        return mEngineStep.isStarted()
                && mBindStep.isStarted()
                && mPreviewStep.isStoppingOrStopped();
    }

    private boolean needsStopPreview() {
        return mPreviewStep.isStartedOrStarting();
    }

    //endregion

    //region Start & Stop the engine

    @NonNull
    @WorkerThread
    private Task<Void> startEngine() {
        if (canStartEngine()) {
            mEngineStep.doStart(false, new Callable<Task<Void>>() {
                @Override
                public Task<Void> call() {
                    if (!collectCameraInfo(mFacing)) {
                        LOG.e("onStartEngine:", "No camera available for facing", mFacing);
                        throw new CameraException(CameraException.REASON_NO_CAMERA);
                    }
                    return onStartEngine();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    mCallback.dispatchOnCameraOpened(mCameraOptions);
                }
            });
        }
        return mEngineStep.getTask();
    }

    @NonNull
    @WorkerThread
    private Task<Void> stopEngine(boolean swallowExceptions) {
        if (needsStopEngine()) {
            mEngineStep.doStop(swallowExceptions, new Callable<Task<Void>>() {
                @Override
                public Task<Void> call() {
                    return onStopEngine();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    mCallback.dispatchOnCameraClosed();
                }
            });
        }
        return mEngineStep.getTask();
    }

    /**
     * Starts the engine.
     * @return a task
     */
    @NonNull
    @WorkerThread
    protected abstract Task<Void> onStartEngine();

    /**
     * Stops the engine.
     * Stop events should generally not throw exceptions. We
     * want to release resources either way.
     * @return a task
     */
    @NonNull
    @WorkerThread
    protected abstract Task<Void> onStopEngine();

    //endregion

    //region Start & Stop binding

    @NonNull
    @WorkerThread
    private Task<Void> startBind() {
        if (canStartBind()) {
            mBindStep.doStart(false, new Callable<Task<Void>>() {
                @Override
                public Task<Void> call() {
                    return onStartBind();
                }
            });
        }
        return mBindStep.getTask();
    }

    @NonNull
    @WorkerThread
    private Task<Void> stopBind(boolean swallowExceptions) {
        if (needsStopBind()) {
            mBindStep.doStop(swallowExceptions, new Callable<Task<Void>>() {
                @Override
                public Task<Void> call() {
                    return onStopBind();
                }
            });
        }
        return mBindStep.getTask();
    }

    /**
     * Starts the binding process.
     * @return a task
     */
    @NonNull
    @WorkerThread
    protected abstract Task<Void> onStartBind();

    /**
     * Stops the binding process.
     * Stop events should generally not throw exceptions. We
     * want to release resources either way.
     * @return a task
     */
    @NonNull
    @WorkerThread
    protected abstract Task<Void> onStopBind();

    @SuppressWarnings("WeakerAccess")
    protected void restartBind() {
        LOG.i("restartPreviewAndBind", "posting.");
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.i("restartPreviewAndBind", "executing.");
                stopPreview(false).continueWithTask(mHandler.getExecutor(), new Continuation<Void, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<Void> task) {
                        return stopBind(false);
                    }
                }).onSuccessTask(mHandler.getExecutor(), new SuccessContinuation<Void, Void>() {
                    @NonNull
                    @Override
                    public Task<Void> then(@Nullable Void aVoid) {
                        return startBind();
                    }
                }).onSuccessTask(mHandler.getExecutor(), new SuccessContinuation<Void, Void>() {
                    @NonNull
                    @Override
                    public Task<Void> then(@Nullable Void aVoid) {
                        return startPreview();
                    }
                });
            }
        });
    }

    //endregion

    //region Start & Stop preview

    @NonNull
    @WorkerThread
    private Task<Void> startPreview() {
        LOG.i("startPreview", "canStartPreview:", canStartPreview());
        if (canStartPreview()) {
            mPreviewStep.doStart(false, new Callable<Task<Void>>() {
                @Override
                public Task<Void> call() {
                    return onStartPreview();
                }
            });
        }
        return mPreviewStep.getTask();
    }

    @NonNull
    @WorkerThread
    private Task<Void> stopPreview(boolean swallowExceptions) {
        LOG.i("stopPreview", "needsStopPreview:", needsStopPreview(), "swallowExceptions:", swallowExceptions);
        if (needsStopPreview()) {
            mPreviewStep.doStop(swallowExceptions, new Callable<Task<Void>>() {
                @Override
                public Task<Void> call() {
                    return onStopPreview();
                }
            });
        }
        return mPreviewStep.getTask();
    }

    @SuppressWarnings("WeakerAccess")
    protected void restartPreview() {
        LOG.i("restartPreview", "posting.");
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.i("restartPreview", "executing.");
                stopPreview(false);
                startPreview();
            }
        });
    }

    /**
     * Starts the preview streaming.
     * @return a task
     */
    @NonNull
    @WorkerThread
    protected abstract Task<Void> onStartPreview();

    /**
     * Stops the preview streaming.
     * Stop events should generally not throw exceptions. We
     * want to release resources either way.
     * @return a task
     */
    @NonNull
    @WorkerThread
    protected abstract Task<Void> onStopPreview();

    //endregion

    //region Surface callbacks

    /**
     * The surface is now available, which means that step 1 has completed.
     * If we have also completed step 2, go on with binding and streaming.
     */
    @Override
    public final void onSurfaceAvailable() {
        LOG.i("onSurfaceAvailable:", "Size is", getPreviewSurfaceSize(REF_VIEW));
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                startBind().onSuccessTask(mHandler.getExecutor(), new SuccessContinuation<Void, Void>() {
                    @NonNull
                    @Override
                    public Task<Void> then(@Nullable Void aVoid) {
                        return startPreview();
                    }
                });
            }
        });
    }

    @Override
    public final void onSurfaceChanged() {
        LOG.i("onSurfaceChanged:", "Size is", getPreviewSurfaceSize(REF_VIEW), "Posting.");
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.i("onSurfaceChanged:",
                        "Engine started?", mEngineStep.isStarted(),
                        "Bind started?", mBindStep.isStarted());
                if (!mEngineStep.isStarted()) return; // Too early
                if (!mBindStep.isStarted()) return; // Too early

                // Compute a new camera preview size and apply.
                Size newSize = computePreviewStreamSize();
                if (newSize.equals(mPreviewStreamSize)) return;
                LOG.i("onSurfaceChanged:", "Computed a new preview size. Going on.");
                mPreviewStreamSize = newSize;
                onPreviewStreamSizeChanged();
            }
        });
    }

    /**
     * The preview stream size has changed. At this point, some engine might want to
     * simply call {@link #restartPreview()}, others to {@link #restartBind()}.
     *
     * It basically depends on the step at which the preview stream size is actually used.
     */
    @WorkerThread
    protected abstract void onPreviewStreamSizeChanged();

    @Override
    public final void onSurfaceDestroyed() {
        LOG.i("onSurfaceDestroyed");
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                stopPreview(false).onSuccessTask(mHandler.getExecutor(), new SuccessContinuation<Void, Void>() {
                    @NonNull
                    @Override
                    public Task<Void> then(@Nullable Void aVoid) {
                        return stopBind(false);
                    }
                });
            }
        });
    }

    //endregion

    //region Start & Stop all

    /**
     * Not final due to mockito requirements, but this is basically
     * it, nothing more to do.
     *
     * NOTE: Should not be called on the {@link #mHandler} thread! I think
     * that would cause deadlocks due to us awaiting for {@link #stop()} to return.
     */
    public void destroy() {
        LOG.i("destroy:", "state:", getEngineStateName());
        // Prevent CameraEngine leaks. Don't set to null, or exceptions
        // inside the standard stop() method might crash the main thread.
        mHandler.getThread().setUncaughtExceptionHandler(new NoOpExceptionHandler());
        // Stop if needed, synchronously and silently.
        // Cannot use Tasks.await() because we might be on the UI thread.
        final CountDownLatch latch = new CountDownLatch(1);
        stop(true).addOnCompleteListener(mHandler.getExecutor(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException ignore) {}
    }

    @SuppressWarnings("WeakerAccess")
    protected final void restart() {
        LOG.i("Restart:", "calling stop and start");
        stop();
        start();
    }

    @NonNull
    public Task<Void> start() {
        LOG.i("Start:", "posting runnable. State:", getEngineStateName());
        final TaskCompletionSource<Void> outTask = new TaskCompletionSource<>();
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.i("Start:", "executing runnable. State:", getEngineStateName());
                if (mAllStep.isStoppingOrStopped()) {
                    mAllStep.doStart(false, new Callable<Task<Void>>() {
                        @Override
                        public Task<Void> call() {
                            return startEngine().addOnFailureListener(mHandler.getExecutor(), new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    outTask.trySetException(e);
                                }
                            }).onSuccessTask(mHandler.getExecutor(), new SuccessContinuation<Void, Void>() {
                                @NonNull
                                @Override
                                public Task<Void> then(@Nullable Void aVoid) {
                                    outTask.trySetResult(null);
                                    return startBind();
                                }
                            }).onSuccessTask(mHandler.getExecutor(), new SuccessContinuation<Void, Void>() {
                                @NonNull
                                @Override
                                public Task<Void> then(@Nullable Void aVoid) {
                                    return startPreview();
                                }
                            });
                        }
                    });
                } else {
                    // NOTE: this returns early if we were STARTING.
                    outTask.trySetResult(null);
                }
            }
        });
        return outTask.getTask();
    }

    @NonNull
    public Task<Void> stop() {
        return stop(false);
    }

    @NonNull
    private Task<Void> stop(final boolean swallowExceptions) {
        LOG.i("Stop:", "posting runnable. State:", getEngineStateName());
        final TaskCompletionSource<Void> outTask = new TaskCompletionSource<>();
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.i("Stop:", "executing runnable. State:", getEngineStateName());
                if (mAllStep.isStartedOrStarting()) {
                    mAllStep.doStop(swallowExceptions, new Callable<Task<Void>>() {
                        @Override
                        public Task<Void> call() {
                            return stopPreview(swallowExceptions).continueWithTask(mHandler.getExecutor(), new Continuation<Void, Task<Void>>() {
                                @Override
                                public Task<Void> then(@NonNull Task<Void> task) {
                                    return stopBind(swallowExceptions);
                                }
                            }).continueWithTask(mHandler.getExecutor(), new Continuation<Void, Task<Void>>() {
                                @Override
                                public Task<Void> then(@NonNull Task<Void> task) {
                                    return stopEngine(swallowExceptions);
                                }
                            }).continueWithTask(mHandler.getExecutor(), new Continuation<Void, Task<Void>>() {
                                @Override
                                public Task<Void> then(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        outTask.trySetResult(null);
                                    } else {
                                        //noinspection ConstantConditions
                                        outTask.trySetException(task.getException());
                                    }
                                    return task;
                                }
                            });
                        }
                    });
                } else {
                    // NOTE: this returns early if we were STOPPING.
                    outTask.trySetResult(null);
                }
            }
        });
        return outTask.getTask();
    }

    //endregion

    //region final setters

    // This is called before start() and never again.
    public final void setDisplayOffset(int displayOffset) {
        mDisplayOffset = displayOffset;
    }

    // This can be called multiple times.
    public final void setDeviceOrientation(int deviceOrientation) {
        mDeviceOrientation = deviceOrientation;
    }

    public final void setPreviewStreamSizeSelector(@Nullable SizeSelector selector) {
        mPreviewStreamSizeSelector = selector;
    }

    public final void setPictureSizeSelector(@NonNull SizeSelector selector) {
        mPictureSizeSelector = selector;
    }

    public final void setVideoSizeSelector(@NonNull SizeSelector selector) {
        mVideoSizeSelector = selector;
    }

    public final void setVideoMaxSize(long videoMaxSizeBytes) {
        mVideoMaxSize = videoMaxSizeBytes;
    }

    public final void setVideoMaxDuration(int videoMaxDurationMillis) {
        mVideoMaxDuration = videoMaxDurationMillis;
    }

    public final void setVideoCodec(@NonNull VideoCodec codec) {
        mVideoCodec = codec;
    }

    public final void setVideoBitRate(int videoBitRate) {
        mVideoBitRate = videoBitRate;
    }

    public final void setAudioBitRate(int audioBitRate) {
        mAudioBitRate = audioBitRate;
    }

    public final void setSnapshotMaxWidth(int maxWidth) {
        mSnapshotMaxWidth = maxWidth;
    }

    public final void setSnapshotMaxHeight(int maxHeight) {
        mSnapshotMaxHeight = maxHeight;
    }

    public final void setAutoFocusResetDelay(long delayMillis) { mAutoFocusResetDelayMillis = delayMillis; }

    /**
     * Sets a new facing value. This will restart the session (if there's any)
     * so that we can open the new facing camera.
     * @param facing facing
     */
    public final void setFacing(final @NonNull Facing facing) {
        final Facing old = mFacing;
        if (facing != old) {
            mFacing = facing;
            mHandler.run(new Runnable() {
                @Override
                public void run() {
                    if (getEngineState() < STATE_STARTED) return;
                    if (collectCameraInfo(facing)) {
                        restart();
                    } else {
                        mFacing = old;
                    }
                }
            });
        }
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

    /**
     * Sets the desired mode (either picture or video).
     * @param mode desired mode.
     */
    public final void setMode(@NonNull Mode mode) {
        if (mode != mMode) {
            mMode = mode;
            mHandler.run(new Runnable() {
                @Override
                public void run() {
                    if (getEngineState() == STATE_STARTED) {
                        restart();
                    }
                }
            });
        }
    }

    //endregion

    //region Abstract setters and APIs

    /**
     * Camera is about to be opened. Implementors should look into available cameras
     * and see if anyone matches the given {@link Facing value}.
     *
     * If so, implementors should set {@link #mSensorOffset} and any other information
     * (like camera ID) needed to start teh engine.
     *
     * @param facing the facing value
     * @return true if we have one
     */
    protected abstract boolean collectCameraInfo(@NonNull Facing facing);

    // If closed, no-op. If opened, check supported and apply.
    public abstract void setZoom(float zoom, @Nullable PointF[] points, boolean notify);

    // If closed, no-op. If opened, check supported and apply.
    public abstract void setExposureCorrection(float EVvalue, @NonNull float[] bounds, @Nullable PointF[] points, boolean notify);

    // If closed, keep. If opened, check supported and apply.
    public abstract void setFlash(@NonNull Flash flash);

    // If closed, keep. If opened, check supported and apply.
    public abstract void setWhiteBalance(@NonNull WhiteBalance whiteBalance);

    // If closed, keep. If opened, check supported and apply.
    public abstract void setHdr(@NonNull Hdr hdr);

    // If closed, keep. If opened, check supported and apply.
    public abstract void setLocation(@Nullable Location location);

    public abstract void startAutoFocus(@Nullable Gesture gesture, @NonNull PointF point);

    public abstract void setPlaySounds(boolean playSounds);

    //endregion

    //region picture and video control

    public abstract void takePicture(@NonNull PictureResult.Stub stub);

    /**
     * The snapshot size is the {@link #getPreviewStreamSize(int)}, but cropped based on the
     * view/surface aspect ratio.
     * @param stub a picture stub
     * @param viewAspectRatio the view aspect ratio
     */
    public final void takePictureSnapshot(final @NonNull PictureResult.Stub stub, @NonNull final AspectRatio viewAspectRatio) {
        LOG.v("takePictureSnapshot", "scheduling");
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.v("takePictureSnapshot", "performing. Engine:", getEngineStateName(), "isTakingPicture:", isTakingPicture());
                if (getEngineState() < STATE_STARTED) return;
                if (isTakingPicture()) return;
                stub.location = mLocation;
                stub.isSnapshot = true;
                stub.facing = mFacing;
                // Leave the other parameters to subclasses.
                LOG.v("takePictureSnapshot", "Rotations", "SV", offset(REF_SENSOR, REF_VIEW), "VS", offset(REF_VIEW, REF_SENSOR));
                LOG.v("takePictureSnapshot", "Rotations", "SO", offset(REF_SENSOR, REF_OUTPUT), "OS", offset(REF_OUTPUT, REF_SENSOR));
                LOG.v("takePictureSnapshot", "Rotations", "VO", offset(REF_VIEW, REF_OUTPUT), "OV", offset(REF_OUTPUT, REF_VIEW));
                onTakePictureSnapshot(stub, viewAspectRatio);
            }
        });
    }

    @Override
    public void onPictureShutter(boolean didPlaySound) {
        mCallback.onShutter(!didPlaySound);
    }

    @Override
    public void onPictureResult(@Nullable PictureResult.Stub result) {
        mPictureRecorder = null;
        if (result != null) {
            mCallback.dispatchOnPictureTaken(result);
        } else {
            // Something went wrong.
            LOG.e("onPictureResult", "result is null: something went wrong.");
            mCallback.dispatchError(new CameraException(CameraException.REASON_PICTURE_FAILED));
        }
    }

    public final void takeVideo(final @NonNull VideoResult.Stub stub, final @NonNull File file) {
        LOG.v("takeVideo", "scheduling");
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.v("takeVideo", "performing. Engine:", getEngineStateName(), "isTakingVideo:", isTakingVideo());
                if (getEngineState() < STATE_STARTED) { mStartVideoOp.end(null); return; }
                if (isTakingVideo()) { mStartVideoOp.end(null); return; }
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
                mStartVideoOp.end(null);
            }
        });
    }

    /**
     * @param stub a video stub
     * @param file the output file
     * @param viewAspectRatio the view aspect ratio
     */
    public final void takeVideoSnapshot(final @NonNull VideoResult.Stub stub, @NonNull final File file, @NonNull final AspectRatio viewAspectRatio) {
        LOG.v("takeVideoSnapshot", "scheduling");
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.v("takeVideoSnapshot", "performing. Engine:", getEngineStateName(), "isTakingVideo:", isTakingVideo());
                if (getEngineState() < STATE_STARTED) { mStartVideoOp.end(null); return; }
                if (isTakingVideo()) { mStartVideoOp.end(null); return; }
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
                onTakeVideoSnapshot(stub, viewAspectRatio);
                mStartVideoOp.end(null);
            }
        });
    }

    public final void stopVideo() {
        LOG.i("stopVideo", "posting");
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.i("stopVideo", "executing.", "isTakingVideo?", isTakingVideo());
                if (mVideoRecorder != null) {
                    mVideoRecorder.stop();
                }
            }
        });
    }

    @Override
    public void onVideoResult(@Nullable VideoResult.Stub result, @Nullable Exception exception) {
        mVideoRecorder = null;
        if (result != null) {
            mCallback.dispatchOnVideoTaken(result);
        } else {
            LOG.e("onVideoResult", "result is null: something went wrong.", exception);
            mCallback.dispatchError(new CameraException(exception, CameraException.REASON_VIDEO_FAILED));
        }
    }

    @WorkerThread
    protected abstract void onTakePictureSnapshot(@NonNull PictureResult.Stub stub, @NonNull AspectRatio viewAspectRatio);

    @WorkerThread
    protected abstract void onTakeVideoSnapshot(@NonNull VideoResult.Stub stub, @NonNull AspectRatio viewAspectRatio);

    @WorkerThread
    protected abstract void onTakeVideo(@NonNull VideoResult.Stub stub);

    //endregion

    //region final getters

    @NonNull
    public final FrameManager getFrameManager() {
        return mFrameManager;
    }

    @Nullable
    public final CameraOptions getCameraOptions() {
        return mCameraOptions;
    }

    @NonNull
    public final Facing getFacing() {
        return mFacing;
    }

    @NonNull
    public final Flash getFlash() {
        return mFlash;
    }

    @NonNull
    public final WhiteBalance getWhiteBalance() {
        return mWhiteBalance;
    }

    public final VideoCodec getVideoCodec() {
        return mVideoCodec;
    }

    public final int getVideoBitRate() {
        return mVideoBitRate;
    }

    public final long getVideoMaxSize() {
        return mVideoMaxSize;
    }

    public final int getVideoMaxDuration() {
        return mVideoMaxDuration;
    }

    @NonNull
    public final Mode getMode() {
        return mMode;
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
    public final Audio getAudio() {
        return mAudio;
    }

    public final int getAudioBitRate() {
        return mAudioBitRate;
    }

    @SuppressWarnings("unused")
    @Nullable
    @VisibleForTesting
    final SizeSelector getPreviewStreamSizeSelector() {
        return mPreviewStreamSizeSelector;
    }

    @SuppressWarnings("unused")
    @NonNull
    public final SizeSelector getPictureSizeSelector() {
        return mPictureSizeSelector;
    }

    @SuppressWarnings("unused")
    @NonNull
    public final SizeSelector getVideoSizeSelector() {
        return mVideoSizeSelector;
    }

    public final float getZoomValue() {
        return mZoomValue;
    }

    public final float getExposureCorrectionValue() {
        return mExposureCorrectionValue;
    }

    public final boolean isTakingVideo() {
        return mVideoRecorder != null && mVideoRecorder.isRecording();
    }

    public final boolean isTakingPicture() {
        return mPictureRecorder != null;
    }

    public final long getAutoFocusResetDelay() { return mAutoFocusResetDelayMillis; }

    final boolean shouldResetAutoFocus() {
        return mAutoFocusResetDelayMillis > 0 && mAutoFocusResetDelayMillis != Long.MAX_VALUE;
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

    // o(S, V) - o(S, O)
    // displayOffset - deviceOrientation

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

    public final boolean flip(int reference1, int reference2) {
        return offset(reference1, reference2) % 180 != 0;
    }

    @Nullable
    public final Size getPictureSize(@SuppressWarnings("SameParameterValue") int reference) {
        if (mCaptureSize == null || mMode == Mode.VIDEO) return null;
        return flip(REF_SENSOR, reference) ? mCaptureSize.flip() : mCaptureSize;
    }

    @Nullable
    public final Size getVideoSize(@SuppressWarnings("SameParameterValue") int reference) {
        if (mCaptureSize == null || mMode == Mode.PICTURE) return null;
        return flip(REF_SENSOR, reference) ? mCaptureSize.flip() : mCaptureSize;
    }

    @Nullable
    public final Size getPreviewStreamSize(int reference) {
        if (mPreviewStreamSize == null) return null;
        return flip(REF_SENSOR, reference) ? mPreviewStreamSize.flip() : mPreviewStreamSize;
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable
    private Size getPreviewSurfaceSize(int reference) {
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
    public final Size getUncroppedSnapshotSize(int reference) {
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
        if (!list.contains(result)) {
            throw new RuntimeException("SizeSelectors must not return Sizes other than those in the input list.");
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
    @NonNull
    protected abstract List<Size> getPreviewStreamAvailableSizes();

    @NonNull
    @SuppressWarnings("WeakerAccess")
    protected final Size computePreviewStreamSize() {
        @NonNull List<Size> previewSizes = getPreviewStreamAvailableSizes();
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
        if (targetMinSize == null) throw new IllegalStateException("targetMinSize should not be null here.");
        AspectRatio targetRatio = AspectRatio.of(mCaptureSize.getWidth(), mCaptureSize.getHeight());
        if (flip) targetRatio = targetRatio.flip();
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
        if (!sizes.contains(result)) {
            throw new RuntimeException("SizeSelectors must not return Sizes other than those in the input list.");
        }
        if (flip) result = result.flip();
        LOG.i("computePreviewStreamSize:", "result:", result, "flip:", flip);
        return result;
    }

    //endregion
}

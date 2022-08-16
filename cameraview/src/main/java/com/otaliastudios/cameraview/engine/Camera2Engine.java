package com.otaliastudios.cameraview.engine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.util.Pair;
import android.util.Range;
import android.util.Rational;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.PictureFormat;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.engine.action.Action;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.Actions;
import com.otaliastudios.cameraview.engine.action.BaseAction;
import com.otaliastudios.cameraview.engine.action.CompletionCallback;
import com.otaliastudios.cameraview.engine.action.LogAction;
import com.otaliastudios.cameraview.engine.mappers.Camera2Mapper;
import com.otaliastudios.cameraview.engine.meter.MeterAction;
import com.otaliastudios.cameraview.engine.meter.MeterResetAction;
import com.otaliastudios.cameraview.engine.offset.Axis;
import com.otaliastudios.cameraview.engine.offset.Reference;
import com.otaliastudios.cameraview.engine.options.Camera2Options;
import com.otaliastudios.cameraview.engine.orchestrator.CameraState;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameManager;
import com.otaliastudios.cameraview.frame.ImageFrameManager;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.internal.CropHelper;
import com.otaliastudios.cameraview.internal.FpsRangeValidator;
import com.otaliastudios.cameraview.metering.MeteringRegions;
import com.otaliastudios.cameraview.picture.Full2PictureRecorder;
import com.otaliastudios.cameraview.picture.Snapshot2PictureRecorder;
import com.otaliastudios.cameraview.preview.RendererCameraPreview;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.video.Full2VideoRecorder;
import com.otaliastudios.cameraview.video.SnapshotVideoRecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Engine extends CameraBaseEngine implements
        ImageReader.OnImageAvailableListener,
        ActionHolder {

    private static final int FRAME_PROCESSING_FORMAT = ImageFormat.YUV_420_888;
    @VisibleForTesting static final long METER_TIMEOUT = 5000;
    private static final long METER_TIMEOUT_SHORT = 2500;

    private final CameraManager mManager;
    private String mCameraId;
    private CameraDevice mCamera;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mRepeatingRequestBuilder;
    private TotalCaptureResult mLastRepeatingResult;
    private final Camera2Mapper mMapper = Camera2Mapper.get();

    // Frame processing
    private ImageReader mFrameProcessingReader; // need this or the reader surface is collected
    private Surface mFrameProcessingSurface;

    // Preview
    private Surface mPreviewStreamSurface;

    // Video recording
    // When takeVideo is called, we restart the session.
    private VideoResult.Stub mFullVideoPendingStub;

    // Picture capturing
    private ImageReader mPictureReader;
    private final boolean mPictureCaptureStopsPreview = false; // can be configurable at some point

    // Actions
    // Use COW to properly synchronize the list. We'll iterate much more than mutate
    private final List<Action> mActions = new CopyOnWriteArrayList<>();
    private MeterAction mMeterAction;

    public Camera2Engine(Callback callback) {
        super(callback);
        mManager = (CameraManager) getCallback().getContext()
                .getSystemService(Context.CAMERA_SERVICE);
        new LogAction().start(this);
    }

    //region Utilities

    @VisibleForTesting
    @NonNull
    <T> T readCharacteristic(@NonNull CameraCharacteristics.Key<T> key,
                             @NonNull T fallback) {
        return readCharacteristic(mCameraCharacteristics, key, fallback);
    }

    @NonNull
    private <T> T readCharacteristic(@NonNull CameraCharacteristics characteristics,
                             @NonNull CameraCharacteristics.Key<T> key,
                             @NonNull T fallback) {
        T value = characteristics.get(key);
        return value == null ? fallback : value;
    }

    @NonNull
    private CameraException createCameraException(@NonNull CameraAccessException exception) {
        int reason;
        switch (exception.getReason()) {
            case CameraAccessException.CAMERA_DISABLED:
            case CameraAccessException.CAMERA_IN_USE:
            case CameraAccessException.MAX_CAMERAS_IN_USE: {
                reason = CameraException.REASON_FAILED_TO_CONNECT;
                break;
            }
            case CameraAccessException.CAMERA_ERROR:
            case CameraAccessException.CAMERA_DISCONNECTED: {
                reason = CameraException.REASON_DISCONNECTED;
                break;
            }
            default: {
                reason = CameraException.REASON_UNKNOWN;
                break;
            }
        }
        return new CameraException(exception, reason);
    }

    @NonNull
    private CameraException createCameraException(int stateCallbackError) {
        int reason;
        switch (stateCallbackError) {
            case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED: // Device policy
            case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE: // Fatal error
            case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE: // Fatal error, might have to
                // restart the device
            case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
            case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE: {
                reason = CameraException.REASON_FAILED_TO_CONNECT;
                break;
            }
            default: {
                reason = CameraException.REASON_UNKNOWN;
                break;
            }
        }
        return new CameraException(reason);
    }

    /**
     * When creating a new builder, we want to
     * - set it to {@link #mRepeatingRequestBuilder}, the current one
     * - add a tag for the template just in case
     * - apply all the current parameters
     */
    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    private CaptureRequest.Builder createRepeatingRequestBuilder(int template)
            throws CameraAccessException {
        CaptureRequest.Builder oldBuilder = mRepeatingRequestBuilder;
        mRepeatingRequestBuilder = mCamera.createCaptureRequest(template);
        mRepeatingRequestBuilder.setTag(template);
        applyAllParameters(mRepeatingRequestBuilder, oldBuilder);
        return mRepeatingRequestBuilder;
    }

    /**
     * Sets up the repeating request builder with default surfaces and extra ones
     * if needed (like a video recording surface).
     */
    private void addRepeatingRequestBuilderSurfaces(@NonNull Surface... extraSurfaces) {
        mRepeatingRequestBuilder.addTarget(mPreviewStreamSurface);
        if (mFrameProcessingSurface != null) {
            mRepeatingRequestBuilder.addTarget(mFrameProcessingSurface);
        }
        for (Surface extraSurface : extraSurfaces) {
            if (extraSurface == null) {
                throw new IllegalArgumentException("Should not add a null surface.");
            }
            mRepeatingRequestBuilder.addTarget(extraSurface);
        }
    }

    /**
     * Removes default surfaces from the repeating request builder.
     */
    private void removeRepeatingRequestBuilderSurfaces() {
        mRepeatingRequestBuilder.removeTarget(mPreviewStreamSurface);
        if (mFrameProcessingSurface != null) {
            mRepeatingRequestBuilder.removeTarget(mFrameProcessingSurface);
        }
    }

    /**
     * Can be changed to select something different than {@link CameraDevice#TEMPLATE_PREVIEW}
     * for the default repeating request.
     * @return the default template for preview
     */
    protected int getRepeatingRequestDefaultTemplate() {
        return CameraDevice.TEMPLATE_PREVIEW;
    }

    /**
     * Applies the repeating request builder to the preview, assuming we actually have a preview
     * running. Can be called after changing parameters to the builder.
     *
     * To apply a new builder (for example switch between TEMPLATE_PREVIEW and TEMPLATE_RECORD)
     * it should be set before calling this method, for example by calling
     * {@link #createRepeatingRequestBuilder(int)}.
     */
    @EngineThread
    @SuppressWarnings("WeakerAccess")
    protected void applyRepeatingRequestBuilder() {
        applyRepeatingRequestBuilder(true, CameraException.REASON_DISCONNECTED);
    }

    @EngineThread
    private void applyRepeatingRequestBuilder(boolean checkStarted, int errorReason) {
        if ((getState() == CameraState.PREVIEW && !isChangingState()) || !checkStarted) {
            try {
                mSession.setRepeatingRequest(mRepeatingRequestBuilder.build(),
                        mRepeatingRequestCallback, null);
            } catch (CameraAccessException e) {
                throw new CameraException(e, errorReason);
            } catch (IllegalStateException e) {
                // mSession is invalid - has been closed. This is extremely worrying because
                // it means that the session state and getPreviewState() are not synced.
                // This probably signals an error in the setup/teardown synchronization.
                LOG.e("applyRepeatingRequestBuilder: session is invalid!", e,
                        "checkStarted:", checkStarted,
                        "currentThread:", Thread.currentThread().getName(),
                        "state:", getState(),
                        "targetState:", getTargetState());
                throw new CameraException(CameraException.REASON_DISCONNECTED);
            }
        }
    }

    private final CameraCaptureSession.CaptureCallback mRepeatingRequestCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                     @NonNull CaptureRequest request,
                                     long timestamp,
                                     long frameNumber) {
            for (Action action : mActions) {
                action.onCaptureStarted(Camera2Engine.this, request);
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            for (Action action : mActions) {
                action.onCaptureProgressed(Camera2Engine.this, request, partialResult);
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            mLastRepeatingResult = result;
            for (Action action : mActions) {
                action.onCaptureCompleted(Camera2Engine.this, request, result);
            }
        }
    };

    //endregion

    //region Protected APIs

    @EngineThread
    @NonNull
    @Override
    protected List<Size> getPreviewStreamAvailableSizes() {
        try {
            CameraCharacteristics characteristics = mManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap streamMap =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (streamMap == null) {
                throw new RuntimeException("StreamConfigurationMap is null. Should not happen.");
            }
            // This works because our previews return either a SurfaceTexture or a SurfaceHolder,
            // which are accepted class types by the getOutputSizes method.
            android.util.Size[] sizes = streamMap.getOutputSizes(mPreview.getOutputClass());
            List<Size> candidates = new ArrayList<>(sizes.length);
            for (android.util.Size size : sizes) {
                Size add = new Size(size.getWidth(), size.getHeight());
                if (!candidates.contains(add)) candidates.add(add);
            }
            return candidates;
        } catch (CameraAccessException e) {
            throw createCameraException(e);
        }
    }

    @EngineThread
    @NonNull
    @Override
    protected List<Size> getFrameProcessingAvailableSizes() {
        try {
            CameraCharacteristics characteristics = mManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap streamMap =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (streamMap == null) {
                throw new RuntimeException("StreamConfigurationMap is null. Should not happen.");
            }
            android.util.Size[] sizes = streamMap.getOutputSizes(mFrameProcessingFormat);
            List<Size> candidates = new ArrayList<>(sizes.length);
            for (android.util.Size size : sizes) {
                Size add = new Size(size.getWidth(), size.getHeight());
                if (!candidates.contains(add)) candidates.add(add);
            }
            return candidates;
        } catch (CameraAccessException e) {
            throw createCameraException(e);
        }
    }

    @EngineThread
    @Override
    protected void onPreviewStreamSizeChanged() {
        LOG.i("onPreviewStreamSizeChanged:", "Calling restartBind().");
        restartBind();
    }

    @EngineThread
    @Override
    protected final boolean collectCameraInfo(@NonNull Facing facing) {
        int internalFacing = mMapper.mapFacing(facing);
        String[] cameraIds = null;
        try {
            cameraIds = mManager.getCameraIdList();
        } catch (CameraAccessException e) {
            // This should never happen, I don't see how it could crash here.
            // However, let's launch an unrecoverable exception.
            throw createCameraException(e);
        }
        LOG.i("collectCameraInfo", "Facing:", facing,
                "Internal:", internalFacing,
                "Cameras:", cameraIds.length);
        for (String cameraId : cameraIds) {
            try {
                CameraCharacteristics characteristics = mManager.getCameraCharacteristics(cameraId);
                if (internalFacing == readCharacteristic(characteristics,
                        CameraCharacteristics.LENS_FACING, -99)) {
                    mCameraId = cameraId;
                    int sensorOffset = readCharacteristic(characteristics,
                            CameraCharacteristics.SENSOR_ORIENTATION, 0);
                    getAngles().setSensorOffset(facing, sensorOffset);
                    return true;
                }
            } catch (CameraAccessException ignore) {
                // This specific camera has been disconnected.
                // Keep searching in other camerIds.
            }
        }
        return false;
    }

    //endregion

    //region Start

    @EngineThread
    @SuppressLint("MissingPermission")
    @NonNull
    @Override
    protected Task<CameraOptions> onStartEngine() {
        final TaskCompletionSource<CameraOptions> task = new TaskCompletionSource<>();
        try {
            // We have a valid camera for this Facing. Go on.
            mManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCamera = camera;

                    // Set parameters that might have been set before the camera was opened.
                    try {
                        LOG.i("onStartEngine:", "Opened camera device.");
                        mCameraCharacteristics = mManager.getCameraCharacteristics(mCameraId);
                        boolean flip = getAngles().flip(Reference.SENSOR, Reference.VIEW);
                        int format;
                        switch (mPictureFormat) {
                            case JPEG: format = ImageFormat.JPEG; break;
                            case DNG: format = ImageFormat.RAW_SENSOR; break;
                            default: throw new IllegalArgumentException("Unknown format:"
                                    + mPictureFormat);
                        }
                        mCameraOptions = new Camera2Options(mManager, mCameraId, flip, format);
                        createRepeatingRequestBuilder(getRepeatingRequestDefaultTemplate());
                    } catch (CameraAccessException e) {
                        task.trySetException(createCameraException(e));
                        return;
                    }
                    task.trySetResult(mCameraOptions);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    // Not sure if this is called INSTEAD of onOpened() or can be called after
                    // as well. Cover both cases with an unrecoverable exception so that the
                    // engine is properly destroyed.
                    CameraException exception
                            = new CameraException(CameraException.REASON_DISCONNECTED);
                    if (!task.getTask().isComplete()) {
                        task.trySetException(exception);
                    } else {
                        LOG.i("CameraDevice.StateCallback reported disconnection.");
                        throw exception;
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    if (!task.getTask().isComplete()) {
                        task.trySetException(createCameraException(error));
                    } else {
                        // This happened while the engine is running. Throw unrecoverable exception
                        // so that engine is properly destroyed.
                        LOG.e("CameraDevice.StateCallback reported an error:", error);
                        throw new CameraException(CameraException.REASON_DISCONNECTED);
                    }
                }
            }, null);
        } catch (CameraAccessException e) {
            throw createCameraException(e);
        }
        return task.getTask();
    }

    @EngineThread
    @NonNull
    @Override
    protected Task<Void> onStartBind() {
        LOG.i("onStartBind:", "Started");
        final TaskCompletionSource<Void> task = new TaskCompletionSource<>();

        // Compute sizes.
        // TODO preview stream should never be bigger than 1920x1080 as per
        //  CameraDevice.createCaptureSession. This should probably be applied
        //  before all the other external selectors, to treat it as a hard limit.
        //  OR: pass an int into these functions to be able to take smaller dims
        //  when session configuration fails
        //  OR: both.
        mCaptureSize = computeCaptureSize();
        mPreviewStreamSize = computePreviewStreamSize();

        // Deal with surfaces.
        // In Camera2, instead of applying the size to the camera params object,
        // we must resize our own surfaces and configure them before opening the session.
        List<Surface> outputSurfaces = new ArrayList<>();

        // 1. PREVIEW
        // Create a preview surface with the correct size.
        final Class outputClass = mPreview.getOutputClass();
        final Object output = mPreview.getOutput();
        if (outputClass == SurfaceHolder.class) {
            try {
                // This must be called from the UI thread...
                LOG.i("onStartBind:", "Waiting on UI thread...");
                Tasks.await(Tasks.call(new Callable<Void>() {
                    @Override
                    public Void call() {
                        ((SurfaceHolder) output).setFixedSize(
                                mPreviewStreamSize.getWidth(),
                                mPreviewStreamSize.getHeight());
                        return null;
                    }
                }));
            } catch (ExecutionException | InterruptedException e) {
                throw new CameraException(e, CameraException.REASON_FAILED_TO_CONNECT);
            }
            mPreviewStreamSurface = ((SurfaceHolder) output).getSurface();
        } else if (outputClass == SurfaceTexture.class) {
            ((SurfaceTexture) output).setDefaultBufferSize(
                    mPreviewStreamSize.getWidth(),
                    mPreviewStreamSize.getHeight());
            mPreviewStreamSurface = new Surface((SurfaceTexture) output);
        } else {
            throw new RuntimeException("Unknown CameraPreview output class.");
        }
        outputSurfaces.add(mPreviewStreamSurface);

        // 2. VIDEO RECORDING
        if (getMode() == Mode.VIDEO) {
            if (mFullVideoPendingStub != null) {
                Full2VideoRecorder recorder = new Full2VideoRecorder(this, mCameraId);
                try {
                    outputSurfaces.add(recorder.createInputSurface(mFullVideoPendingStub));
                } catch (Full2VideoRecorder.PrepareException e) {
                    throw new CameraException(e, CameraException.REASON_FAILED_TO_CONNECT);
                }
                mVideoRecorder = recorder;
            }
        }

        // 3. PICTURE RECORDING
        // Format is supported, or it would have thrown in Camera2Options constructor.
        if (getMode() == Mode.PICTURE) {
            int format;
            switch (mPictureFormat) {
                case JPEG: format = ImageFormat.JPEG; break;
                case DNG: format = ImageFormat.RAW_SENSOR; break;
                default: throw new IllegalArgumentException("Unknown format:" + mPictureFormat);
            }
            mPictureReader = ImageReader.newInstance(
                    mCaptureSize.getWidth(),
                    mCaptureSize.getHeight(),
                    format, 2);
            outputSurfaces.add(mPictureReader.getSurface());
        }

        // 4. FRAME PROCESSING
        if (hasFrameProcessors()) {
            mFrameProcessingSize = computeFrameProcessingSize();
            // Hard to write down why, but in Camera2 we need a number of Frames that's one less
            // than the number of Images. If we let all Images be part of Frames, thus letting all
            // Images be used by processor at any given moment, the Camera2 output breaks.
            // In fact, if there are no Images available, the sensor BLOCKS until it finds one,
            // which is a big issue because processor times become a bottleneck for the preview.
            // This is a design flaw in the ImageReader / sensor implementation, as they should
            // simply DROP frames written to the surface if there are no Images available.
            // Since this is not how things work, we ensure that one Image is always available here.
            mFrameProcessingReader = ImageReader.newInstance(
                    mFrameProcessingSize.getWidth(),
                    mFrameProcessingSize.getHeight(),
                    mFrameProcessingFormat,
                    getFrameProcessingPoolSize() + 1);
            mFrameProcessingReader.setOnImageAvailableListener(this,
                    null);
            mFrameProcessingSurface = mFrameProcessingReader.getSurface();
            outputSurfaces.add(mFrameProcessingSurface);
        } else {
            mFrameProcessingReader = null;
            mFrameProcessingSize = null;
            mFrameProcessingSurface = null;
        }

        try {
            // null handler means using the current looper which is totally ok.
            mCamera.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mSession = session;
                    LOG.i("onStartBind:", "Completed");
                    task.trySetResult(null);
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    String message = LOG.e("onConfigureFailed! Session", session);
                    Throwable cause = new RuntimeException(message);
                    if (!task.getTask().isComplete()) {
                        task.trySetException(new CameraException(cause,
                                CameraException.REASON_FAILED_TO_START_PREVIEW));
                    } else {
                        // Like onStartEngine.onError
                        throw new CameraException(CameraException.REASON_DISCONNECTED);
                    }
                }

                @Override
                public void onReady(@NonNull CameraCaptureSession session) {
                    super.onReady(session);
                    LOG.i("CameraCaptureSession.StateCallback reported onReady.");
                }
            }, null);
        } catch (CameraAccessException e) {
            throw createCameraException(e);
        }
        return task.getTask();
    }

    @EngineThread
    @NonNull
    @Override
    protected Task<Void> onStartPreview() {
        LOG.i("onStartPreview:", "Dispatching onCameraPreviewStreamSizeChanged.");
        getCallback().onCameraPreviewStreamSizeChanged();

        Size previewSizeForView = getPreviewStreamSize(Reference.VIEW);
        if (previewSizeForView == null) {
            throw new IllegalStateException("previewStreamSize should not be null at this point.");
        }
        mPreview.setStreamSize(previewSizeForView.getWidth(), previewSizeForView.getHeight());
        mPreview.setDrawRotation(getAngles().offset(Reference.BASE, Reference.VIEW, Axis.ABSOLUTE));
        if (hasFrameProcessors()) {
            getFrameManager().setUp(mFrameProcessingFormat, mFrameProcessingSize, getAngles());
        }

        LOG.i("onStartPreview:", "Starting preview.");
        addRepeatingRequestBuilderSurfaces();
        applyRepeatingRequestBuilder(false,
                CameraException.REASON_FAILED_TO_START_PREVIEW);
        LOG.i("onStartPreview:", "Started preview.");

        // Start delayed video if needed.
        if (mFullVideoPendingStub != null) {
            // Do not call takeVideo/onTakeVideo. It will reset some stub parameters that
            // the recorder sets. Also we are posting so that doTakeVideo sees a started preview.
            final VideoResult.Stub stub = mFullVideoPendingStub;
            mFullVideoPendingStub = null;
            getOrchestrator().scheduleStateful("do take video", CameraState.PREVIEW,
                    new Runnable() {
                @Override
                public void run() {
                    doTakeVideo(stub);
                }
            });
        }

        // Wait for the first frame.
        final TaskCompletionSource<Void> task = new TaskCompletionSource<>();
        new BaseAction() {
            @Override
            public void onCaptureCompleted(@NonNull ActionHolder holder,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(holder, request, result);
                setState(STATE_COMPLETED);
                task.trySetResult(null);
            }
        }.start(this);
        return task.getTask();
    }

    //endregion

    //region Stop

    @EngineThread
    @NonNull
    @Override
    protected Task<Void> onStopPreview() {
        LOG.i("onStopPreview:", "Started.");
        if (mVideoRecorder != null) {
            // This should synchronously call onVideoResult that will reset the repeating builder
            // to the PREVIEW template. This is very important.
            mVideoRecorder.stop(true);
            mVideoRecorder = null;
        }
        mPictureRecorder = null;
        if (hasFrameProcessors()) {
            getFrameManager().release();
        }
        // Removing the part below for now. It hangs on emulators and can take a lot of time
        // in real devices, for benefits that I'm not 100% sure about.
        if (false) {
            try {
                // Preferring abortCaptures() over stopRepeating(): it makes sure that all
                // in-flight operations are discarded as fast as possible, which is what we want.
                // NOTE: this call is asynchronous. Should find a good way to wait for the outcome.
                LOG.i("onStopPreview:", "calling abortCaptures().");
                mSession.abortCaptures();
                LOG.i("onStopPreview:", "called abortCaptures().");
            } catch (CameraAccessException e) {
                // This tells us that we should stop everything. It's better to throw an
                // unrecoverable exception rather than just swallow, so everything gets stopped.
                LOG.w("onStopPreview:", "abortCaptures failed!", e);
                throw createCameraException(e);
            } catch (IllegalStateException e) {
                // This tells us that the session was already closed.
                // Not sure if this can happen, but we can swallow it.
            }
        }
        removeRepeatingRequestBuilderSurfaces();
        mLastRepeatingResult = null;
        LOG.i("onStopPreview:", "Returning.");
        return Tasks.forResult(null);
    }

    @EngineThread
    @NonNull
    @Override
    protected Task<Void> onStopBind() {
        LOG.i("onStopBind:", "About to clean up.");
        mFrameProcessingSurface = null;
        mPreviewStreamSurface = null;
        mPreviewStreamSize = null;
        mCaptureSize = null;
        mFrameProcessingSize = null;
        if (mFrameProcessingReader != null) {
            // WARNING: This call synchronously releases all Images and their underlying
            // properties. This can cause issues if the Image is being used.
            mFrameProcessingReader.close();
            mFrameProcessingReader = null;
        }
        if (mPictureReader != null) {
            mPictureReader.close();
            mPictureReader = null;
        }
        mSession.close();
        mSession = null;
        LOG.i("onStopBind:", "Returning.");
        return Tasks.forResult(null);
    }

    @EngineThread
    @NonNull
    @Override
    protected Task<Void> onStopEngine() {
        try {
            LOG.i("onStopEngine:", "Clean up.", "Releasing camera.");
            // Just like Camera1Engine, this call can hang (at least on emulators) and if
            // we don't find a way around the lock, it leaves the camera in a bad state.
            //
            // 12:33:28.152  2888  5470 I CameraEngine: onStopEngine: Clean up. Releasing camera.[0m
            // 12:33:29.476  1384  1555 E audio_hw_generic: pcm_write failed cannot write stream data: I/O error[0m
            // 12:33:33.206  1512  3616 E Camera3-Device: Camera 0: waitUntilDrainedLocked: Error waiting for HAL to drain: Connection timed out (-110)[0m
            // 12:33:33.242  1512  3616 E CameraDeviceClient: detachDevice: waitUntilDrained failed with code 0xffffff92[0m
            // 12:33:33.243  1512  3616 E Camera3-Device: Camera 0: disconnect: Shutting down in an error state[0m
            //
            // I believe there is a thread deadlock due to this call internally waiting to
            // dispatch some callback to us (pending captures, ...), but the callback thread
            // is blocked here. We try to workaround this in CameraEngine.destroy().
            mCamera.close();
            LOG.i("onStopEngine:", "Clean up.", "Released camera.");
        } catch (Exception e) {
            LOG.w("onStopEngine:", "Clean up.", "Exception while releasing camera.", e);
        }
        mCamera = null;

        // After engine is stopping, the repeating request builder will be null,
        // so the ActionHolder.getBuilder() contract would be broken. Same for characteristics.
        // This can cause crashes if some ongoing Action queries the holder. So we abort them.
        LOG.i("onStopEngine:", "Aborting actions.");
        for (Action action : mActions) {
            action.abort(this);
        }

        mCameraCharacteristics = null;
        mCameraOptions = null;
        mVideoRecorder = null;
        mRepeatingRequestBuilder = null;
        LOG.w("onStopEngine:", "Returning.");
        return Tasks.forResult(null);
    }

    //endregion

    //region Pictures

    @EngineThread
    @Override
    protected void onTakePictureSnapshot(@NonNull final PictureResult.Stub stub,
                                         @NonNull final AspectRatio outputRatio,
                                         boolean doMetering) {
        if (doMetering) {
            LOG.i("onTakePictureSnapshot:", "doMetering is true. Delaying.");
            Action action = Actions.timeout(METER_TIMEOUT_SHORT, createMeterAction(null));
            action.addCallback(new CompletionCallback() {
                @Override
                protected void onActionCompleted(@NonNull Action action) {
                    // This is called on any thread, so be careful.
                    setPictureSnapshotMetering(false);
                    takePictureSnapshot(stub);
                    setPictureSnapshotMetering(true);
                }
            });
            action.start(this);
        } else {
            LOG.i("onTakePictureSnapshot:", "doMetering is false. Performing.");
            if (!(mPreview instanceof RendererCameraPreview)) {
                throw new RuntimeException("takePictureSnapshot with Camera2 is only " +
                        "supported with Preview.GL_SURFACE");
            }
            // stub.size is not the real size: it will be cropped to the given ratio
            // stub.rotation will be set to 0 - we rotate the texture instead.
            stub.size = getUncroppedSnapshotSize(Reference.OUTPUT);
            stub.rotation = getAngles().offset(Reference.VIEW, Reference.OUTPUT, Axis.ABSOLUTE);
            mPictureRecorder = new Snapshot2PictureRecorder(stub, this,
                    (RendererCameraPreview) mPreview, outputRatio);
            mPictureRecorder.take();
        }
    }

    @EngineThread
    @Override
    protected void onTakePicture(@NonNull final PictureResult.Stub stub, boolean doMetering) {
        if (doMetering) {
            LOG.i("onTakePicture:", "doMetering is true. Delaying.");
            Action action = Actions.timeout(METER_TIMEOUT_SHORT, createMeterAction(null));
            action.addCallback(new CompletionCallback() {
                @Override
                protected void onActionCompleted(@NonNull Action action) {
                    // This is called on any thread, so be careful.
                    setPictureMetering(false);
                    takePicture(stub);
                    setPictureMetering(true);
                }
            });
            action.start(this);
        } else {
            LOG.i("onTakePicture:", "doMetering is false. Performing.");
            stub.rotation = getAngles().offset(Reference.SENSOR, Reference.OUTPUT,
                    Axis.RELATIVE_TO_SENSOR);
            stub.size = getPictureSize(Reference.OUTPUT);
            try {
                if (mPictureCaptureStopsPreview) {
                    // These two are present in official samples and are probably meant to
                    // speed things up? But from my tests, they actually make everything slower.
                    // So this is disabled by default with a boolean flag. Maybe in the future
                    // we can make this configurable as some people might want to stop the preview
                    // while picture is being taken even if it increases the latency.
                    mSession.stopRepeating();
                    mSession.abortCaptures();
                }
                CaptureRequest.Builder builder
                        = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                applyAllParameters(builder, mRepeatingRequestBuilder);
                mPictureRecorder = new Full2PictureRecorder(stub, this, builder,
                        mPictureReader);
                mPictureRecorder.take();
            } catch (CameraAccessException e) {
                throw createCameraException(e);
            }
        }
    }

    @Override
    public void onPictureResult(@Nullable PictureResult.Stub result, @Nullable Exception error) {
        boolean fullPicture = mPictureRecorder instanceof Full2PictureRecorder;
        super.onPictureResult(result, error);
        if (fullPicture && mPictureCaptureStopsPreview) {
            applyRepeatingRequestBuilder();
        }

        // Some picture recorders might lock metering, and we usually run a metering sequence
        // before running the recorders. So, run an unlock/reset sequence if needed.
        boolean unlock = (fullPicture && getPictureMetering())
                || (!fullPicture && getPictureSnapshotMetering());
        if (unlock) {
            getOrchestrator().scheduleStateful("reset metering after picture",
                    CameraState.PREVIEW,
                    new Runnable() {
                @Override
                public void run() {
                    unlockAndResetMetering();
                }
            });
        }
    }

    //endregion

    //region Videos

    @EngineThread
    @Override
    protected void onTakeVideo(@NonNull VideoResult.Stub stub) {
        LOG.i("onTakeVideo", "called.");
        stub.rotation = getAngles().offset(Reference.SENSOR, Reference.OUTPUT,
                Axis.RELATIVE_TO_SENSOR);
        stub.size = getAngles().flip(Reference.SENSOR, Reference.OUTPUT) ?
                mCaptureSize.flip() : mCaptureSize;
        // We must restart the session at each time.
        // Save the pending data and restart the session.
        LOG.w("onTakeVideo", "calling restartBind.");
        mFullVideoPendingStub = stub;
        restartBind();
    }

    private void doTakeVideo(@NonNull final VideoResult.Stub stub) {
        if (!(mVideoRecorder instanceof Full2VideoRecorder)) {
            throw new IllegalStateException("doTakeVideo called, but video recorder " +
                    "is not a Full2VideoRecorder! " + mVideoRecorder);
        }
        Full2VideoRecorder recorder = (Full2VideoRecorder) mVideoRecorder;
        try {
            createRepeatingRequestBuilder(CameraDevice.TEMPLATE_RECORD);
            addRepeatingRequestBuilderSurfaces(recorder.getInputSurface());
            applyRepeatingRequestBuilder(true, CameraException.REASON_DISCONNECTED);
            mVideoRecorder.start(stub);
        } catch (CameraAccessException e) {
            onVideoResult(null, e);
            throw createCameraException(e);
        } catch (CameraException e) {
            onVideoResult(null, e);
            throw e;
        }
    }

    @EngineThread
    @Override
    protected void onTakeVideoSnapshot(@NonNull VideoResult.Stub stub,
                                       @NonNull AspectRatio outputRatio) {
        if (!(mPreview instanceof RendererCameraPreview)) {
            throw new IllegalStateException("Video snapshots are only supported with GL_SURFACE.");
        }
        RendererCameraPreview glPreview = (RendererCameraPreview) mPreview;
        Size outputSize = getUncroppedSnapshotSize(Reference.OUTPUT);
        if (outputSize == null) {
            throw new IllegalStateException("outputSize should not be null.");
        }
        Rect outputCrop = CropHelper.computeCrop(outputSize, outputRatio);
        outputSize = new Size(outputCrop.width(), outputCrop.height());
        stub.size = outputSize;
        stub.rotation = getAngles().offset(Reference.VIEW, Reference.OUTPUT, Axis.ABSOLUTE);
        stub.videoFrameRate = Math.round(mPreviewFrameRate);
        LOG.i("onTakeVideoSnapshot", "rotation:", stub.rotation, "size:", stub.size);
        mVideoRecorder = new SnapshotVideoRecorder(this, glPreview, getOverlay());
        mVideoRecorder.start(stub);
    }

    /**
     * When video ends we must stop the recorder and remove the recorder surface from
     * camera outputs. This is done in onVideoResult. However, on some devices, order matters.
     * If we stop the recorder and AFTER send camera frames to it, the camera will try to fill
     * the recorder "abandoned" Surface and on some devices with a poor internal implementation
     * (HW_LEVEL_LEGACY) this crashes. So if the conditions are met, we restore here. Issue #549.
     */
    @Override
    public void onVideoRecordingEnd() {
        super.onVideoRecordingEnd();
        // SnapshotRecorder will invoke this on its own thread which is risky, but if it was a
        // snapshot, this function does nothing so it's safe.
        boolean needsIssue549Workaround = (mVideoRecorder instanceof Full2VideoRecorder) &&
                (readCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1)
                        == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        if (needsIssue549Workaround) {
            LOG.w("Applying the Issue549 workaround.", Thread.currentThread());
            maybeRestorePreviewTemplateAfterVideo();
            LOG.w("Applied the Issue549 workaround. Sleeping...");
            try { Thread.sleep(600); } catch (InterruptedException ignore) {}
            LOG.w("Applied the Issue549 workaround. Slept!");
        }
    }

    @Override
    public void onVideoResult(@Nullable VideoResult.Stub result, @Nullable Exception exception) {
        super.onVideoResult(result, exception);
        // SnapshotRecorder will invoke this on its own thread, so let's post in our own thread
        // and check camera state before trying to restore the preview. Engine might have been
        // torn down in the engine thread while this was still being called.
        getOrchestrator().scheduleStateful("restore preview template", CameraState.BIND,
                new Runnable() {
            @Override
            public void run() {
                maybeRestorePreviewTemplateAfterVideo();
            }
        });
    }

    /**
     * Video recorders might change the camera template to {@link CameraDevice#TEMPLATE_RECORD}.
     * After the video is taken, we should restore the template preview, which also means that
     * we'll remove any extra surface target that was added by the video recorder.
     *
     * This method avoids doing this twice by checking the request tag, as set by
     * the {@link #createRepeatingRequestBuilder(int)} method.
     */
    @EngineThread
    private void maybeRestorePreviewTemplateAfterVideo() {
        int template = (int) mRepeatingRequestBuilder.build().getTag();
        if (template != getRepeatingRequestDefaultTemplate()) {
            try {
                createRepeatingRequestBuilder(getRepeatingRequestDefaultTemplate());
                addRepeatingRequestBuilderSurfaces();
                applyRepeatingRequestBuilder();
            } catch (CameraAccessException e) {
                throw createCameraException(e);
            }
        }
    }

    //endregion

    //region Parameters

    private void applyAllParameters(@NonNull CaptureRequest.Builder builder,
                                    @Nullable CaptureRequest.Builder oldBuilder) {
        LOG.i("applyAllParameters:", "called for tag", builder.build().getTag());
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        applyDefaultFocus(builder);
        applyFlash(builder, Flash.OFF);
        applyLocation(builder, null);
        applyWhiteBalance(builder, WhiteBalance.AUTO);
        applyHdr(builder, Hdr.OFF);
        applyZoom(builder, 0F);
        applyExposureCorrection(builder, 0F);
        applyPreviewFrameRate(builder, 0F);

        if (oldBuilder != null) {
            // We might be in a metering operation, or the old builder might have some special
            // metering parameters. Copy these special keys over to the new builder.
            // These are the keys changed by metering.Parameters, or by us in applyFocusForMetering.
            builder.set(CaptureRequest.CONTROL_AF_REGIONS,
                    oldBuilder.get(CaptureRequest.CONTROL_AF_REGIONS));
            builder.set(CaptureRequest.CONTROL_AE_REGIONS,
                    oldBuilder.get(CaptureRequest.CONTROL_AE_REGIONS));
            builder.set(CaptureRequest.CONTROL_AWB_REGIONS,
                    oldBuilder.get(CaptureRequest.CONTROL_AWB_REGIONS));
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    oldBuilder.get(CaptureRequest.CONTROL_AF_MODE));
            // Do NOT copy exposure or focus triggers!
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected void applyDefaultFocus(@NonNull CaptureRequest.Builder builder) {
        int[] modesArray = readCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES,
                new int[]{});
        List<Integer> modes = new ArrayList<>();
        for (int mode : modesArray) { modes.add(mode); }
        if (getMode() == Mode.VIDEO &&
                modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)) {
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            return;
        }

        if (modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            return;
        }

        if (modes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO)) {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            return;
        }

        if (modes.contains(CaptureRequest.CONTROL_AF_MODE_OFF)) {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0F);
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    /**
     * All focus modes support the AF trigger, except OFF and EDOF.
     * However, unlike the preview, we'd prefer AUTO to any CONTINUOUS value.
     * An AUTO value means that focus is locked unless we run the focus trigger,
     * which is what metering does.
     *
     * @param builder builder
     */
    @SuppressWarnings("WeakerAccess")
    protected void applyFocusForMetering(@NonNull CaptureRequest.Builder builder) {
        int[] modesArray = readCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES,
                new int[]{});
        List<Integer> modes = new ArrayList<>();
        for (int mode : modesArray) { modes.add(mode); }
        if (modes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO)) {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            return;
        }
        if (getMode() == Mode.VIDEO &&
                modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)) {
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            return;
        }

        if (modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    @Override
    public void setFlash(@NonNull final Flash flash) {
        final Flash old = mFlash;
        mFlash = flash;
        mFlashTask = getOrchestrator().scheduleStateful("flash (" + flash + ")",
                CameraState.ENGINE,
                new Runnable() {
            @Override
            public void run() {
                boolean shouldApply = applyFlash(mRepeatingRequestBuilder, old);
                boolean needsWorkaround = getState() == CameraState.PREVIEW;
                if (needsWorkaround) {
                    // Runtime changes to the flash value are not correctly handled by the
                    // driver. See https://stackoverflow.com/q/53003383/4288782 for example.
                    // For this reason, we go back to OFF, capture once, then go to the new one.
                    mFlash = Flash.OFF;
                    applyFlash(mRepeatingRequestBuilder, old);
                    try {
                        mSession.capture(mRepeatingRequestBuilder.build(), null,
                                null);
                    } catch (CameraAccessException e) {
                        throw createCameraException(e);
                    }
                    mFlash = flash;
                    applyFlash(mRepeatingRequestBuilder, old);
                    applyRepeatingRequestBuilder();

                } else if (shouldApply) {
                    applyRepeatingRequestBuilder();
                }
            }
        });
    }

    /**
     * This sets the CONTROL_AE_MODE to either:
     * - {@link CaptureRequest#CONTROL_AE_MODE_ON}
     * - {@link CaptureRequest#CONTROL_AE_MODE_ON_AUTO_FLASH}
     * - {@link CaptureRequest#CONTROL_AE_MODE_ON_ALWAYS_FLASH}
     *
     * The API offers a high level control through {@link CaptureRequest#CONTROL_AE_MODE},
     * which is what the mapper looks at. It will trigger (if specified) flash only for
     * still captures which is exactly what we want.
     *
     * However, we set CONTROL_AE_MODE to ON/OFF (depending
     * on which is available) with both {@link Flash#OFF} and {@link Flash#TORCH}.
     *
     * When CONTROL_AE_MODE is ON or OFF, the low level control, called
     * {@link CaptureRequest#FLASH_MODE}, becomes effective, and that's where we can actually
     * distinguish between a turned off flash and a torch flash.
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean applyFlash(@NonNull CaptureRequest.Builder builder,
                                 @NonNull Flash oldFlash) {
        if (mCameraOptions.supports(mFlash)) {
            int[] availableAeModesArray = readCharacteristic(
                    CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, new int[]{});
            List<Integer> availableAeModes = new ArrayList<>();
            for (int mode : availableAeModesArray) { availableAeModes.add(mode); }

            List<Pair<Integer, Integer>> pairs = mMapper.mapFlash(mFlash);
            for (Pair<Integer, Integer> pair : pairs) {
                if (availableAeModes.contains(pair.first)) {
                    LOG.i("applyFlash: setting CONTROL_AE_MODE to", pair.first);
                    LOG.i("applyFlash: setting FLASH_MODE to", pair.second);
                    builder.set(CaptureRequest.CONTROL_AE_MODE, pair.first);
                    builder.set(CaptureRequest.FLASH_MODE, pair.second);
                    return true;
                }
            }
        }
        mFlash = oldFlash;
        return false;
    }

    @Override
    public void setLocation(@Nullable Location location) {
        final Location old = mLocation;
        mLocation = location;
        mLocationTask = getOrchestrator().scheduleStateful("location",
                CameraState.ENGINE,
                new Runnable() {
            @Override
            public void run() {
                if (applyLocation(mRepeatingRequestBuilder, old)) {
                    applyRepeatingRequestBuilder();
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean applyLocation(@NonNull CaptureRequest.Builder builder,
                                    @SuppressWarnings("unused") @Nullable Location oldLocation) {
        if (mLocation != null) {
            builder.set(CaptureRequest.JPEG_GPS_LOCATION, mLocation);
        }
        return true;
    }

    @Override
    public void setWhiteBalance(@NonNull WhiteBalance whiteBalance) {
        final WhiteBalance old = mWhiteBalance;
        mWhiteBalance = whiteBalance;
        mWhiteBalanceTask = getOrchestrator().scheduleStateful(
                "white balance (" + whiteBalance + ")",
                CameraState.ENGINE,
                new Runnable() {
            @Override
            public void run() {
                if (applyWhiteBalance(mRepeatingRequestBuilder, old)) {
                    applyRepeatingRequestBuilder();
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean applyWhiteBalance(@NonNull CaptureRequest.Builder builder,
                                        @NonNull WhiteBalance oldWhiteBalance) {
        if (mCameraOptions.supports(mWhiteBalance)) {
            int whiteBalance = mMapper.mapWhiteBalance(mWhiteBalance);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, whiteBalance);
            return true;
        }
        mWhiteBalance = oldWhiteBalance;
        return false;
    }

    @Override
    public void setHdr(@NonNull Hdr hdr) {
        final Hdr old = mHdr;
        mHdr = hdr;
        mHdrTask = getOrchestrator().scheduleStateful("hdr (" + hdr + ")",
                CameraState.ENGINE,
                new Runnable() {
            @Override
            public void run() {
                if (applyHdr(mRepeatingRequestBuilder, old)) {
                    applyRepeatingRequestBuilder();
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean applyHdr(@NonNull CaptureRequest.Builder builder, @NonNull Hdr oldHdr) {
        if (mCameraOptions.supports(mHdr)) {
            int hdr = mMapper.mapHdr(mHdr);
            builder.set(CaptureRequest.CONTROL_SCENE_MODE, hdr);
            return true;
        }
        mHdr = oldHdr;
        return false;
    }

    @Override
    public void setZoom(final float zoom, final @Nullable PointF[] points, final boolean notify) {
        final float old = mZoomValue;
        mZoomValue = zoom;
        // Zoom requests can be high frequency (e.g. linked to touch events), let's trim the oldest.
        getOrchestrator().trim("zoom", ALLOWED_ZOOM_OPS);
        mZoomTask = getOrchestrator().scheduleStateful(
                "zoom",
                CameraState.ENGINE,
                new Runnable() {
            @Override
            public void run() {
                if (applyZoom(mRepeatingRequestBuilder, old)) {
                    applyRepeatingRequestBuilder();
                    if (notify) {
                        getCallback().dispatchOnZoomChanged(zoom, points);
                    }
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean applyZoom(@NonNull CaptureRequest.Builder builder, float oldZoom) {
        if (mCameraOptions.isZoomSupported()) {
            float maxZoom = readCharacteristic(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, 1F);
            // converting 0.0f-1.0f zoom scale to the actual camera digital zoom scale
            // (which will be for example, 1.0-10.0)
            float calculatedZoom = (mZoomValue * (maxZoom - 1.0f)) + 1.0f;
            Rect newRect = getZoomRect(calculatedZoom, maxZoom);
            builder.set(CaptureRequest.SCALER_CROP_REGION, newRect);
            return true;
        }
        mZoomValue = oldZoom;
        return false;
    }

    @NonNull
    private Rect getZoomRect(float zoomLevel, float maxDigitalZoom) {
        Rect activeRect = readCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect());
        int minW = (int) (activeRect.width() / maxDigitalZoom);
        int minH = (int) (activeRect.height() / maxDigitalZoom);
        int difW = activeRect.width() - minW;
        int difH = activeRect.height() - minH;

        // When zoom is 1, we want to return new Rect(0, 0, width, height).
        // When zoom is maxZoom, we want to return a centered rect with minW and minH
        int cropW = (int) (difW * (zoomLevel - 1) / (maxDigitalZoom - 1) / 2F);
        int cropH = (int) (difH * (zoomLevel - 1) / (maxDigitalZoom - 1) / 2F);
        return new Rect(cropW, cropH, activeRect.width() - cropW,
                activeRect.height() - cropH);
    }

    @Override
    public void setExposureCorrection(final float EVvalue,
                                      @NonNull final float[] bounds,
                                      @Nullable final PointF[] points,
                                      final boolean notify) {
        final float old = mExposureCorrectionValue;
        mExposureCorrectionValue = EVvalue;
        // EV requests can be high frequency (e.g. linked to touch events), let's trim the oldest.
        getOrchestrator().trim("exposure correction", ALLOWED_EV_OPS);
        mExposureCorrectionTask = getOrchestrator().scheduleStateful(
                "exposure correction",
                CameraState.ENGINE,
                new Runnable() {
            @Override
            public void run() {
                if (applyExposureCorrection(mRepeatingRequestBuilder, old)) {
                    applyRepeatingRequestBuilder();
                    if (notify) {
                        getCallback().dispatchOnExposureCorrectionChanged(EVvalue, bounds, points);
                    }
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean applyExposureCorrection(@NonNull CaptureRequest.Builder builder,
                                              float oldEVvalue) {
        if (mCameraOptions.isExposureCorrectionSupported()) {
            Rational exposureCorrectionStep = readCharacteristic(
                    CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP,
                    new Rational(1, 1));
            int exposureCorrectionSteps = Math.round(mExposureCorrectionValue
                    * exposureCorrectionStep.floatValue());
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureCorrectionSteps);
            return true;
        }
        mExposureCorrectionValue = oldEVvalue;
        return false;
    }

    @Override
    public void setPlaySounds(boolean playSounds) {
        mPlaySounds = playSounds;
        mPlaySoundsTask = Tasks.forResult(null);
    }

    @Override
    public void setPreviewFrameRate(float previewFrameRate) {
        final float oldPreviewFrameRate = mPreviewFrameRate;
        mPreviewFrameRate = previewFrameRate;
        mPreviewFrameRateTask = getOrchestrator().scheduleStateful(
                "preview fps (" + previewFrameRate + ")",
                CameraState.ENGINE,
                new Runnable() {
            @Override
            public void run() {
                if (applyPreviewFrameRate(mRepeatingRequestBuilder, oldPreviewFrameRate)) {
                    applyRepeatingRequestBuilder();
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean applyPreviewFrameRate(@NonNull CaptureRequest.Builder builder,
                                            float oldPreviewFrameRate) {
        //noinspection unchecked
        Range<Integer>[] fpsRanges = readCharacteristic(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES,
                new Range[]{});
        sortFrameRateRanges(fpsRanges);
        if (mPreviewFrameRate == 0F) {
            // 0F is a special value. Fallback to a reasonable default.
            for (Range<Integer> fpsRange : filterFrameRateRanges(fpsRanges)) {
                if (fpsRange.contains(30) || fpsRange.contains(24)) {
                    builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                    return true;
                }
            }
        } else {
            // If out of boundaries, adjust it.
            mPreviewFrameRate = Math.min(mPreviewFrameRate,
                    mCameraOptions.getPreviewFrameRateMaxValue());
            mPreviewFrameRate = Math.max(mPreviewFrameRate,
                    mCameraOptions.getPreviewFrameRateMinValue());
            for (Range<Integer> fpsRange : filterFrameRateRanges(fpsRanges)) {
                if (fpsRange.contains(Math.round(mPreviewFrameRate))) {
                    builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                    return true;
                }
            }
        }
        mPreviewFrameRate = oldPreviewFrameRate;
        return false;
    }

    private void sortFrameRateRanges(@NonNull Range<Integer>[] fpsRanges) {
        final boolean ascending = getPreviewFrameRateExact() && mPreviewFrameRate != 0;
        Arrays.sort(fpsRanges, new Comparator<Range<Integer>>() {
            @Override
            public int compare(Range<Integer> range1, Range<Integer> range2) {
                if (ascending) {
                    return (range1.getUpper() - range1.getLower())
                            - (range2.getUpper() - range2.getLower());
                } else {
                    return (range2.getUpper() - range2.getLower())
                            - (range1.getUpper() - range1.getLower());
                }
            }
        });
    }

    @NonNull
    protected List<Range<Integer>> filterFrameRateRanges(@NonNull Range<Integer>[] fpsRanges) {
        List<Range<Integer>> results = new ArrayList<>();
        int min = Math.round(mCameraOptions.getPreviewFrameRateMinValue());
        int max = Math.round(mCameraOptions.getPreviewFrameRateMaxValue());
        for (Range<Integer> fpsRange : fpsRanges) {
            if (!fpsRange.contains(min) && !fpsRange.contains(max)) continue;
            if (!FpsRangeValidator.validate(fpsRange)) continue;
            results.add(fpsRange);
        }
        return results;
    }

    @Override
    public void setPictureFormat(final @NonNull PictureFormat pictureFormat) {
        if (pictureFormat != mPictureFormat) {
            mPictureFormat = pictureFormat;
            getOrchestrator().scheduleStateful("picture format (" + pictureFormat + ")",
                    CameraState.ENGINE,
                    new Runnable() {
                @Override
                public void run() {
                    restart();
                }
            });
        }
    }

    //endregion

    //region Frame Processing

    @NonNull
    @Override
    protected FrameManager instantiateFrameManager(int poolSize) {
        return new ImageFrameManager(poolSize);
    }

    @EngineThread
    @Override
    public void onImageAvailable(ImageReader reader) {
        LOG.v("onImageAvailable:", "trying to acquire Image.");
        Image image = null;
        try {
            image = reader.acquireLatestImage();
        } catch (Exception ignore) { }
        if (image == null) {
            LOG.w("onImageAvailable:", "failed to acquire Image!");
        } else if (getState() == CameraState.PREVIEW && !isChangingState()) {
            // After preview, the frame manager is correctly set up
            //noinspection unchecked
            Frame frame = getFrameManager().getFrame(image,
                    System.currentTimeMillis());
            if (frame != null) {
                LOG.v("onImageAvailable:", "Image acquired, dispatching.");
                getCallback().dispatchFrame(frame);
            } else {
                LOG.i("onImageAvailable:", "Image acquired, but no free frames. DROPPING.");
            }
        } else {
            LOG.i("onImageAvailable:", "Image acquired in wrong state. Closing it now.");
            image.close();
        }
    }

    @Override
    public void setHasFrameProcessors(final boolean hasFrameProcessors) {
        // Frame processing is set up partially when binding and partially when starting
        // the preview. If the value is changed between the two, the preview step can crash.
        getOrchestrator().schedule("has frame processors (" + hasFrameProcessors + ")",
                true, new Runnable() {
            @Override
            public void run() {
                if (getState().isAtLeast(CameraState.BIND) && isChangingState()) {
                    // Extremely rare case in which this was called in between startBind and
                    // startPreview. This can cause issues. Try later.
                    setHasFrameProcessors(hasFrameProcessors);
                    return;
                }
                // Apply and restart.
                mHasFrameProcessors = hasFrameProcessors;
                if (getState().isAtLeast(CameraState.BIND)) {
                    restartBind();
                }
            }
        });
    }

    @Override
    public void setFrameProcessingFormat(final int format) {
        // This is called during initialization. Set our default first.
        if (mFrameProcessingFormat == 0) mFrameProcessingFormat = FRAME_PROCESSING_FORMAT;
        // Frame processing format is used both when binding and when starting the preview.
        // If the value is changed between the two, the preview step can crash.
        getOrchestrator().schedule("frame processing format (" + format + ")",
                true, new Runnable() {
            @Override
            public void run() {
                if (getState().isAtLeast(CameraState.BIND) && isChangingState()) {
                    // Extremely rare case in which this was called in between startBind and
                    // startPreview. This can cause issues. Try later.
                    setFrameProcessingFormat(format);
                    return;
                }
                mFrameProcessingFormat = format > 0 ? format : FRAME_PROCESSING_FORMAT;
                if (getState().isAtLeast(CameraState.BIND)) {
                    restartBind();
                }
            }
        });
    }

    //endregion

    //region 3A Metering

    @Override
    public void startAutoFocus(@Nullable final Gesture gesture,
                               @NonNull final MeteringRegions regions,
                               @NonNull final PointF legacyPoint) {
        // This will only work when we have a preview, since it launches the preview
        // in the end. Even without this it would need the bind state at least,
        // since we need the preview size.
        getOrchestrator().scheduleStateful("autofocus (" + gesture + ")",
                CameraState.PREVIEW,
                new Runnable() {
            @Override
            public void run() {
                // The camera options API still has the auto focus API but it really
                // refers to "3A metering to a specific point". Since we have a point, check.
                if (!mCameraOptions.isAutoFocusSupported()) return;

                // Create the meter and start.
                getCallback().dispatchOnFocusStart(gesture, legacyPoint);
                final MeterAction action = createMeterAction(regions);
                Action wrapper = Actions.timeout(METER_TIMEOUT, action);
                wrapper.start(Camera2Engine.this);
                wrapper.addCallback(new CompletionCallback() {
                    @Override
                    protected void onActionCompleted(@NonNull Action a) {
                        getCallback().dispatchOnFocusEnd(gesture,
                                action.isSuccessful(), legacyPoint);
                        getOrchestrator().remove("reset metering");
                        if (shouldResetAutoFocus()) {
                            getOrchestrator().scheduleStatefulDelayed("reset metering",
                                    CameraState.PREVIEW,
                                    getAutoFocusResetDelay(),
                                    new Runnable() {
                                @Override
                                public void run() {
                                    unlockAndResetMetering();
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @NonNull
    private MeterAction createMeterAction(@Nullable MeteringRegions regions) {
        // Before creating any new meter action, abort the old one.
        if (mMeterAction != null) mMeterAction.abort(this);
        // The meter will check the current configuration to see if AF/AE/AWB should run.
        // - AE should be on CONTROL_AE_MODE_ON*    (this depends on setFlash())
        // - AWB should be on CONTROL_AWB_MODE_AUTO (this depends on setWhiteBalance())
        // - AF should be on CONTROL_AF_MODE_AUTO or others
        // The last one is under our control because the library has no focus API.
        // So let's set a good af mode here. This operation is reverted during onMeteringReset().
        applyFocusForMetering(mRepeatingRequestBuilder);
        mMeterAction = new MeterAction(Camera2Engine.this, regions, regions == null);
        return mMeterAction;
    }

    @EngineThread
    private void unlockAndResetMetering() {
        // Needs the PREVIEW state!
        Actions.sequence(
                new BaseAction() {
                    @Override
                    protected void onStart(@NonNull ActionHolder holder) {
                        super.onStart(holder);
                        applyDefaultFocus(holder.getBuilder(this));
                        holder.getBuilder(this)
                                .set(CaptureRequest.CONTROL_AE_LOCK, false);
                        holder.getBuilder(this)
                                .set(CaptureRequest.CONTROL_AWB_LOCK, false);
                        holder.applyBuilder(this);
                        setState(STATE_COMPLETED);
                        // TODO should wait results?
                    }
                },
                new MeterResetAction()
        ).start(Camera2Engine.this);
    }

    //endregion

    //region Actions

    @Override
    public void addAction(final @NonNull Action action) {
        if (!mActions.contains(action)) {
            mActions.add(action);
        }
    }

    @Override
    public void removeAction(final @NonNull Action action) {
        mActions.remove(action);
    }

    @NonNull
    @Override
    public CameraCharacteristics getCharacteristics(@NonNull Action action) {
        return mCameraCharacteristics;
    }

    @Nullable
    @Override
    public TotalCaptureResult getLastResult(@NonNull Action action) {
        return mLastRepeatingResult;
    }

    @NonNull
    @Override
    public CaptureRequest.Builder getBuilder(@NonNull Action action) {
        return mRepeatingRequestBuilder;
    }

    @EngineThread
    @Override
    public void applyBuilder(@NonNull Action source) {
        // NOTE: Should never be called on a non-engine thread!
        // Non-engine threads are not protected by the uncaught exception handler
        // and can make the process crash.
        applyRepeatingRequestBuilder();
    }

    @Override
    public void applyBuilder(@NonNull Action source, @NonNull CaptureRequest.Builder builder)
            throws CameraAccessException {
        // Risky - would be better to ensure that thread is the engine one.
        if (getState() == CameraState.PREVIEW && !isChangingState()) {
            mSession.capture(builder.build(), mRepeatingRequestCallback, null);
        }
    }

    //endregion
}
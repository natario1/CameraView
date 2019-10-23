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
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.engine.action.Action;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.Actions;
import com.otaliastudios.cameraview.engine.action.BaseAction;
import com.otaliastudios.cameraview.engine.action.CompletionCallback;
import com.otaliastudios.cameraview.engine.mappers.Camera2Mapper;
import com.otaliastudios.cameraview.engine.meter.MeterAction;
import com.otaliastudios.cameraview.engine.meter.MeterResetAction;
import com.otaliastudios.cameraview.engine.offset.Axis;
import com.otaliastudios.cameraview.engine.offset.Reference;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameManager;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.internal.utils.CropHelper;
import com.otaliastudios.cameraview.internal.utils.ImageHelper;
import com.otaliastudios.cameraview.internal.utils.WorkerHandler;
import com.otaliastudios.cameraview.picture.Full2PictureRecorder;
import com.otaliastudios.cameraview.picture.Snapshot2PictureRecorder;
import com.otaliastudios.cameraview.preview.GlCameraPreview;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.size.SizeSelectors;
import com.otaliastudios.cameraview.video.Full2VideoRecorder;
import com.otaliastudios.cameraview.video.SnapshotVideoRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Engine extends CameraEngine implements ImageReader.OnImageAvailableListener,
        ActionHolder {

    private static final String TAG = Camera2Engine.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private static final int FRAME_PROCESSING_FORMAT = ImageFormat.NV21;
    private static final int FRAME_PROCESSING_INPUT_FORMAT = ImageFormat.YUV_420_888;
    @VisibleForTesting static final long METER_TIMEOUT = 2500;

    private final CameraManager mManager;
    private String mCameraId;
    private CameraDevice mCamera;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mRepeatingRequestBuilder;
    private TotalCaptureResult mLastRepeatingResult;
    private final Camera2Mapper mMapper = Camera2Mapper.get();

    // Frame processing
    private Size mFrameProcessingSize;
    private ImageReader mFrameProcessingReader; // need this or the reader surface is collected
    private final WorkerHandler mFrameConversionHandler;
    private final Object mFrameProcessingImageLock = new Object();
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
        mManager = (CameraManager) mCallback.getContext().getSystemService(Context.CAMERA_SERVICE);
        mFrameConversionHandler = WorkerHandler.get("CameraFrameConversion");
        new LogAction().start(this);
    }

    //region Utilities

    @NonNull
    private <T> T readCharacteristic(@NonNull CameraCharacteristics.Key<T> key,
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
     * Applies the repeating request builder to the preview, assuming we actually have a preview
     * running. Can be called after changing parameters to the builder.
     *
     * To apply a new builder (for example switch between TEMPLATE_PREVIEW and TEMPLATE_RECORD)
     * it should be set before calling this method, for example by calling
     * {@link #createRepeatingRequestBuilder(int)}.
     */
    @SuppressWarnings("WeakerAccess")
    protected void applyRepeatingRequestBuilder() {
        applyRepeatingRequestBuilder(true, CameraException.REASON_DISCONNECTED);
    }

    private void applyRepeatingRequestBuilder(boolean checkStarted, int errorReason) {
        if (getPreviewState() == STATE_STARTED || !checkStarted) {
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
                        "previewState:", getPreviewState(),
                        "bindState:", getBindState(),
                        "engineState:", getEngineState());
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
    @Override
    protected void onPreviewStreamSizeChanged() {
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
    protected Task<Void> onStartEngine() {
        final TaskCompletionSource<Void> task = new TaskCompletionSource<>();
        try {
            // We have a valid camera for this Facing. Go on.
            mManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCamera = camera;

                    // Set parameters that might have been set before the camera was opened.
                    try {
                        LOG.i("createCamera:", "Applying default parameters.");
                        mCameraCharacteristics = mManager.getCameraCharacteristics(mCameraId);
                        boolean flip = getAngles().flip(Reference.SENSOR, Reference.VIEW);
                        mCameraOptions = new CameraOptions(mManager, mCameraId, flip);
                        createRepeatingRequestBuilder(CameraDevice.TEMPLATE_PREVIEW);
                    } catch (CameraAccessException e) {
                        task.trySetException(createCameraException(e));
                        return;
                    }
                    task.trySetResult(null);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    // Not sure if this is called INSTEAD of onOpened() or can be called after
                    // as well. However, using trySetException should address this problem -
                    // it will only trigger if the task has no result.
                    //
                    // Docs say to release this camera instance, however, since we throw an
                    // unrecoverable CameraException, this will trigger a stop() through the
                    // exception handler.
                    task.trySetException(new CameraException(CameraException.REASON_DISCONNECTED));
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    task.trySetException(createCameraException(error));
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
        mCaptureSize = computeCaptureSize();
        mPreviewStreamSize = computePreviewStreamSize();

        // Deal with surfaces.
        // In Camera2, instead of applying the size to the camera params object,
        // we must resize our own surfaces and configure them before opening the session.
        List<Surface> outputSurfaces = new ArrayList<>();

        // 1. PREVIEW
        // Create a preview surface with the correct size.
        final Object output = mPreview.getOutput();
        if (output instanceof SurfaceHolder) {
            try {
                // This must be called from the UI thread...
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
        } else if (output instanceof SurfaceTexture) {
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
        if (getMode() == Mode.PICTURE) {
            mPictureReader = ImageReader.newInstance(
                    mCaptureSize.getWidth(),
                    mCaptureSize.getHeight(),
                    ImageFormat.JPEG,
                    2
            );
            outputSurfaces.add(mPictureReader.getSurface());
        }

        // 4. FRAME PROCESSING
        if (hasFrameProcessors()) {
            // Choose the size.
            StreamConfigurationMap streamMap = mCameraCharacteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (streamMap == null) {
                throw new RuntimeException("StreamConfigurationMap is null. Should not happen.");
            }
            android.util.Size[] aSizes = streamMap.getOutputSizes(FRAME_PROCESSING_INPUT_FORMAT);
            List<Size> sizes = new ArrayList<>();
            for (android.util.Size aSize : aSizes) {
                sizes.add(new Size(aSize.getWidth(), aSize.getHeight()));
            }
            mFrameProcessingSize = SizeSelectors.and(
                    SizeSelectors.maxWidth(Math.min(700, mPreviewStreamSize.getWidth())),
                    SizeSelectors.maxHeight(Math.min(700, mPreviewStreamSize.getHeight())),
                    SizeSelectors.biggest()).select(sizes).get(0);
            mFrameProcessingReader = ImageReader.newInstance(
                    mFrameProcessingSize.getWidth(),
                    mFrameProcessingSize.getHeight(),
                    FRAME_PROCESSING_INPUT_FORMAT,
                    2);
            mFrameProcessingReader.setOnImageAvailableListener(this,
                    mFrameConversionHandler.getHandler());
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
                    // This SHOULD be a library error so we throw a RuntimeException.
                    String message = LOG.e("onConfigureFailed! Session", session);
                    throw new RuntimeException(message);
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
        LOG.i("onStartPreview", "Dispatching onCameraPreviewStreamSizeChanged.");
        mCallback.onCameraPreviewStreamSizeChanged();

        Size previewSizeForView = getPreviewStreamSize(Reference.VIEW);
        if (previewSizeForView == null) {
            throw new IllegalStateException("previewStreamSize should not be null at this point.");
        }
        mPreview.setStreamSize(previewSizeForView.getWidth(), previewSizeForView.getHeight());
        mPreview.setDrawRotation(getAngles().offset(Reference.BASE, Reference.VIEW, Axis.ABSOLUTE));
        if (hasFrameProcessors()) {
            getFrameManager().setUp(FRAME_PROCESSING_FORMAT, mFrameProcessingSize);
        }

        LOG.i("onStartPreview", "Starting preview.");
        addRepeatingRequestBuilderSurfaces();
        applyRepeatingRequestBuilder(false,
                CameraException.REASON_FAILED_TO_START_PREVIEW);
        LOG.i("onStartPreview", "Started preview.");

        // Start delayed video if needed.
        if (mFullVideoPendingStub != null) {
            // Do not call takeVideo/onTakeVideo. It will reset some stub parameters that
            // the recorder sets. Also we are posting so that doTakeVideo sees a started preview.
            LOG.i("onStartPreview", "Posting doTakeVideo call.");
            final VideoResult.Stub stub = mFullVideoPendingStub;
            mFullVideoPendingStub = null;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    LOG.i("onStartPreview", "Executing doTakeVideo call.");
                    doTakeVideo(stub);
                }
            });
        }
        return Tasks.forResult(null);
    }

    //endregion

    //region Stop

    @EngineThread
    @NonNull
    @Override
    protected Task<Void> onStopPreview() {
        LOG.i("onStopPreview:", "About to clean up.");
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
        try {
            // NOTE: should we wait for onReady() like docs say?
            // Leaving this synchronous for now.
            mSession.stopRepeating();
        } catch (CameraAccessException e) {
            // This tells us that we should stop everything. It's better to throw an unrecoverable
            // exception rather than just swallow this, so everything gets stopped.
            LOG.w("stopRepeating failed!", e);
            throw createCameraException(e);
        }
        removeRepeatingRequestBuilderSurfaces();
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
            synchronized (mFrameProcessingImageLock) {
                // This call synchronously releases all Images and their underlying properties.
                // This can cause a segmentation fault while converting the Image to NV21.
                // So we use this lock for the two operations.
                mFrameProcessingReader.close();
            }
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
            Action action = Actions.timeout(METER_TIMEOUT, createMeterAction(null));
            action.addCallback(new CompletionCallback() {
                @Override
                protected void onActionCompleted(@NonNull Action action) {
                    onTakePictureSnapshot(stub, outputRatio, false);
                }
            });
            action.start(this);
        } else {
            LOG.i("onTakePictureSnapshot:", "doMetering is false. Performing.");
            if (!(mPreview instanceof GlCameraPreview)) {
                throw new RuntimeException("takePictureSnapshot with Camera2 is only " +
                        "supported with Preview.GL_SURFACE");
            }
            // stub.size is not the real size: it will be cropped to the given ratio
            // stub.rotation will be set to 0 - we rotate the texture instead.
            stub.size = getUncroppedSnapshotSize(Reference.OUTPUT);
            stub.rotation = getAngles().offset(Reference.SENSOR, Reference.OUTPUT,
                    Axis.RELATIVE_TO_SENSOR);
            mPictureRecorder = new Snapshot2PictureRecorder(stub, this,
                    (GlCameraPreview) mPreview, outputRatio);
            mPictureRecorder.take();
        }
    }

    @EngineThread
    @Override
    protected void onTakePicture(@NonNull final PictureResult.Stub stub, boolean doMetering) {
        if (doMetering) {
            LOG.i("onTakePicture:", "doMetering is true. Delaying.");
            Action action = Actions.timeout(METER_TIMEOUT, createMeterAction(null));
            action.addCallback(new CompletionCallback() {
                @Override
                protected void onActionCompleted(@NonNull Action action) {
                    onTakePicture(stub, false);
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
            unlockAndResetMetering();
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
        if (!(mPreview instanceof GlCameraPreview)) {
            throw new IllegalStateException("Video snapshots are only supported with GL_SURFACE.");
        }
        GlCameraPreview glPreview = (GlCameraPreview) mPreview;
        Size outputSize = getUncroppedSnapshotSize(Reference.OUTPUT);
        if (outputSize == null) {
            throw new IllegalStateException("outputSize should not be null.");
        }
        Rect outputCrop = CropHelper.computeCrop(outputSize, outputRatio);
        outputSize = new Size(outputCrop.width(), outputCrop.height());
        stub.size = outputSize;
        // Vertical:               0   (270-0-0)
        // Left (unlocked):        270 (270-90-270)
        // Right (unlocked):       90  (270-270-90)
        // Upside down (unlocked): 180 (270-180-180)
        // Left (locked):          270 (270-0-270)
        // Right (locked):         90  (270-0-90)
        // Upside down (locked):   180 (270-0-180)
        // Unlike Camera1, the correct formula seems to be deviceOrientation,
        // which means offset(Reference.BASE, Reference.OUTPUT, Axis.ABSOLUTE).
        stub.rotation = getAngles().offset(Reference.BASE, Reference.OUTPUT, Axis.ABSOLUTE);
        stub.videoFrameRate = Math.round(mPreviewFrameRate);
        LOG.i("onTakeVideoSnapshot", "rotation:", stub.rotation, "size:", stub.size);

        // Start.
        // The overlay rotation should alway be VIEW-OUTPUT, just liek Camera1Engine.
        int overlayRotation = getAngles().offset(Reference.VIEW, Reference.OUTPUT, Axis.ABSOLUTE);
        mVideoRecorder = new SnapshotVideoRecorder(this, glPreview, getOverlay(),
                overlayRotation);
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
        // snapshot, this function returns early so its safe.
        boolean needsIssue549Workaround = (mVideoRecorder instanceof Full2VideoRecorder) &&
                (readCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1)
                        == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        if (needsIssue549Workaround) {
            maybeRestorePreviewTemplateAfterVideo();
        }
    }

    @Override
    public void onVideoResult(@Nullable VideoResult.Stub result, @Nullable Exception exception) {
        super.onVideoResult(result, exception);
        // SnapshotRecorder will invoke this on its own thread, so let's post in our own thread
        // and check camera state before trying to restore the preview. Engine might have been
        // torn down in the engine thread while this was still being called.
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                if (getBindState() < STATE_STARTED) return;
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
        if (template != CameraDevice.TEMPLATE_PREVIEW) {
            try {
                createRepeatingRequestBuilder(CameraDevice.TEMPLATE_PREVIEW);
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
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                if (getEngineState() == STATE_STARTED) {
                    boolean shouldApply = applyFlash(mRepeatingRequestBuilder, old);
                    boolean needsWorkaround = getPreviewState() == STATE_STARTED;
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
                mFlashOp.end(null);
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
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                if (getEngineState() == STATE_STARTED) {
                    if (applyLocation(mRepeatingRequestBuilder, old)) {
                        applyRepeatingRequestBuilder();
                    }
                }
                mLocationOp.end(null);
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
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                if (getEngineState() == STATE_STARTED) {
                    if (applyWhiteBalance(mRepeatingRequestBuilder, old)) {
                        applyRepeatingRequestBuilder();
                    }
                }
                mWhiteBalanceOp.end(null);
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
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                if (getEngineState() == STATE_STARTED) {
                    if (applyHdr(mRepeatingRequestBuilder, old)) {
                        applyRepeatingRequestBuilder();
                    }
                }
                mHdrOp.end(null);
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
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                if (getEngineState() == STATE_STARTED) {
                    if (applyZoom(mRepeatingRequestBuilder, old)) {
                        applyRepeatingRequestBuilder();
                        if (notify) {
                            mCallback.dispatchOnZoomChanged(zoom, points);
                        }
                    }
                }
                mZoomOp.end(null);
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
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                if (getEngineState() == STATE_STARTED) {
                    if (applyExposureCorrection(mRepeatingRequestBuilder, old)) {
                        applyRepeatingRequestBuilder();
                        if (notify) {
                            mCallback.dispatchOnExposureCorrectionChanged(EVvalue, bounds, points);
                        }
                    }
                }
                mExposureCorrectionOp.end(null);
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
        mPlaySoundsOp.end(null);
    }

    @Override public void setPreviewFrameRate(float previewFrameRate) {
        final float oldPreviewFrameRate = mPreviewFrameRate;
        mPreviewFrameRate = previewFrameRate;
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                if (getEngineState() == STATE_STARTED) {
                    if (applyPreviewFrameRate(mRepeatingRequestBuilder, oldPreviewFrameRate)) {
                        applyRepeatingRequestBuilder();
                    }
                }
                mPreviewFrameRateOp.end(null);
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean applyPreviewFrameRate(@NonNull CaptureRequest.Builder builder,
                                            float oldPreviewFrameRate) {
        //noinspection unchecked
        Range<Integer>[] fallback = new Range[]{};
        Range<Integer>[] fpsRanges = readCharacteristic(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES,
                fallback);
        if (mPreviewFrameRate == 0F) {
            // 0F is a special value. Fallback to a reasonable default.
            for (Range<Integer> fpsRange : fpsRanges) {
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
            for (Range<Integer> fpsRange : fpsRanges) {
                if (fpsRange.contains(Math.round(mPreviewFrameRate))) {
                    builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                    return true;
                }
            }
        }
        mPreviewFrameRate = oldPreviewFrameRate;
        return false;
    }

    //endregion

    //region Frame Processing

    @NonNull
    @Override
    protected FrameManager instantiateFrameManager() {
        return new FrameManager(2, null);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        byte[] data = getFrameManager().getBuffer();
        if (data == null) {
            LOG.w("onImageAvailable", "no byte buffer!");
            return;
        }
        LOG.v("onImageAvailable", "trying to acquire Image.");
        Image image = null;
        try {
            image = reader.acquireLatestImage();
        } catch (IllegalStateException ignore) { }
        if (image == null) {
            LOG.w("onImageAvailable", "we have a byte buffer but no Image!");
            getFrameManager().onBufferUnused(data);
            return;
        }
        LOG.v("onImageAvailable", "we have both a byte buffer and an Image.");
        try {
            synchronized (mFrameProcessingImageLock) {
                ImageHelper.convertToNV21(image, data);
            }
        } catch (Exception e) {
            LOG.w("onImageAvailable", "error while converting.");
            getFrameManager().onBufferUnused(data);
            image.close();
            return;
        }
        image.close();
        if (getPreviewState() == STATE_STARTED) {
            // After preview, the frame manager is correctly set up
            Frame frame = getFrameManager().getFrame(data,
                    System.currentTimeMillis(),
                    getAngles().offset(Reference.SENSOR, Reference.OUTPUT,
                            Axis.RELATIVE_TO_SENSOR));
            mCallback.dispatchFrame(frame);
        } else {
            getFrameManager().onBufferUnused(data);
        }
    }

    @Override
    public void setHasFrameProcessors(final boolean hasFrameProcessors) {
        LOG.i("setHasFrameProcessors", "changing to", hasFrameProcessors, "posting.");
        Camera2Engine.super.setHasFrameProcessors(hasFrameProcessors);
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.i("setHasFrameProcessors", "changing to", hasFrameProcessors,
                        "executing. BindState:", getBindState(),
                        "PreviewState:", getPreviewState());

                // Frame processing is set up partially when binding and partially when starting
                // the preview. We don't want to only check bind state or startPreview can fail.
                if (getBindState() == STATE_STOPPED) {
                    LOG.i("setHasFrameProcessors", "not bound so won't restart.");
                } else if (getPreviewState() == STATE_STARTED) {
                    // This needs a restartBind(). NOTE: if taking video, this stops it.
                    LOG.i("setHasFrameProcessors", "bound with preview.",
                            "Calling restartBind().");
                    restartBind();
                } else {
                    // Bind+Preview is not completely started yet not completely stopped.
                    // This can happen if the user adds a frame processor in onCameraOpened().
                    // Supporting this would add lot of complexity to this class, and
                    // this should be discouraged anyway since changing the frame processor number
                    // at this time requires restarting the camera when it was just opened.
                    // For these reasons, let's throw.
                    throw new IllegalStateException("Added/removed a FrameProcessor at illegal " +
                            "time. These operations should be done before opening the camera, or " +
                            "before closing it - NOT when it just opened, for example during the " +
                            "onCameraOpened() callback.");
                }
            }
        });
    }

    //endregion

    //region 3A Metering

    @Override
    public void startAutoFocus(@Nullable final Gesture gesture, @NonNull final PointF point) {
        LOG.i("startAutoFocus", "dispatching. Gesture:", gesture);
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                LOG.i("startAutoFocus", "executing. Preview state:", getPreviewState());
                // This will only work when we have a preview, since it launches the preview
                // in the end. Even without this it would need the bind state at least,
                // since we need the preview size.
                if (getPreviewState() < STATE_STARTED) return;

                // The camera options API still has the auto focus API but it really
                // refers to "3A metering to a specific point". Since we have a point, check.
                if (!mCameraOptions.isAutoFocusSupported()) return;

                // Create the meter and start.
                mCallback.dispatchOnFocusStart(gesture, point);
                final MeterAction action = createMeterAction(point);
                Action wrapper = Actions.timeout(METER_TIMEOUT, action);
                wrapper.start(Camera2Engine.this);
                wrapper.addCallback(new CompletionCallback() {
                    @Override
                    protected void onActionCompleted(@NonNull Action a) {
                        mCallback.dispatchOnFocusEnd(gesture, action.isSuccessful(), point);
                        mHandler.remove(mUnlockAndResetMeteringRunnable);
                        if (shouldResetAutoFocus()) {
                            mHandler.post(getAutoFocusResetDelay(),
                                    mUnlockAndResetMeteringRunnable);
                        }
                    }
                });
            }
        });
    }

    @NonNull
    private MeterAction createMeterAction(@Nullable PointF point) {
        // Before creating any new meter action, abort the old one.
        if (mMeterAction != null) mMeterAction.abort(this);
        // The meter will check the current configuration to see if AF/AE/AWB should run.
        // - AE should be on CONTROL_AE_MODE_ON*    (this depends on setFlash())
        // - AWB should be on CONTROL_AWB_MODE_AUTO (this depends on setWhiteBalance())
        // - AF should be on CONTROL_AF_MODE_AUTO or others
        // The last one is under our control because the library has no focus API.
        // So let's set a good af mode here. This operation is reverted during onMeteringReset().
        applyFocusForMetering(mRepeatingRequestBuilder);
        mMeterAction = new MeterAction(Camera2Engine.this, point,
                point == null);
        return mMeterAction;
    }

    private final Runnable mUnlockAndResetMeteringRunnable = new Runnable() {
        @Override
        public void run() {
            unlockAndResetMetering();
        }
    };

    private void unlockAndResetMetering() {
        if (getEngineState() == STATE_STARTED) {
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

    @Override
    public void applyBuilder(@NonNull Action source) {
        applyRepeatingRequestBuilder();
    }

    @Override
    public void applyBuilder(@NonNull Action source, @NonNull CaptureRequest.Builder builder)
            throws CameraAccessException {
        mSession.capture(builder.build(), mRepeatingRequestCallback, null);
    }

    //endregion
}
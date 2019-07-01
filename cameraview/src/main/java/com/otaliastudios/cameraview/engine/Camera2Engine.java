package com.otaliastudios.cameraview.engine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.MediaCodec;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.internal.utils.CropHelper;
import com.otaliastudios.cameraview.picture.SnapshotGlPictureRecorder;
import com.otaliastudios.cameraview.preview.GlCameraPreview;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.video.Full2VideoRecorder;
import com.otaliastudios.cameraview.video.SnapshotVideoRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;

// TODO fix flash, it's not that simple
// TODO zoom
// TODO exposure correction
// TODO autofocus
// TODO pictures
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Engine extends CameraEngine {

    private static final String TAG = Camera2Engine.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private final CameraManager mManager;
    private String mCameraId;
    private CameraDevice mCamera;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mPreviewStreamRequestBuilder;
    private CaptureRequest mPreviewStreamRequest;
    private Surface mPreviewStreamSurface;

    // API 23+ for full video recording. The surface is created before.
    private Surface mFullVideoPersistentSurface;

    // API 21-22 for full video recording. When takeVideo is called, we have to reset the session.
    private VideoResult.Stub mFullVideoPendingStub;

    public Camera2Engine(Callback callback) {
        super(callback);
        mMapper = Mapper.get(Engine.CAMERA2);
        mManager = (CameraManager) mCallback.getContext().getSystemService(Context.CAMERA_SERVICE);
    }

    //region Utilities

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
            case CameraAccessException.CAMERA_DISABLED: reason = CameraException.REASON_FAILED_TO_CONNECT; break;
            case CameraAccessException.CAMERA_ERROR: reason = CameraException.REASON_DISCONNECTED; break;
            case CameraAccessException.CAMERA_DISCONNECTED: reason = CameraException.REASON_DISCONNECTED; break;
            case CameraAccessException.CAMERA_IN_USE: reason = CameraException.REASON_FAILED_TO_CONNECT; break;
            case CameraAccessException.MAX_CAMERAS_IN_USE: reason = CameraException.REASON_FAILED_TO_CONNECT; break;
            default: reason = CameraException.REASON_UNKNOWN; break;
        }
        return new CameraException(exception, reason);
    }

    @NonNull
    private CameraException createCameraException(int stateCallbackError) {
        int reason;
        switch (stateCallbackError) {
            case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED: reason = CameraException.REASON_FAILED_TO_CONNECT; break; // Device policy
            case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE: reason = CameraException.REASON_FAILED_TO_CONNECT; break; // Fatal error
            case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE: reason = CameraException.REASON_FAILED_TO_CONNECT; break; // Fatal error, device might have to be restarted
            case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE: reason = CameraException.REASON_FAILED_TO_CONNECT; break;
            case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE: reason = CameraException.REASON_FAILED_TO_CONNECT; break;
            default: reason = CameraException.REASON_UNKNOWN; break;
        }
        return new CameraException(reason);
    }

    //endregion

    //region Protected APIs

    @NonNull
    @Override
    protected List<Size> getPreviewStreamAvailableSizes() {
        try {
            CameraCharacteristics characteristics = mManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (streamMap == null)
                throw new RuntimeException("StreamConfigurationMap is null. Should not happen.");
            // This works because our previews return either a SurfaceTexture or a SurfaceHolder, which are
            // accepted class types by the getOutputSizes method.
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

    @WorkerThread
    @Override
    protected void onPreviewStreamSizeChanged() {
        restartBind();
    }

    @Override
    protected boolean collectCameraInfo(@NonNull Facing facing) {
        int internalFacing = mMapper.map(facing);
        String[] cameraIds = null;
        try {
            cameraIds = mManager.getCameraIdList();
        } catch (CameraAccessException e) {
            // This should never happen, I don't see how it could crash here.
            // However, let's launch an unrecoverable exception.
            throw createCameraException(e);
        }
        LOG.i("collectCameraInfo", "Facing:", facing, "Internal:", internalFacing, "Cameras:", cameraIds.length);
        for (String cameraId : cameraIds) {
            try {
                CameraCharacteristics characteristics = mManager.getCameraCharacteristics(cameraId);
                if (internalFacing == readCharacteristic(characteristics, CameraCharacteristics.LENS_FACING, -99)) {
                    mCameraId = cameraId;
                    mSensorOffset = readCharacteristic(characteristics, CameraCharacteristics.SENSOR_ORIENTATION, 0);
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
                        mCameraOptions = new CameraOptions(mManager, mCameraId, flip(REF_SENSOR, REF_VIEW));
                        mPreviewStreamRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        applyAll(mPreviewStreamRequestBuilder);
                    } catch (CameraAccessException e) {
                        task.trySetException(createCameraException(e));
                        return;
                    }
                    task.trySetResult(null);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    // Not sure if this is called INSTEAD of onOpened() or can be called after as well.
                    // However, using trySetException should address this problem - it will only trigger
                    // if the task has no result.
                    //
                    // Docs say to release this camera instance, however, since we throw an unrecoverable CameraException,
                    // this will trigger a stop() through the exception handler.
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

    @NonNull
    @Override
    protected Task<Void> onStartBind() {
        LOG.i("onStartBind:", "Started");
        final TaskCompletionSource<Void> task = new TaskCompletionSource<>();

        // Compute sizes.
        mCaptureSize = computeCaptureSize();
        mPreviewStreamSize = computePreviewStreamSize();

        // Create a preview surface with the correct size.In Camera2, instead of applying it to
        // the camera params object, we must resize our own surfaces.
        final Object output = mPreview.getOutput();
        if (output instanceof SurfaceHolder) {
            try {
                // This must be called from the UI thread...
                Tasks.await(Tasks.call(new Callable<Void>() {
                    @Override
                    public Void call() {
                        ((SurfaceHolder) output).setFixedSize(mPreviewStreamSize.getWidth(), mPreviewStreamSize.getHeight());
                        return null;
                    }
                }));
            } catch (ExecutionException | InterruptedException e) {
                throw new CameraException(e, CameraException.REASON_FAILED_TO_CONNECT);
            }
            mPreviewStreamSurface = ((SurfaceHolder) output).getSurface();
        } else if (output instanceof SurfaceTexture) {
            ((SurfaceTexture) output).setDefaultBufferSize(mPreviewStreamSize.getWidth(), mPreviewStreamSize.getHeight());
            mPreviewStreamSurface = new Surface((SurfaceTexture) output);
        } else {
            throw new RuntimeException("Unknown CameraPreview output class.");
        }

        Surface captureSurface = null;
        if (mMode == Mode.PICTURE) {
            // TODO picture recorder
        } else {
            if (Full2VideoRecorder.SUPPORTS_PERSISTENT_SURFACE) {
                mFullVideoPersistentSurface = MediaCodec.createPersistentInputSurface();
                captureSurface = mFullVideoPersistentSurface;
            } else if (mFullVideoPendingStub != null) {
                Full2VideoRecorder recorder = new Full2VideoRecorder(this, mCameraId, null);
                try {
                    captureSurface = recorder.createInputSurface(mFullVideoPendingStub);
                } catch (Full2VideoRecorder.PrepareException e) {
                    throw new CameraException(e, CameraException.REASON_FAILED_TO_CONNECT);
                }
                mVideoRecorder = recorder;
            }
        }

        List<Surface> outputSurfaces = new ArrayList<>();
        outputSurfaces.add(mPreviewStreamSurface);
        if (captureSurface != null) outputSurfaces.add(captureSurface);

        try {
            mPreviewStreamRequestBuilder.addTarget(mPreviewStreamSurface);

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
                    // I would say this should be a library error and as such we throw a Runtime Exception.
                    String message = LOG.e("onConfigureFailed! Session", session);
                    throw new RuntimeException(message);
                }
            }, null);
        } catch (CameraAccessException e) {
            throw createCameraException(e);
        }
        return task.getTask();
    }

    @NonNull
    @Override
    protected Task<Void> onStartPreview() {
        LOG.i("onStartPreview", "Dispatching onCameraPreviewStreamSizeChanged.");
        mCallback.onCameraPreviewStreamSizeChanged();

        Size previewSizeForView = getPreviewStreamSize(REF_VIEW);
        if (previewSizeForView == null) {
            throw new IllegalStateException("previewStreamSize should not be null at this point.");
        }
        mPreview.setStreamSize(previewSizeForView.getWidth(), previewSizeForView.getHeight());

        // Set the preview rotation.
        mPreview.setDrawRotation(mDisplayOffset);

        // TODO mPreviewStreamFormat = params.getPreviewFormat();
        // TODO mCamera.setPreviewCallbackWithBuffer(null); // Release anything left
        // TODO mCamera.setPreviewCallbackWithBuffer(this); // Add ourselves
        // TODO mFrameManager.setUp(ImageFormat.getBitsPerPixel(mPreviewStreamFormat), mPreviewStreamSize);

        LOG.i("onStartPreview", "Starting preview with startPreview().");
        try {
            mPreviewStreamRequest = mPreviewStreamRequestBuilder.build();
            mSession.setRepeatingRequest(mPreviewStreamRequest, null, null);
        } catch (Exception e) {
            // This is an unrecoverable exception that will stop everything.
            LOG.e("onStartPreview", "Failed to start preview.", e);
            throw new CameraException(e, CameraException.REASON_FAILED_TO_START_PREVIEW);
        }
        LOG.i("onStartPreview", "Started preview.");

        // Start delayed video if needed.
        if (mFullVideoPendingStub != null) {
            // Do not call takeVideo. It will reset some stub parameters that the recorder sets.
            onTakeVideo(mFullVideoPendingStub);
        }
        return Tasks.forResult(null);
    }

    //endregion

    //region Stop

    @NonNull
    @Override
    protected Task<Void> onStopPreview() {
        if (mVideoRecorder != null) {
            mVideoRecorder.stop();
            mVideoRecorder = null;
        }
        mPreviewStreamFormat = 0;
        getFrameManager().release();
        // TODO mCamera.setPreviewCallbackWithBuffer(null); // Release anything left
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
        mPreviewStreamRequest = null;
        return Tasks.forResult(null);
    }


    @NonNull
    @Override
    protected Task<Void> onStopBind() {
        if (mFullVideoPersistentSurface != null) {
            mFullVideoPersistentSurface.release();
            mFullVideoPersistentSurface = null;
        }
        mPreviewStreamRequestBuilder.removeTarget(mPreviewStreamSurface);
        mPreviewStreamSurface = null;
        mPreviewStreamSize = null;
        mCaptureSize = null;
        mSession.close();
        mSession = null;
        return Tasks.forResult(null);
    }


    @NonNull
    @Override
    protected Task<Void> onStopEngine() {
        LOG.i("onStopEngine:", "About to clean up.");
        try {
            LOG.i("onStopEngine:", "Clean up.", "Releasing camera.");
            mCamera.close();
            LOG.i("onStopEngine:", "Clean up.", "Released camera.");
        } catch (Exception e) {
            LOG.w("onStopEngine:", "Clean up.", "Exception while releasing camera.", e);
        }
        mCamera = null;
        mCameraOptions = null;
        mVideoRecorder = null;
        mPreviewStreamRequestBuilder = null;
        LOG.w("onStopEngine:", "Returning.");
        return Tasks.forResult(null);
    }

    //endregion

    //region Pictures

    @WorkerThread
    @Override
    protected void onTakePictureSnapshot(@NonNull PictureResult.Stub stub, @NonNull AspectRatio viewAspectRatio) {
        stub.size = getUncroppedSnapshotSize(REF_OUTPUT); // Not the real size: it will be cropped to match the view ratio
        stub.rotation = offset(REF_SENSOR, REF_OUTPUT); // Actually it will be rotated and set to 0.
        AspectRatio outputRatio = flip(REF_OUTPUT, REF_VIEW) ? viewAspectRatio.flip() : viewAspectRatio;
        if (mPreview instanceof GlCameraPreview) {
            mPictureRecorder = new SnapshotGlPictureRecorder(stub, this, (GlCameraPreview) mPreview, outputRatio);
        } else {
            throw new RuntimeException("takePictureSnapshot with Camera2 is only supported with Preview.GL_SURFACE");
        }
        mPictureRecorder.take();
    }

    //endregion

    //region Videos

    @WorkerThread
    @Override
    protected void onTakeVideo(@NonNull VideoResult.Stub stub) {
        stub.rotation = offset(REF_SENSOR, REF_OUTPUT);
        stub.size = flip(REF_SENSOR, REF_OUTPUT) ? mCaptureSize.flip() : mCaptureSize;
        if (!Full2VideoRecorder.SUPPORTS_PERSISTENT_SURFACE) {
            // On API 21 and 22, we must restart the session at each time.
            if (stub == mFullVideoPendingStub) {
                // We have restarted and are ready to take the video.
                doTakeVideo(mFullVideoPendingStub);
                mFullVideoPendingStub = null;
            } else {
                // Save the pending data and restart the session.
                mFullVideoPendingStub = stub;
                restartBind();
            }
        } else {
            doTakeVideo(stub);
        }
    }

    private void doTakeVideo(@NonNull final VideoResult.Stub stub) {
        if (!(mVideoRecorder instanceof Full2VideoRecorder)) {
            mVideoRecorder = new Full2VideoRecorder(this, mCameraId, mFullVideoPersistentSurface);
        }
        Full2VideoRecorder recorder = (Full2VideoRecorder) mVideoRecorder;
        try {
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            applyAll(builder);
            //noinspection ConstantConditions
            builder.addTarget(recorder.getInputSurface());
            builder.addTarget(mPreviewStreamSurface);

            final AtomicBoolean started = new AtomicBoolean(false);
            mSession.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    if (started.compareAndSet(false, true)) mVideoRecorder.start(stub);
                }
            }, null);
        } catch (CameraAccessException e) {
            onVideoResult(null, e);
            throw createCameraException(e);
        }
    }

    /**
     * See {@link CameraEngine#onTakeVideoSnapshot(VideoResult.Stub, AspectRatio)}
     * to read about the size and rotation computation.
     */
    @WorkerThread
    @Override
    protected void onTakeVideoSnapshot(@NonNull VideoResult.Stub stub, @NonNull AspectRatio viewAspectRatio) {
        if (!(mPreview instanceof GlCameraPreview)) {
            throw new IllegalStateException("Video snapshots are only supported with GlCameraPreview.");
        }
        GlCameraPreview glPreview = (GlCameraPreview) mPreview;
        Facing realFacing = mFacing;
        mFacing = Facing.BACK;
        Size outputSize = getUncroppedSnapshotSize(REF_OUTPUT);
        if (outputSize == null) {
            throw new IllegalStateException("outputSize should not be null.");
        }
        AspectRatio outputRatio = flip(REF_OUTPUT, REF_VIEW) ? viewAspectRatio.flip() : viewAspectRatio;
        Rect outputCrop = CropHelper.computeCrop(outputSize, outputRatio);
        outputSize = new Size(outputCrop.width(), outputCrop.height());
        stub.size = outputSize;
        stub.rotation = offset(REF_VIEW, REF_OUTPUT);

        // Reset facing and start.
        mFacing = realFacing;
        if (!(mVideoRecorder instanceof SnapshotVideoRecorder)) {
            mVideoRecorder = new SnapshotVideoRecorder(this, glPreview);
        }
        mVideoRecorder.start(stub);
    }

    @Override
    public void onVideoResult(@Nullable VideoResult.Stub result, @Nullable Exception exception) {
        if (mVideoRecorder instanceof Full2VideoRecorder) {
            // We have to stop all repeating requests and restart them.
            try {
                if (getBindState() == STATE_STARTED) {
                    mSession.stopRepeating();
                }
                if (getPreviewState() == STATE_STARTED) {
                    mPreviewStreamRequest = mPreviewStreamRequestBuilder.build();
                    mSession.setRepeatingRequest(mPreviewStreamRequest, null, null);
                }
            } catch (CameraAccessException e) {
                throw createCameraException(e);
            }
        }
        super.onVideoResult(result, exception);
    }

    //endregion

    //region Parameters

    private void syncStream() {
        if (getPreviewState() == STATE_STARTED) {
            try {
                // TODO if we are capturing a video or a picture, this is not what we should do!
                mPreviewStreamRequest = mPreviewStreamRequestBuilder.build();
                mSession.setRepeatingRequest(mPreviewStreamRequest, null, null);
            } catch (Exception e) {
                throw new CameraException(e, CameraException.REASON_DISCONNECTED);
            }
        }
    }

    private void applyAll(@NonNull CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        applyDefaultFocus(builder);
        applyFlash(builder, Flash.OFF);
        applyLocation(builder, null);
        applyWhiteBalance(builder, WhiteBalance.AUTO);
        applyHdr(builder, Hdr.OFF);
    }

    private void applyDefaultFocus(@NonNull CaptureRequest.Builder builder) {
        int[] modesArray = readCharacteristic(mCameraCharacteristics, CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, new int[]{});
        List<Integer> modes = new ArrayList<>();
        for (int mode : modesArray) { modes.add(mode); }
        if (mMode == Mode.VIDEO &&
                modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)) {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            return;
        }

        if (modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
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

    @Override
    public void setFlash(@NonNull Flash flash) {
        final Flash old = mFlash;
        mFlash = flash;
        mHandler.run(new Runnable() {
            @Override
            public void run() {
                if (getEngineState() == STATE_STARTED) {
                    if (applyFlash(mPreviewStreamRequestBuilder, old)) syncStream();
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
     */
    private boolean applyFlash(@NonNull CaptureRequest.Builder builder,
                               @NonNull Flash oldFlash) {
        if (mCameraOptions.supports(mFlash)) {
            List<Integer> modes = mMapper.map(mFlash);
            int[] availableModes = readCharacteristic(mCameraCharacteristics, CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, new int[]{});
            for (int mode : modes) {
                for (int availableMode : availableModes) {
                    if (mode == availableMode) {
                        builder.set(CaptureRequest.CONTROL_AE_MODE, mode);
                        return true;
                    }
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
                    if (applyLocation(mPreviewStreamRequestBuilder, old)) syncStream();
                }
                mLocationOp.end(null);
            }
        });
    }

    private boolean applyLocation(@NonNull CaptureRequest.Builder builder,
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
                    if (applyWhiteBalance(mPreviewStreamRequestBuilder, old)) syncStream();
                }
                mWhiteBalanceOp.end(null);
            }
        });
    }

    private boolean applyWhiteBalance(@NonNull CaptureRequest.Builder builder,
                                      @NonNull WhiteBalance oldWhiteBalance) {
        if (mCameraOptions.supports(mWhiteBalance)) {
            Integer whiteBalance = mMapper.map(mWhiteBalance);
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
                    if (applyHdr(mPreviewStreamRequestBuilder, old)) syncStream();
                }
                mHdrOp.end(null);
            }
        });
    }


    private boolean applyHdr( @NonNull CaptureRequest.Builder builder, @NonNull Hdr oldHdr) {
        if (mCameraOptions.supports(mHdr)) {
            Integer hdr = mMapper.map(mHdr);
            builder.set(CaptureRequest.CONTROL_SCENE_MODE, hdr);
            return true;
        }
        mHdr = oldHdr;
        return false;
    }

    @Override
    public void setPlaySounds(boolean playSounds) {
        mPlaySounds = playSounds;
        mPlaySoundsOp.end(null);
    }

    //endregion


    @Override
    public void onBufferAvailable(@NonNull byte[] buffer) {

    }

    @Override
    public void setZoom(float zoom, @Nullable PointF[] points, boolean notify) {

    }

    @Override
    public void setExposureCorrection(float EVvalue, @NonNull float[] bounds, @Nullable PointF[] points, boolean notify) {

    }

    @Override
    public void takePicture(@NonNull PictureResult.Stub stub) {

    }

    @Override
    public void startAutoFocus(@Nullable Gesture gesture, @NonNull PointF point) {

    }
}


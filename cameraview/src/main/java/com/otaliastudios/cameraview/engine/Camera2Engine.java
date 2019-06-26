package com.otaliastudios.cameraview.engine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.internal.utils.Task;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Engine extends CameraEngine {

    private static final String TAG = Camera2Engine.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private final CameraManager mManager;
    private String mCameraId;
    private CameraDevice mCamera;
    private CameraCaptureSession mSession; // TODO must be released and nulled
    private CaptureRequest.Builder mPreviewStreamRequestBuilder; // TODO must be nulled
    private CaptureRequest mPreviewStreamRequest; // TODO must be nulled
    private boolean mIsBound = false;

    public Camera2Engine(Callback callback) {
        super(callback);
        mMapper = Mapper.get(Engine.CAMERA2);
        mManager = (CameraManager) mCallback.getContext().getSystemService(Context.CAMERA_SERVICE);
    }

    private void schedule(@Nullable final Task<Void> task, final boolean ensureAvailable, final Runnable action) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (ensureAvailable && !isCameraAvailable()) {
                    if (task != null) task.end(null);
                } else {
                    action.run();
                    if (task != null) task.end(null);
                }
            }
        });
    }

    @NonNull
    private <T> T readCharacteristic(@NonNull CameraCharacteristics characteristics,
                                     @NonNull CameraCharacteristics.Key<T> key,
                                     @NonNull T fallback) {
        T value = characteristics.get(key);
        return value == null ? fallback : value;
    }

    @NonNull
    private Size computePreviewStreamSize() throws CameraAccessException {
        CameraCharacteristics characteristics = mManager.getCameraCharacteristics(mCameraId);
        StreamConfigurationMap streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamMap == null) throw new RuntimeException("StreamConfigurationMap is null. Should not happen.");
        // This works because our previews return either a SurfaceTexture or a SurfaceHolder, which are
        // accepted class types by the getOutputSizes method.
        android.util.Size[] sizes = streamMap.getOutputSizes(mPreview.getOutputClass());
        List<Size> candidates = new ArrayList<>(sizes.length);
        for (android.util.Size size : sizes) {
            Size add = new Size(size.getWidth(), size.getHeight());
            if (!candidates.contains(add)) candidates.add(add);
        }
        return computePreviewStreamSize(candidates);
    }

    private boolean collectCameraId() throws CameraAccessException {
        int internalFacing = mMapper.map(mFacing);
        int cameras = mManager.getCameraIdList().length;
        LOG.i("collectCameraId", "Facing:", mFacing, "Internal:", internalFacing, "Cameras:", cameras);
        for (String cameraId : mManager.getCameraIdList()) {
            CameraCharacteristics characteristics = mManager.getCameraCharacteristics(cameraId);
            if (internalFacing == readCharacteristic(characteristics, CameraCharacteristics.LENS_FACING, -99)) {
                mCameraId = cameraId;
                mSensorOffset = readCharacteristic(characteristics, CameraCharacteristics.SENSOR_ORIENTATION, 0);
                return true;
            }
        }
        return false;
    }

    private boolean isCameraAvailable() {
        switch (getState()) {
            // If we are stopped, don't.
            case STATE_STOPPED:
                return false;
            // If we are going to be closed, don't act on camera.
            // Even if mCamera != null, it might have been released.
            case STATE_STOPPING:
                return false;
            // If we are started, mCamera should never be null.
            case STATE_STARTED:
                return true;
            // If we are starting, theoretically we could act.
            // Just check that camera is available.
            case STATE_STARTING:
                return mCamera != null;
        }
        return false;
    }

    @Override
    protected void onStart() {
        if (isCameraAvailable()) {
            LOG.w("onStart:", "Camera not available. Should not happen.");
            onStop(); // Should not happen.
        }
        try {
            if (collectCameraId()) {
                createCamera();
                LOG.i("onStart:", "Ended");
            } else {
                LOG.e("onStart:", "No camera available for facing", mFacing);
                throw new CameraException(CameraException.REASON_NO_CAMERA);
            }
        } catch (CameraAccessException e) {
            // TODO
        }
    }

    @SuppressLint("MissingPermission")
    private void createCamera() throws CameraAccessException {
        mManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                mCamera = camera;

                // TODO Set parameters that might have been set before the camera was opened.
                try {
                    LOG.i("createCamera:", "Applying default parameters.");
                    CameraCharacteristics characteristics = mManager.getCameraCharacteristics(mCameraId);
                    mCameraOptions = new CameraOptions(mManager, characteristics, flip(REF_SENSOR, REF_VIEW));
                    // applyDefaultFocus(params);
                    // applyFlash(params, Flash.OFF);
                    // applyLocation(params, null);
                    // applyWhiteBalance(params, WhiteBalance.AUTO);
                    // applyHdr(params, Hdr.OFF);
                    // applyPlaySounds(mPlaySounds);
                    // params.setRecordingHint(mMode == Mode.VIDEO);
                    // mCamera.setParameters(params);
                } catch (CameraAccessException e) {
                    // TODO
                    throw new RuntimeException(e);
                }

                // Set display orientation, not allowed during preview
                // TODO not needed anymore? mCamera.setDisplayOrientation(offset(REF_SENSOR, REF_VIEW));

                try {
                    if (shouldBindToSurface()) bindToSurface("onStart");
                } catch (CameraAccessException e) {
                    // TODO
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                // TODO not sure what to do here. maybe stop(). Read docs.

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                // TODO
            }
        }, null);
    }

    private boolean shouldBindToSurface() {
        return isCameraAvailable() && mPreview != null && mPreview.hasSurface() && !mIsBound;
    }

    /**
     * The act of binding an "open" camera to a "ready" preview.
     * These can happen at different times but we want to end up here.
     * At this point we are sure that mPreview is not null.
     */
    @SuppressLint("Recycle")
    @WorkerThread
    private void bindToSurface(final @NonNull String trigger) throws CameraAccessException {
        LOG.i("bindToSurface:", "Started");
        Object output = mPreview.getOutput();
        Surface previewSurface;
        if (output instanceof SurfaceHolder) {
            previewSurface = ((SurfaceHolder) output).getSurface();
        } else if (output instanceof SurfaceTexture) {
            previewSurface = new Surface((SurfaceTexture) output);
        } else {
            throw new RuntimeException("Unknown CameraPreview output class.");
        }
        //noinspection ArraysAsListWithZeroOrOneArgument
        List<Surface> outputSurfaces = Arrays.asList(previewSurface);

        mPreviewStreamRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewStreamRequestBuilder.addTarget(previewSurface);

        // null handler means using the current looper which is totally ok.
        mCamera.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                try {
                    mCaptureSize = computeCaptureSize();
                    mPreviewStreamSize = computePreviewStreamSize();
                    mSession = session;
                    mIsBound = true;
                    if (shouldStartPreview()) startPreview(trigger);
                } catch (CameraAccessException e) {
                    // TODO
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                // TODO
            }

        }, null);
    }

    private boolean shouldStartPreview() {
        return isCameraAvailable() && mIsBound;
    }

    /**
     * To be called when the preview size is setup or changed.
     * @param trigger a log helper
     */
    private void startPreview(@NonNull String trigger) {
        LOG.i(trigger, "Dispatching onCameraPreviewStreamSizeChanged.");
        mCallback.onCameraPreviewStreamSizeChanged();

        Size previewSize = getPreviewStreamSize(REF_VIEW);
        if (previewSize == null) {
            throw new IllegalStateException("previewStreamSize should not be null at this point.");
        }
        mPreview.setStreamSize(previewSize.getWidth(), previewSize.getHeight());

        // TODO mPreviewStreamFormat = params.getPreviewFormat();

        // TODO: previewSize and captureSize
        /* params.setPreviewSize(mPreviewStreamSize.getWidth(), mPreviewStreamSize.getHeight()); // <- not allowed during preview
        if (mMode == Mode.PICTURE) {
            params.setPictureSize(mCaptureSize.getWidth(), mCaptureSize.getHeight()); // <- allowed
        } else {
            // mCaptureSize in this case is a video size. The available video sizes are not necessarily
            // a subset of the picture sizes, so we can't use the mCaptureSize value: it might crash.
            // However, the setPictureSize() passed here is useless : we don't allow HQ pictures in video mode.
            // While this might be lifted in the future, for now, just use a picture capture size.
            Size pictureSize = computeCaptureSize(Mode.PICTURE);
            params.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        } */

        // TODO mCamera.setPreviewCallbackWithBuffer(null); // Release anything left
        // TODO mCamera.setPreviewCallbackWithBuffer(this); // Add ourselves
        // TODO mFrameManager.allocateBuffers(ImageFormat.getBitsPerPixel(mPreviewStreamFormat), mPreviewStreamSize);

        LOG.i(trigger, "Starting preview with startPreview().");
        try {
            mPreviewStreamRequest = mPreviewStreamRequestBuilder.build();
            mSession.setRepeatingRequest(mPreviewStreamRequest, null, null);
        } catch (Exception e) {
            LOG.e(trigger, "Failed to start preview.", e);
            throw new CameraException(e, CameraException.REASON_FAILED_TO_START_PREVIEW);
        }
        LOG.i(trigger, "Started preview.");
    }

    @Override
    protected void onStop() {
        LOG.i("onStop:", "About to clean up.");
        if (mVideoRecorder != null) {
            mVideoRecorder.stop();
            mVideoRecorder = null;
        }
        if (mCamera != null) {
            stopPreview();
            if (mIsBound) unbindFromSurface();
            destroyCamera();
        }
        mCameraOptions = null;
        mCamera = null;
        mPreviewStreamSize = null;
        mCaptureSize = null;
        mIsBound = false;
        LOG.w("onStop:", "Clean up.", "Returning.");
    }

    private void stopPreview() {
        mPreviewStreamFormat = 0;
        mFrameManager.release();
        // TODO mCamera.setPreviewCallbackWithBuffer(null); // Release anything left
        try {
            mSession.stopRepeating();
            // TODO should wait for onReady?
        } catch (CameraAccessException e) {
            LOG.w("stopRepeating failed!", e);
        }
        mPreviewStreamRequest = null;
    }

    private void unbindFromSurface() {
        mIsBound = false;
        mPreviewStreamRequestBuilder = null;
        mPreviewStreamSize = null;
        mCaptureSize = null;
        mSession.close();
        mSession = null;
    }

    private void destroyCamera() {
        try {
            LOG.i("destroyCamera:", "Clean up.", "Releasing camera.");
            mCamera.close();
            LOG.i("destroyCamera:", "Clean up.", "Released camera.");
        } catch (Exception e) {
            LOG.w("destroyCamera:", "Clean up.", "Exception while releasing camera.", e);
        }
        mCamera = null;
        mCameraOptions = null;
    }


    /**
     * Preview surface is now available. If camera is open, set up.
     * At this point we are sure that mPreview is not null.
     */
    @Override
    public void onSurfaceAvailable() {
        LOG.i("onSurfaceAvailable:", "Size is", getPreviewSurfaceSize(REF_VIEW));
        schedule(null, false, new Runnable() {
            @Override
            public void run() {
                LOG.i("onSurfaceAvailable:", "Inside handler. About to bind.");
                try {
                    if (shouldBindToSurface()) bindToSurface("onSurfaceAvailable");
                } catch (CameraAccessException e) {
                    // TODO
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Preview surface did change its size. Compute a new preview size.
     * This requires stopping and restarting the preview.
     * At this point we are sure that mPreview is not null.
     */
    @Override
    public void onSurfaceChanged() {
        LOG.i("onSurfaceChanged, size is", getPreviewSurfaceSize(REF_VIEW));
        schedule(null, true, new Runnable() {
            @Override
            public void run() {
                if (!mIsBound) return;

                // Compute a new camera preview size and apply.
                try {
                    Size newSize = computePreviewStreamSize();
                    if (newSize.equals(mPreviewStreamSize)) return;
                    LOG.i("onSurfaceChanged:", "Computed a new preview size. Going on.");
                    mPreviewStreamSize = newSize;
                } catch (CameraAccessException e) {
                    // TODO
                    throw new RuntimeException(e);
                }
                stopPreview();
                startPreview("onSurfaceChanged:");
            }
        });
    }

    @Override
    public void onSurfaceDestroyed() {
        LOG.i("onSurfaceDestroyed");
        schedule(null, true, new Runnable() {
            @Override
            public void run() {
                stopPreview();
                if (mIsBound) unbindFromSurface();
            }
        });
    }














    @Override
    public void onBufferAvailable(@NonNull byte[] buffer) {

    }

    @Override
    public void setMode(@NonNull Mode mode) {

    }

    @Override
    public void setFacing(@NonNull Facing facing) {
        final Facing old = mFacing;
        if (facing != old) {
            mFacing = facing;
            schedule(null, true, new Runnable() {
                @Override
                public void run() {
                    boolean success;
                    try {
                        success = collectCameraId();
                    } catch (CameraAccessException e) {
                        success = false;
                    }
                    if (success) {
                        restart();
                    } else {
                        mFacing = old;
                    }
                }
            });
        }
    }

    @Override
    public void setZoom(float zoom, @Nullable PointF[] points, boolean notify) {

    }

    @Override
    public void setExposureCorrection(float EVvalue, @NonNull float[] bounds, @Nullable PointF[] points, boolean notify) {

    }

    @Override
    public void setFlash(@NonNull Flash flash) {

    }

    @Override
    public void setWhiteBalance(@NonNull WhiteBalance whiteBalance) {

    }

    @Override
    public void setHdr(@NonNull Hdr hdr) {

    }

    @Override
    public void setLocation(@Nullable Location location) {

    }

    @Override
    public void setAudio(@NonNull Audio audio) {

    }

    @Override
    public void takePicture(@NonNull PictureResult.Stub stub) {

    }

    @Override
    public void takePictureSnapshot(@NonNull PictureResult.Stub stub, @NonNull AspectRatio viewAspectRatio) {

    }

    @Override
    public void takeVideo(@NonNull VideoResult.Stub stub, @NonNull File file) {

    }

    @Override
    public void takeVideoSnapshot(@NonNull VideoResult.Stub stub, @NonNull File file, @NonNull AspectRatio viewAspectRatio) {

    }

    @Override
    public void stopVideo() {

    }

    @Override
    public void startAutoFocus(@Nullable Gesture gesture, @NonNull PointF point) {

    }

    @Override
    public void setPlaySounds(boolean playSounds) {

    }
}


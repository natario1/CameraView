package com.otaliastudios.cameraview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("deprecation")
class Camera1 extends CameraController implements Camera.PreviewCallback, Camera.ErrorCallback,
        VideoRecorder.VideoResultListener,
        PictureRecorder.PictureResultListener {

    private static final String TAG = Camera1.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private Camera mCamera;
    private boolean mIsBound = false;

    private final int mPostFocusResetDelay = 3000;
    private Runnable mPostFocusResetRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCameraAvailable()) return;
            mCamera.cancelAutoFocus();
            Camera.Parameters params = mCamera.getParameters();
            int maxAF = params.getMaxNumFocusAreas();
            int maxAE = params.getMaxNumMeteringAreas();
            if (maxAF > 0) params.setFocusAreas(null);
            if (maxAE > 0) params.setMeteringAreas(null);
            applyDefaultFocus(params); // Revert to internal focus.
            mCamera.setParameters(params);
        }
    };

    Camera1(@NonNull CameraView.CameraCallbacks callback) {
        super(callback);
        mMapper = new Mapper1();
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
                if (shouldBindToSurface()) bindToSurface();
                if (shouldStartPreview()) startPreview("onSurfaceAvailable");
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

                // Compute a new camera preview size.
                Size newSize = computePreviewStreamSize(sizesFromList(mCamera.getParameters().getSupportedPreviewSizes()));
                if (newSize.equals(mPreviewStreamSize)) return;

                // Apply.
                LOG.i("onSurfaceChanged:", "Computed a new preview size. Going on.");
                mPreviewStreamSize = newSize;
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

    private boolean shouldBindToSurface() {
        return isCameraAvailable() && mPreview != null && mPreview.hasSurface() && !mIsBound;
    }

    /**
     * The act of binding an "open" camera to a "ready" preview.
     * These can happen at different times but we want to end up here.
     * At this point we are sure that mPreview is not null.
     */
    @WorkerThread
    private void bindToSurface() {
        LOG.i("bindToSurface:", "Started");
        Object output = mPreview.getOutput();
        try {
            if (output instanceof SurfaceHolder) {
                mCamera.setPreviewDisplay((SurfaceHolder) output);
            } else if (output instanceof SurfaceTexture) {
                mCamera.setPreviewTexture((SurfaceTexture) output);
            } else {
                throw new RuntimeException("Unknown CameraPreview output class.");
            }
        } catch (IOException e) {
            LOG.e("bindToSurface:", "Failed to bind.", e);
            throw new CameraException(e, CameraException.REASON_FAILED_TO_START_PREVIEW);
        }

        mCaptureSize = computeCaptureSize();
        mPreviewStreamSize = computePreviewStreamSize(sizesFromList(mCamera.getParameters().getSupportedPreviewSizes()));
        mIsBound = true;
    }

    @WorkerThread
    private void unbindFromSurface() {
        mIsBound = false;
        mPreviewStreamSize = null;
        mCaptureSize = null;
        try {
            if (mPreview.getOutputClass() == SurfaceHolder.class) {
                mCamera.setPreviewDisplay(null);
            } else if (mPreview.getOutputClass() == SurfaceTexture.class) {
                mCamera.setPreviewTexture(null);
            } else {
                throw new RuntimeException("Unknown CameraPreview output class.");
            }
        } catch (IOException e) {
            LOG.e("unbindFromSurface", "Could not release surface", e);
        }
    }

    private boolean shouldStartPreview() {
        return isCameraAvailable() && mIsBound;
    }

    // To be called when the preview size is setup or changed.
    private void startPreview(String log) {
        LOG.i(log, "Dispatching onCameraPreviewStreamSizeChanged.");
        mCameraCallbacks.onCameraPreviewStreamSizeChanged();

        Size previewSize = getPreviewStreamSize(REF_VIEW);
        boolean wasFlipped = flip(REF_SENSOR, REF_VIEW);
        mPreview.setStreamSize(previewSize.getWidth(), previewSize.getHeight(), wasFlipped);

        Camera.Parameters params = mCamera.getParameters();
        mPreviewFormat = params.getPreviewFormat();
        params.setPreviewSize(mPreviewStreamSize.getWidth(), mPreviewStreamSize.getHeight()); // <- not allowed during preview
        if (mMode == Mode.PICTURE) {
            params.setPictureSize(mCaptureSize.getWidth(), mCaptureSize.getHeight()); // <- allowed
        } else {
            // mCaptureSize in this case is a video size. The available video sizes are not necessarily
            // a subset of the picture sizes, so we can't use the mCaptureSize value: it might crash.
            // However, the setPictureSize() passed here is useless : we don't allow HQ pictures in video mode.
            // While this might be lifted in the future, for now, just use a picture capture size.
            Size pictureSize = computeCaptureSize(Mode.PICTURE);
            params.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        }
        mCamera.setParameters(params);

        mCamera.setPreviewCallbackWithBuffer(null); // Release anything left
        mCamera.setPreviewCallbackWithBuffer(this); // Add ourselves
        mFrameManager.allocate(ImageFormat.getBitsPerPixel(mPreviewFormat), mPreviewStreamSize);

        LOG.i(log, "Starting preview with startPreview().");
        try {
            mCamera.startPreview();
        } catch (Exception e) {
            LOG.e(log, "Failed to start preview.", e);
            throw new CameraException(e, CameraException.REASON_FAILED_TO_START_PREVIEW);
        }
        LOG.i(log, "Started preview.");
    }

    private void stopPreview() {
        mPreviewFormat = 0;
        mFrameManager.release();
        mCamera.setPreviewCallbackWithBuffer(null); // Release anything left
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            LOG.e("stopPreview", "Could not stop preview", e);
        }
    }

    private void createCamera() {
        try {
            mCamera = Camera.open(mCameraId);
        } catch (Exception e) {
            LOG.e("createCamera:", "Failed to connect. Maybe in use by another app?");
            throw new CameraException(e, CameraException.REASON_FAILED_TO_CONNECT);
        }
        mCamera.setErrorCallback(this);

        // Set parameters that might have been set before the camera was opened.
        LOG.i("createCamera:", "Applying default parameters.");
        Camera.Parameters params = mCamera.getParameters();
        mCameraOptions = new CameraOptions(params, flip(REF_SENSOR, REF_VIEW));
        applyDefaultFocus(params);
        applyFlash(params, Flash.DEFAULT);
        applyLocation(params, null);
        applyWhiteBalance(params, WhiteBalance.DEFAULT);
        applyHdr(params, Hdr.DEFAULT);
        applyPlaySounds(mPlaySounds);
        params.setRecordingHint(mMode == Mode.VIDEO);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(offset(REF_SENSOR, REF_VIEW)); // <- not allowed during preview
    }

    private void destroyCamera() {
        try {
            LOG.i("destroyCamera:", "Clean up.", "Releasing camera.");
            mCamera.release();
            LOG.i("destroyCamera:", "Clean up.", "Released camera.");
        } catch (Exception e) {
            LOG.w("destroyCamera:", "Clean up.", "Exception while releasing camera.", e);
        }
        mCamera = null;
        mCameraOptions = null;
    }

    @WorkerThread
    @Override
    void onStart() {
        if (isCameraAvailable()) {
            LOG.w("onStart:", "Camera not available. Should not happen.");
            onStop(); // Should not happen.
        }
        if (collectCameraId()) {
            createCamera();
            if (shouldBindToSurface()) bindToSurface();
            if (shouldStartPreview()) startPreview("onStart");
            LOG.i("onStart:", "Ended");
        } else {
            LOG.e("onStart:", "No camera available for facing", mFacing);
            throw new CameraException(CameraException.REASON_NO_CAMERA);
        }
    }

    @WorkerThread
    @Override
    void onStop() {
        LOG.i("onStop:", "About to clean up.");
        mHandler.get().removeCallbacks(mPostFocusResetRunnable);
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

        // We were saving a reference to the exception here and throwing to the user.
        // I don't think it's correct. We are closing and have already done our best
        // to clean up resources. No need to throw.
        // if (error != null) throw new CameraException(error);
    }

    private boolean collectCameraId() {
        int internalFacing = mMapper.map(mFacing);
        LOG.i("collectCameraId", "Facing:", mFacing, "Internal:", internalFacing, "Cameras:", Camera.getNumberOfCameras());
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == internalFacing) {
                mSensorOffset = cameraInfo.orientation;
                mCameraId = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBufferAvailable(@NonNull byte[] buffer) {
        // TODO: sync with handler?
        if (isCameraAvailable()) {
            mCamera.addCallbackBuffer(buffer);
        }
    }

    @Override
    public void onError(int error, Camera camera) {
        if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
            // Looks like this is recoverable.
            LOG.w("Recoverable error inside the onError callback.", "CAMERA_ERROR_SERVER_DIED");
            stopImmediately();
            start();
            return;
        }

        LOG.e("Error inside the onError callback.", error);
        Exception runtime = new RuntimeException(CameraLogger.lastMessage);
        int reason;
        switch (error) {
            case Camera.CAMERA_ERROR_EVICTED: reason = CameraException.REASON_DISCONNECTED; break;
            case Camera.CAMERA_ERROR_UNKNOWN: reason = CameraException.REASON_UNKNOWN; break;
            default: reason = CameraException.REASON_UNKNOWN;
        }
        throw new CameraException(runtime, reason);
    }

    @Override
    void setMode(@NonNull Mode mode) {
        if (mode != mMode) {
            mMode = mode;
            schedule(null, true, new Runnable() {
                @Override
                public void run() {
                    restart();
                }
            });
        }
    }

    @Override
    void setLocation(@Nullable Location location) {
        final Location oldLocation = mLocation;
        mLocation = location;
        schedule(mLocationTask, true, new Runnable() {
            @Override
            public void run() {
                Camera.Parameters params = mCamera.getParameters();
                if (applyLocation(params, oldLocation)) mCamera.setParameters(params);
            }
        });
    }

    private boolean applyLocation(@NonNull Camera.Parameters params, @Nullable Location oldLocation) {
        if (mLocation != null) {
            params.setGpsLatitude(mLocation.getLatitude());
            params.setGpsLongitude(mLocation.getLongitude());
            params.setGpsAltitude(mLocation.getAltitude());
            params.setGpsTimestamp(mLocation.getTime());
            params.setGpsProcessingMethod(mLocation.getProvider());
        }
        return true;
    }

    @Override
    void setFacing(@NonNull Facing facing) {
        final Facing old = mFacing;
        if (facing != old) {
            mFacing = facing;
            schedule(null, true, new Runnable() {
                @Override
                public void run() {
                    if (collectCameraId()) {
                        restart();
                    } else {
                        mFacing = old;
                    }
                }
            });
        }
    }

    @Override
    void setWhiteBalance(@NonNull WhiteBalance whiteBalance) {
        final WhiteBalance old = mWhiteBalance;
        mWhiteBalance = whiteBalance;
        schedule(mWhiteBalanceTask, true, new Runnable() {
            @Override
            public void run() {
                Camera.Parameters params = mCamera.getParameters();
                if (applyWhiteBalance(params, old)) mCamera.setParameters(params);
            }
        });
    }

    private boolean applyWhiteBalance(@NonNull Camera.Parameters params, @NonNull WhiteBalance oldWhiteBalance) {
        if (mCameraOptions.supports(mWhiteBalance)) {
            params.setWhiteBalance((String) mMapper.map(mWhiteBalance));
            return true;
        }
        mWhiteBalance = oldWhiteBalance;
        return false;
    }

    @Override
    void setHdr(@NonNull Hdr hdr) {
        final Hdr old = mHdr;
        mHdr = hdr;
        schedule(mHdrTask, true, new Runnable() {
            @Override
            public void run() {
                Camera.Parameters params = mCamera.getParameters();
                if (applyHdr(params, old)) mCamera.setParameters(params);
            }
        });
    }

    private boolean applyHdr(@NonNull Camera.Parameters params, @NonNull Hdr oldHdr) {
        if (mCameraOptions.supports(mHdr)) {
            params.setSceneMode((String) mMapper.map(mHdr));
            return true;
        }
        mHdr = oldHdr;
        return false;
    }

    @TargetApi(17)
    private boolean applyPlaySounds(boolean oldPlaySound) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, info);
            if (info.canDisableShutterSound) {
                try {
                    // this method is documented to throw on some occasions. #377
                    return mCamera.enableShutterSound(mPlaySounds);
                } catch (RuntimeException exception) {
                    return false;
                }
            }
        }
        if (mPlaySounds) {
            return true;
        }
        mPlaySounds = oldPlaySound;
        return false;
    }


    @Override
    void setAudio(@NonNull Audio audio) {
        if (mAudio != audio) {
            if (isTakingVideo()) {
                LOG.w("Audio setting was changed while recording. " +
                        "Changes will take place starting from next video");
            }
            mAudio = audio;
        }
    }

    @Override
    void setFlash(@NonNull Flash flash) {
        final Flash old = mFlash;
        mFlash = flash;
        schedule(mFlashTask, true, new Runnable() {
            @Override
            public void run() {
                Camera.Parameters params = mCamera.getParameters();
                if (applyFlash(params, old)) mCamera.setParameters(params);
            }
        });
    }


    private boolean applyFlash(@NonNull Camera.Parameters params, @NonNull Flash oldFlash) {
        if (mCameraOptions.supports(mFlash)) {
            params.setFlashMode((String) mMapper.map(mFlash));
            return true;
        }
        mFlash = oldFlash;
        return false;
    }


    // Choose the best default focus, based on session type.
    private void applyDefaultFocus(@NonNull Camera.Parameters params) {
        List<String> modes = params.getSupportedFocusModes();

        if (mMode == Mode.VIDEO &&
                modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            return;
        }

        if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            return;
        }

        if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            return;
        }

        if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    // -----------------
    // Picture recording stuff.


    @Override
    public void onPictureShutter(boolean didPlaySound) {
        mCameraCallbacks.onShutter(!didPlaySound);
    }

    @Override
    public void onPictureResult(@Nullable PictureResult result) {
        mPictureRecorder = null;
        if (result != null) {
            mCameraCallbacks.dispatchOnPictureTaken(result);
        } else {
            // Something went wrong.
            mCameraCallbacks.dispatchError(new CameraException(CameraException.REASON_PICTURE_FAILED));
            LOG.e("onPictureResult", "result is null: something went wrong.");
        }
    }

    @Override
    void takePicture() {
        LOG.v("takePicture: scheduling");
        schedule(null, true, new Runnable() {
            @Override
            public void run() {
                if (mMode == Mode.VIDEO) {
                    // Could redirect to takePictureSnapshot, but it's better if people know
                    // what they are doing.
                    throw new IllegalStateException("Can't take hq pictures while in VIDEO mode");
                }

                LOG.v("takePicture: performing.", isTakingPicture());
                if (isTakingPicture()) return;
                PictureResult result = new PictureResult();
                result.isSnapshot = false;
                result.location = mLocation;
                result.rotation = offset(REF_SENSOR, REF_OUTPUT);
                result.size = getPictureSize(REF_OUTPUT);
                result.facing = mFacing;
                mPictureRecorder = new FullPictureRecorder(result, Camera1.this, mCamera);
                mPictureRecorder.take();
            }
        });
    }

    /**
     * Just a note about the snapshot size - it is the PreviewStreamSize, cropped with the view ratio.
     * @param viewAspectRatio the view aspect ratio
     */
    @Override
    void takePictureSnapshot(@NonNull final AspectRatio viewAspectRatio) {
        LOG.v("takePictureSnapshot: scheduling");
        schedule(null, true, new Runnable() {
            @Override
            public void run() {
                LOG.v("takePictureSnapshot: performing.", isTakingPicture());
                if (isTakingPicture()) return;

                PictureResult result = new PictureResult();
                result.location = mLocation;
                result.isSnapshot = true;
                result.facing = mFacing;
                result.size = getUncroppedSnapshotSize(REF_OUTPUT); // Not the real size: it will be cropped to match the view ratio
                result.rotation = offset(REF_SENSOR, REF_OUTPUT); // Actually it will be rotated and set to 0.
                AspectRatio outputRatio = flip(REF_OUTPUT, REF_VIEW) ? viewAspectRatio.inverse() : viewAspectRatio;
                // LOG.e("ROTBUG_pic", "aspectRatio (REF_VIEW):", viewAspectRatio);
                // LOG.e("ROTBUG_pic", "aspectRatio (REF_OUTPUT):", outputRatio);
                // LOG.e("ROTBUG_pic", "sizeUncropped (REF_OUTPUT):", result.size);
                // LOG.e("ROTBUG_pic", "rotation:", result.rotation);

                LOG.v("Rotations", "SV", offset(REF_SENSOR, REF_VIEW), "VS", offset(REF_VIEW, REF_SENSOR));
                LOG.v("Rotations", "SO", offset(REF_SENSOR, REF_OUTPUT), "OS", offset(REF_OUTPUT, REF_SENSOR));
                LOG.v("Rotations", "VO", offset(REF_VIEW, REF_OUTPUT), "OV", offset(REF_OUTPUT, REF_VIEW));

                mPictureRecorder = new SnapshotPictureRecorder(result, Camera1.this, mCamera, outputRatio);
                mPictureRecorder.take();
            }
        });
    }

    @Override
    public void onPreviewFrame(@NonNull byte[] data, Camera camera) {
        Frame frame = mFrameManager.getFrame(data,
                System.currentTimeMillis(),
                offset(REF_SENSOR, REF_OUTPUT),
                mPreviewStreamSize,
                mPreviewFormat);
        mCameraCallbacks.dispatchFrame(frame);
    }

    private boolean isCameraAvailable() {
        switch (mState) {
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

    // -----------------
    // Video recording stuff.

    @Override
    public void onVideoResult(@Nullable VideoResult result) {
        mVideoRecorder = null;
        if (result != null) {
            mCameraCallbacks.dispatchOnVideoTaken(result);
        } else {
            // Something went wrong, lock the camera again.
            mCameraCallbacks.dispatchError(new CameraException(CameraException.REASON_VIDEO_FAILED));
            mCamera.lock();
        }
    }

    @Override
    void takeVideo(@NonNull final File videoFile) {
        schedule(mStartVideoTask, true, new Runnable() {
            @Override
            public void run() {
                if (mMode == Mode.PICTURE) {
                    throw new IllegalStateException("Can't record video while in PICTURE mode");
                }

                if (isTakingVideo()) return;

                // Create the video result stub
                VideoResult videoResult = new VideoResult();
                videoResult.file = videoFile;
                videoResult.isSnapshot = false;
                videoResult.codec = mVideoCodec;
                videoResult.location = mLocation;
                videoResult.facing = mFacing;
                videoResult.rotation = offset(REF_SENSOR, REF_OUTPUT);
                videoResult.size = flip(REF_SENSOR, REF_OUTPUT) ? mCaptureSize.flip() : mCaptureSize;
                videoResult.audio = mAudio;
                videoResult.maxSize = mVideoMaxSize;
                videoResult.maxDuration = mVideoMaxDuration;
                videoResult.videoBitRate = mVideoBitRate;
                videoResult.audioBitRate = mAudioBitRate;

                // Unlock the camera and start recording.
                try {
                    mCamera.unlock();
                } catch (Exception e) {
                    // If this failed, we are unlikely able to record the video.
                    // Dispatch an error.
                    onVideoResult(null);
                    return;
                }
                mVideoRecorder = new FullVideoRecorder(videoResult, Camera1.this,
                        Camera1.this, mCamera, mCameraId);
                mVideoRecorder.start();
            }
        });
    }

    /**
     * @param file the output file
     * @param viewAspectRatio the view aspect ratio
     */
    @SuppressLint("NewApi")
    @Override
    void takeVideoSnapshot(@NonNull final File file, @NonNull final AspectRatio viewAspectRatio) {
        if (!(mPreview instanceof GlCameraPreview)) {
            throw new IllegalStateException("Video snapshots are only supported with GlCameraPreview.");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            throw new IllegalStateException("Video snapshots are only supported starting from API 18.");
        }
        schedule(mStartVideoTask, true, new Runnable() {
            @Override
            public void run() {
                if (isTakingVideo()) return;

                // Create the video result stub
                VideoResult videoResult = new VideoResult();
                videoResult.file = file;
                videoResult.isSnapshot = true;
                videoResult.codec = mVideoCodec;
                videoResult.location = mLocation;
                videoResult.facing = mFacing;
                videoResult.videoBitRate = mVideoBitRate;
                videoResult.audioBitRate = mAudioBitRate;
                videoResult.audio = mAudio;
                videoResult.maxSize = mVideoMaxSize;
                videoResult.maxDuration = mVideoMaxDuration;

                // Size and rotation turned out to be extremely tricky. In case of SnapshotPictureRecorder
                // we use the preview size in REF_OUTPUT (cropped) and offset(REF_SENSOR, REF_OUTPUT) as rotation.
                // These values mean that we expect input to be in the REF_SENSOR system.

                // Here everything seems different. We would expect a difference because the two snapshot
                // recorders have different mechanics (the picture one uses a SurfaceTexture with setBufferSize,
                // the video one here uses the MediaCodec input surface which we can't control).

                // The strangest thing is the fact that the correct angle seems to be the same for FRONT and
                // BACK sensor, which means that our sensor correction actually screws things up. For this reason
                // facing value is temporarily set to BACK.
                Facing realFacing = mFacing;
                mFacing = Facing.BACK;

                // These are the angles that make it work on a Nexus5X, compared to the offset() results.
                // For instance, SV means offset(REF_SENSOR, REF_VIEW). The rest should be clear.
                //    CONFIG   | WANTED |   SV   |   VS   |   VO   |   OV   |   SO   |   OS   |
                // ------------|--------|--------|--------|--------|--------|--------|--------|
                //   Vertical  |   0    |   270  |   90   |   0    |   0    |   270  |   90   |
                //     Left    |   270  |   270  |   90   |  270   |   90   |   180  |   180  |
                //    Right    |   90   |   270  |   90   |   90   |   270  |    0   |    0   |
                // Upside down |   180  |   270  |   90   |  180   |   180  |   90   |   270  |

                // The VO is the only correct value. Things change when using FRONT camera, in which case,
                // no value is actually correct, and the needed values are the same of BACK!
                //    CONFIG   | WANTED |   SV   |   VS   |   VO   |   OV   |   SO   |   OS   |
                // ------------|--------|--------|--------|--------|--------|--------|--------|
                //   Vertical  |   0    |   90   |   270  |  180   |   180  |   270  |   90   |
                //     Left    |   270  |   90   |   270  |  270   |   90   |    0   |    0   |
                //    Right    |   90   |   90   |   270  |   90   |   270  |   180  |   180  |
                // Upside down |   180  |   90   |   270  |   0    |    0   |   90   |   270  |

                // Based on this we will use VO for everything. See if we get issues about distortion
                // and maybe we can improve. The reason why this happen is beyond my understanding.

                Size outputSize = getUncroppedSnapshotSize(REF_OUTPUT);
                AspectRatio outputRatio = flip(REF_OUTPUT, REF_VIEW) ? viewAspectRatio.inverse() : viewAspectRatio;
                Rect outputCrop = CropHelper.computeCrop(outputSize, outputRatio);
                outputSize = new Size(outputCrop.width(), outputCrop.height());
                videoResult.size = outputSize;
                videoResult.rotation = offset(REF_VIEW, REF_OUTPUT);
                // LOG.e("ROTBUG_video", "aspectRatio (REF_VIEW):", viewAspectRatio);
                // LOG.e("ROTBUG_video", "aspectRatio (REF_OUTPUT):", outputRatio);
                // LOG.e("ROTBUG_video", "sizeUncropped (REF_OUTPUT):", outputSize);
                // LOG.e("ROTBUG_video", "sizeCropped (REF_OUTPUT):", videoResult.size);
                // LOG.e("ROTBUG_video", "rotation:", videoResult.rotation);

                // Reset facing and start.
                mFacing = realFacing;
                GlCameraPreview cameraPreview = (GlCameraPreview) mPreview;
                mVideoRecorder = new SnapshotVideoRecorder(videoResult, Camera1.this, cameraPreview);
                mVideoRecorder.start();
            }
        });
    }

    @Override
    void stopVideo() {
        schedule(null, false, new Runnable() {
            @Override
            public void run() {
                LOG.i("stopVideo", "mVideoRecorder is null?", mVideoRecorder == null);
                if (mVideoRecorder != null) {
                    mVideoRecorder.stop();
                    mVideoRecorder = null;
                }
            }
        });
    }

    // -----------------
    // Zoom and simpler stuff.


    @Override
    void setZoom(final float zoom, @Nullable final PointF[] points, final boolean notify) {
        schedule(mZoomTask, true, new Runnable() {
            @Override
            public void run() {
                if (!mCameraOptions.isZoomSupported()) return;

                mZoomValue = zoom;
                Camera.Parameters params = mCamera.getParameters();
                float max = params.getMaxZoom();
                params.setZoom((int) (zoom * max));
                mCamera.setParameters(params);

                if (notify) {
                    mCameraCallbacks.dispatchOnZoomChanged(zoom, points);
                }
            }
        });
    }

    @Override
    void setExposureCorrection(final float EVvalue, @NonNull final float[] bounds,
                               @Nullable final PointF[] points, final boolean notify) {
        schedule(mExposureCorrectionTask, true, new Runnable() {
            @Override
            public void run() {
                if (!mCameraOptions.isExposureCorrectionSupported()) return;

                float value = EVvalue;
                float max = mCameraOptions.getExposureCorrectionMaxValue();
                float min = mCameraOptions.getExposureCorrectionMinValue();
                value = value < min ? min : value > max ? max : value; // cap
                mExposureCorrectionValue = value;
                Camera.Parameters params = mCamera.getParameters();
                int indexValue = (int) (value / params.getExposureCompensationStep());
                params.setExposureCompensation(indexValue);
                mCamera.setParameters(params);

                if (notify) {
                    mCameraCallbacks.dispatchOnExposureCorrectionChanged(value, bounds, points);
                }
            }
        });
    }

    // -----------------
    // Tap to focus stuff.


    @Override
    void startAutoFocus(@Nullable final Gesture gesture, @NonNull final PointF point) {
        // Must get width and height from the UI thread.
        int viewWidth = 0, viewHeight = 0;
        if (mPreview != null && mPreview.hasSurface()) {
            viewWidth = mPreview.getView().getWidth();
            viewHeight = mPreview.getView().getHeight();
        }
        final int viewWidthF = viewWidth;
        final int viewHeightF = viewHeight;
        // Schedule.
        schedule(null, true, new Runnable() {
            @Override
            public void run() {
                if (!mCameraOptions.isAutoFocusSupported()) return;
                final PointF p = new PointF(point.x, point.y); // copy.
                List<Camera.Area> meteringAreas2 = computeMeteringAreas(p.x, p.y,
                        viewWidthF, viewHeightF, offset(REF_SENSOR, REF_VIEW));
                List<Camera.Area> meteringAreas1 = meteringAreas2.subList(0, 1);

                // At this point we are sure that camera supports auto focus... right? Look at CameraView.onTouchEvent().
                Camera.Parameters params = mCamera.getParameters();
                int maxAF = params.getMaxNumFocusAreas();
                int maxAE = params.getMaxNumMeteringAreas();
                if (maxAF > 0) params.setFocusAreas(maxAF > 1 ? meteringAreas2 : meteringAreas1);
                if (maxAE > 0) params.setMeteringAreas(maxAE > 1 ? meteringAreas2 : meteringAreas1);
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(params);
                mCameraCallbacks.dispatchOnFocusStart(gesture, p);
                // TODO this is not guaranteed to be called... Fix.
                try {
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            // TODO lock auto exposure and white balance for a while
                            mCameraCallbacks.dispatchOnFocusEnd(gesture, success, p);
                            mHandler.get().removeCallbacks(mPostFocusResetRunnable);
                            mHandler.get().postDelayed(mPostFocusResetRunnable, mPostFocusResetDelay);
                        }
                    });
                } catch (RuntimeException e) {
                    // Handling random auto-focus exception on some devices
                    // See https://github.com/natario1/CameraView/issues/181
                    LOG.e("startAutoFocus:", "Error calling autoFocus", e);
                    mCameraCallbacks.dispatchOnFocusEnd(gesture, false, p);
                }
            }
        });
    }

    @NonNull
    @WorkerThread
    private static List<Camera.Area> computeMeteringAreas(double viewClickX, double viewClickY,
                                                          int viewWidth, int viewHeight,
                                                          int sensorToDisplay) {
        // Event came in view coordinates. We must rotate to sensor coordinates.
        // First, rescale to the -1000 ... 1000 range.
        int displayToSensor = -sensorToDisplay;
        viewClickX = -1000d + (viewClickX / (double) viewWidth) * 2000d;
        viewClickY = -1000d + (viewClickY / (double) viewHeight) * 2000d;

        // Apply rotation to this point.
        // https://academo.org/demos/rotation-about-point/
        double theta = ((double) displayToSensor) * Math.PI / 180;
        double sensorClickX = viewClickX * Math.cos(theta) - viewClickY * Math.sin(theta);
        double sensorClickY = viewClickX * Math.sin(theta) + viewClickY * Math.cos(theta);
        LOG.i("focus:", "viewClickX:", viewClickX, "viewClickY:", viewClickY);
        LOG.i("focus:", "sensorClickX:", sensorClickX, "sensorClickY:", sensorClickY);

        // Compute the rect bounds.
        Rect rect1 = computeMeteringArea(sensorClickX, sensorClickY, 150d);
        int weight1 = 1000; // 150 * 150 * 1000 = more than 10.000.000
        Rect rect2 = computeMeteringArea(sensorClickX, sensorClickY, 300d);
        int weight2 = 100; // 300 * 300 * 100 = 9.000.000

        List<Camera.Area> list = new ArrayList<>(2);
        list.add(new Camera.Area(rect1, weight1));
        list.add(new Camera.Area(rect2, weight2));
        return list;
    }

    @NonNull
    private static Rect computeMeteringArea(double centerX, double centerY, double size) {
        double delta = size / 2d;
        int top = (int) Math.max(centerY - delta, -1000);
        int bottom = (int) Math.min(centerY + delta, 1000);
        int left = (int) Math.max(centerX - delta, -1000);
        int right = (int) Math.min(centerX + delta, 1000);
        LOG.i("focus:", "computeMeteringArea:", "top:", top, "left:", left, "bottom:", bottom, "right:", right);
        return new Rect(left, top, right, bottom);
    }


    // -----------------
    // Size stuff.


    @Nullable
    private List<Size> sizesFromList(@Nullable List<Camera.Size> sizes) {
        if (sizes == null) return null;
        List<Size> result = new ArrayList<>(sizes.size());
        for (Camera.Size size : sizes) {
            Size add = new Size(size.width, size.height);
            if (!result.contains(add)) result.add(add);
        }
        LOG.i("size:", "sizesFromList:", result);
        return result;
    }

    @Override
    void setPlaySounds(boolean playSounds) {
        final boolean old = mPlaySounds;
        mPlaySounds = playSounds;
        schedule(mPlaySoundsTask, true, new Runnable() {
            @Override
            public void run() {
                applyPlaySounds(old);
            }
        });
    }
}


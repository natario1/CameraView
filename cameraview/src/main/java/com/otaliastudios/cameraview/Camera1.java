package com.otaliastudios.cameraview;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("deprecation")
class Camera1 extends CameraController implements Camera.PreviewCallback, Camera.ErrorCallback {

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

    Camera1(CameraView.CameraCallbacks callback) {
        super(callback);
        mMapper = new Mapper.Mapper1();
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

    // Preview surface is now available. If camera is open, set up.
    @Override
    public void onSurfaceAvailable() {
        LOG.i("onSurfaceAvailable:", "Size is", mPreview.getSurfaceSize());
        schedule(null, false, new Runnable() {
            @Override
            public void run() {
                if (shouldBindToSurface()) {
                    LOG.i("onSurfaceAvailable:", "Inside handler. About to bind.");
                    try {
                        bindToSurface();
                    } catch (Exception e) {
                        LOG.e("onSurfaceAvailable:", "Exception while binding camera to preview.", e);
                        throw new CameraException(e);
                    }
                }
            }
        });
    }

    // Preview surface did change its size. Compute a new preview size.
    // This requires stopping and restarting the preview.
    @Override
    public void onSurfaceChanged() {
        LOG.i("onSurfaceChanged, size is", mPreview.getSurfaceSize());
        schedule(null, true, new Runnable() {
            @Override
            public void run() {
                if (!mIsBound) return;

                // Compute a new camera preview size.
                Size newSize = computePreviewSize(sizesFromList(mCamera.getParameters().getSupportedPreviewSizes()));
                if (newSize.equals(mPreviewSize)) return;

                // Apply.
                LOG.i("onSurfaceChanged:", "Computed a new preview size. Going on.");
                mPreviewSize = newSize;
                mCamera.stopPreview();
                applySizesAndStartPreview("onSurfaceChanged:");
            }
        });
    }

    private boolean shouldBindToSurface() {
        return isCameraAvailable() && mPreview != null && mPreview.isReady() && !mIsBound;
    }

    // The act of binding an "open" camera to a "ready" preview.
    // These can happen at different times but we want to end up here.
    @WorkerThread
    private void bindToSurface() {
        LOG.i("bindToSurface:", "Started");
        Object output = mPreview.getOutput();
        try {
            if (mPreview.getOutputClass() == SurfaceHolder.class) {
                mCamera.setPreviewDisplay((SurfaceHolder) output);
            } else {
                mCamera.setPreviewTexture((SurfaceTexture) output);
            }
        } catch (IOException e) {
            throw new CameraException(e);
        }

        mPictureSize = computePictureSize();
        mPreviewSize = computePreviewSize(sizesFromList(mCamera.getParameters().getSupportedPreviewSizes()));
        applySizesAndStartPreview("bindToSurface:");
        mIsBound = true;
    }

    // To be called when the preview size is setup or changed.
    private void applySizesAndStartPreview(String log) {
        LOG.i(log, "Dispatching onCameraPreviewSizeChanged.");
        mCameraCallbacks.onCameraPreviewSizeChanged();

        boolean invertPreviewSizes = shouldFlipSizes();
        mPreview.setDesiredSize(
                invertPreviewSizes ? mPreviewSize.getHeight() : mPreviewSize.getWidth(),
                invertPreviewSizes ? mPreviewSize.getWidth() : mPreviewSize.getHeight()
        );

        Camera.Parameters params = mCamera.getParameters();
        mPreviewFormat = params.getPreviewFormat();
        params.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight()); // <- not allowed during preview
        params.setPictureSize(mPictureSize.getWidth(), mPictureSize.getHeight()); // <- allowed
        mCamera.setParameters(params);

        mCamera.setPreviewCallbackWithBuffer(null); // Release anything left
        mCamera.setPreviewCallbackWithBuffer(this); // Add ourselves
        mFrameManager.allocate(ImageFormat.getBitsPerPixel(mPreviewFormat), mPreviewSize);

        LOG.i(log, "Starting preview with startPreview().");
        mCamera.startPreview();
        LOG.i(log, "Started preview.");
    }

    @WorkerThread
    @Override
    void onStart() {
        if (isCameraAvailable()) {
            LOG.w("onStart:", "Camera not available. Should not happen.");
            onStop(); // Should not happen.
        }
        if (collectCameraId()) {
            mCamera = Camera.open(mCameraId);
            mCamera.setErrorCallback(this);

            // Set parameters that might have been set before the camera was opened.
            LOG.i("onStart:", "Applying default parameters.");
            Camera.Parameters params = mCamera.getParameters();
            mExtraProperties = new ExtraProperties(params);
            mCameraOptions = new CameraOptions(params, shouldFlipSizes());
            applyDefaultFocus(params);
            mergeFlash(params, Flash.DEFAULT);
            mergeLocation(params, null);
            mergeWhiteBalance(params, WhiteBalance.DEFAULT);
            mergeHdr(params, Hdr.DEFAULT);
            mergePlaySound(mPlaySounds);
            params.setRecordingHint(mSessionType == SessionType.VIDEO);
            mCamera.setParameters(params);

            // Try starting preview.
            mCamera.setDisplayOrientation(computeSensorToViewOffset()); // <- not allowed during preview
            if (shouldBindToSurface()) bindToSurface();
            LOG.i("onStart:", "Ended");
        }
    }

    @WorkerThread
    @Override
    void onStop() {
        Exception error = null;
        LOG.i("onStop:", "About to clean up.");
        mHandler.get().removeCallbacks(mPostFocusResetRunnable);
        mFrameManager.release();

        if (mCamera != null) {
            LOG.i("onStop:", "Clean up.", "Ending video.");
            endVideoImmediately();

            try {
                LOG.i("onStop:", "Clean up.", "Stopping preview.");
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.stopPreview();
                LOG.i("onStop:", "Clean up.", "Stopped preview.");
            } catch (Exception e) {
                LOG.w("onStop:", "Clean up.", "Exception while stopping preview.", e);
                error = e;
            }

            try {
                LOG.i("onStop:", "Clean up.", "Releasing camera.");
                mCamera.release();
                LOG.i("onStop:", "Clean up.", "Released camera.");
            } catch (Exception e) {
                LOG.w("onStop:", "Clean up.", "Exception while releasing camera.", e);
                error = e;
            }
        }
        mExtraProperties = null;
        mCameraOptions = null;
        mCamera = null;
        mPreviewSize = null;
        mPictureSize = null;
        mIsBound = false;
        mIsCapturingImage = false;
        mIsCapturingVideo = false;
        LOG.w("onStop:", "Clean up.", "Returning.");
        if (error != null) throw new CameraException(error);
    }

    private boolean collectCameraId() {
        int internalFacing = mMapper.map(mFacing);
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
    public void onBufferAvailable(byte[] buffer) {
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
        throw new CameraException(new RuntimeException(CameraLogger.lastMessage));
    }

    @Override
    void setSessionType(SessionType sessionType) {
        if (sessionType != mSessionType) {
            mSessionType = sessionType;
            schedule(null, true, new Runnable() {
                @Override
                public void run() {
                    restart();
                }
            });
        }
    }

    @Override
    void setLocation(Location location) {
        final Location oldLocation = mLocation;
        mLocation = location;
        schedule(mLocationTask, true, new Runnable() {
            @Override
            public void run() {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeLocation(params, oldLocation)) mCamera.setParameters(params);
            }
        });
    }

    private boolean mergeLocation(Camera.Parameters params, Location oldLocation) {
        if (mLocation != null) {
            params.setGpsLatitude(mLocation.getLatitude());
            params.setGpsLongitude(mLocation.getLongitude());
            params.setGpsAltitude(mLocation.getAltitude());
            params.setGpsTimestamp(mLocation.getTime());
            params.setGpsProcessingMethod(mLocation.getProvider());

            if (mIsCapturingVideo && mMediaRecorder != null) {
                mMediaRecorder.setLocation((float) mLocation.getLatitude(),
                        (float) mLocation.getLongitude());
            }
        }
        return true;
    }

    @Override
    void setFacing(Facing facing) {
        if (facing != mFacing) {
            mFacing = facing;
            schedule(null, true, new Runnable() {
                @Override
                public void run() {
                    if (collectCameraId()) {
                        restart();
                    }
                }
            });
        }
    }

    @Override
    void setWhiteBalance(WhiteBalance whiteBalance) {
        final WhiteBalance old = mWhiteBalance;
        mWhiteBalance = whiteBalance;
        schedule(mWhiteBalanceTask, true, new Runnable() {
            @Override
            public void run() {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeWhiteBalance(params, old)) mCamera.setParameters(params);
            }
        });
    }

    private boolean mergeWhiteBalance(Camera.Parameters params, WhiteBalance oldWhiteBalance) {
        if (mCameraOptions.supports(mWhiteBalance)) {
            params.setWhiteBalance((String) mMapper.map(mWhiteBalance));
            return true;
        }
        mWhiteBalance = oldWhiteBalance;
        return false;
    }

    @Override
    void setHdr(Hdr hdr) {
        final Hdr old = mHdr;
        mHdr = hdr;
        schedule(mHdrTask, true, new Runnable() {
            @Override
            public void run() {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeHdr(params, old)) mCamera.setParameters(params);
            }
        });
    }

    private boolean mergeHdr(Camera.Parameters params, Hdr oldHdr) {
        if (mCameraOptions.supports(mHdr)) {
            params.setSceneMode((String) mMapper.map(mHdr));
            return true;
        }
        mHdr = oldHdr;
        return false;
    }

    @TargetApi(17)
    private boolean mergePlaySound(boolean oldPlaySound) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, info);
            if (info.canDisableShutterSound) {
                mCamera.enableShutterSound(mPlaySounds);
                return true;
            }
        }
        if (mPlaySounds) {
            return true;
        }
        mPlaySounds = oldPlaySound;
        return false;
    }


    @Override
    void setAudio(Audio audio) {
        if (mAudio != audio) {
            if (mIsCapturingVideo) {
                LOG.w("Audio setting was changed while recording. " +
                        "Changes will take place starting from next video");
            }
            mAudio = audio;
        }
    }

    @Override
    void setFlash(Flash flash) {
        final Flash old = mFlash;
        mFlash = flash;
        schedule(mFlashTask, true, new Runnable() {
            @Override
            public void run() {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeFlash(params, old)) mCamera.setParameters(params);
            }
        });
    }


    private boolean mergeFlash(Camera.Parameters params, Flash oldFlash) {
        if (mCameraOptions.supports(mFlash)) {
            params.setFlashMode((String) mMapper.map(mFlash));
            return true;
        }
        mFlash = oldFlash;
        return false;
    }


    // Choose the best default focus, based on session type.
    private void applyDefaultFocus(Camera.Parameters params) {
        List<String> modes = params.getSupportedFocusModes();

        if (mSessionType == SessionType.VIDEO &&
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
            return;
        }
    }


    @Override
    void setVideoQuality(VideoQuality videoQuality) {
        final VideoQuality old = mVideoQuality;
        mVideoQuality = videoQuality;
        schedule(mVideoQualityTask, true, new Runnable() {
            @Override
            public void run() {
                if (mIsCapturingVideo) {
                    // TODO: actually any call to getParameters() could fail while recording a video.
                    // See. https://stackoverflow.com/questions/14941625/
                    mVideoQuality = old;
                    throw new IllegalStateException("Can't change video quality while recording a video.");
                }

                if (mSessionType == SessionType.VIDEO) {
                    // Change capture size to a size that fits the video aspect ratio.
                    Size oldSize = mPictureSize;
                    mPictureSize = computePictureSize();
                    if (!mPictureSize.equals(oldSize)) {
                        // New video quality triggers a new aspect ratio.
                        // Go on and see if preview size should change also.
                        Camera.Parameters params = mCamera.getParameters();
                        params.setPictureSize(mPictureSize.getWidth(), mPictureSize.getHeight());
                        mCamera.setParameters(params);
                        onSurfaceChanged();
                    }
                    LOG.i("setVideoQuality:", "captureSize:", mPictureSize);
                    LOG.i("setVideoQuality:", "previewSize:", mPreviewSize);
                }
            }
        });
    }

    @Override
    void capturePicture() {
        LOG.v("capturePicture: scheduling");
        schedule(null, true, new Runnable() {
            @Override
            public void run() {
                LOG.v("capturePicture: performing.", mIsCapturingImage);
                if (mIsCapturingImage) return;
                if (mIsCapturingVideo && !mCameraOptions.isVideoSnapshotSupported()) return;

                mIsCapturingImage = true;
                final int sensorToOutput = computeSensorToOutputOffset();
                final int sensorToView = computeSensorToViewOffset();
                final boolean outputMatchesView = (sensorToOutput + sensorToView + 180) % 180 == 0;
                final boolean outputFlip = mFacing == Facing.FRONT;
                Camera.Parameters params = mCamera.getParameters();
                params.setRotation(sensorToOutput);
                mCamera.setParameters(params);
                mCamera.takePicture(
                        new Camera.ShutterCallback() {
                            @Override
                            public void onShutter() {
                                mCameraCallbacks.onShutter(false);
                            }
                        },
                        null,
                        null,
                        new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, final Camera camera) {
                                mIsCapturingImage = false;
                                mCameraCallbacks.processImage(data, outputMatchesView, outputFlip);
                                camera.startPreview(); // This is needed, read somewhere in the docs.
                            }
                        }
                );
            }
        });
    }


    @Override
    void captureSnapshot() {
        LOG.v("captureSnapshot: scheduling");
        schedule(null, true, new Runnable() {
            @Override
            public void run() {
                LOG.v("captureSnapshot: performing.", mIsCapturingImage);
                if (mIsCapturingImage) return;
                // This won't work while capturing a video.
                // Switch to capturePicture.
                if (mIsCapturingVideo) {
                    capturePicture();
                    return;
                }
                mIsCapturingImage = true;
                mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(final byte[] data, Camera camera) {
                        mCameraCallbacks.onShutter(true);

                        // Got to rotate the preview frame, since byte[] data here does not include
                        // EXIF tags automatically set by camera. So either we add EXIF, or we rotate.
                        // Adding EXIF to a byte array, unfortunately, is hard.
                        final int sensorToOutput = computeSensorToOutputOffset();
                        final int sensorToView = computeSensorToViewOffset();
                        final boolean outputMatchesView = (sensorToOutput + sensorToView + 180) % 180 == 0;
                        final boolean outputFlip = mFacing == Facing.FRONT;
                        final boolean flip = sensorToOutput % 180 != 0;
                        final int preWidth = mPreviewSize.getWidth();
                        final int preHeight = mPreviewSize.getHeight();
                        final int postWidth = flip ? preHeight : preWidth;
                        final int postHeight = flip ? preWidth : preHeight;
                        final int format = mPreviewFormat;
                        WorkerHandler.run(new Runnable() {
                            @Override
                            public void run() {

                                LOG.v("captureSnapshot: rotating.");
                                byte[] rotatedData = RotationHelper.rotate(data, preWidth, preHeight, sensorToOutput);
                                LOG.v("captureSnapshot: rotated.");
                                YuvImage yuv = new YuvImage(rotatedData, format, postWidth, postHeight, null);
                                mCameraCallbacks.processSnapshot(yuv, outputMatchesView, outputFlip);
                                mIsCapturingImage = false;
                            }
                        });

                        // It seems that the buffers are already cleared here, so we need to allocate again.
                        mCamera.setPreviewCallbackWithBuffer(null); // Release anything left
                        mCamera.setPreviewCallbackWithBuffer(Camera1.this); // Add ourselves
                        mFrameManager.allocate(ImageFormat.getBitsPerPixel(mPreviewFormat), mPreviewSize);
                    }
                });
            }
        });
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Frame frame = mFrameManager.getFrame(data,
                System.currentTimeMillis(),
                computeSensorToOutputOffset(),
                mPreviewSize,
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
    void startVideo(@NonNull final File videoFile) {
        schedule(mStartVideoTask, true, new Runnable() {
            @Override
            public void run() {
                if (mIsCapturingVideo) return;
                if (mSessionType == SessionType.VIDEO) {
                    mVideoFile = videoFile;
                    mIsCapturingVideo = true;
                    initMediaRecorder();
                    try {
                        mMediaRecorder.prepare();
                        mMediaRecorder.start();
                    } catch (Exception e) {
                        LOG.e("Error while starting MediaRecorder. Swallowing.", e);
                        mVideoFile = null;
                        mCamera.lock();
                        endVideoImmediately();
                    }
                } else {
                    throw new IllegalStateException("Can't record video while session type is picture");
                }
            }
        });
    }

    @Override
    void endVideo() {
        schedule(null, false, new Runnable() {
            @Override
            public void run() {
                endVideoImmediately();
            }
        });
    }

    @WorkerThread
    private void endVideoImmediately() {
        LOG.i("endVideoImmediately:", "is capturing:", mIsCapturingVideo);
        mIsCapturingVideo = false;
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                // This can happen if endVideo() is called right after startVideo(). We don't care.
                LOG.w("endVideoImmediately:", "Error while closing media recorder. Swallowing", e);
            }
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mVideoFile != null) {
            mCameraCallbacks.dispatchOnVideoTaken(mVideoFile);
            mVideoFile = null;
        }
    }

    @WorkerThread
    private void initMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if (mAudio == Audio.ON) {
            // Must be called before setOutputFormat.
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }
        CamcorderProfile profile = getCamcorderProfile();
        mMediaRecorder.setOutputFormat(profile.fileFormat);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncoder(mMapper.map(mVideoCodec));
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        if (mAudio == Audio.ON) {
            mMediaRecorder.setAudioChannels(profile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(profile.audioCodec);
            mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        }

        if (mLocation != null) {
            mMediaRecorder.setLocation(
                    (float) mLocation.getLatitude(),
                    (float) mLocation.getLongitude());
        }

        mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());
        mMediaRecorder.setOrientationHint(computeSensorToOutputOffset());

        mMediaRecorder.setMaxFileSize(mVideoMaxSize);
        mMediaRecorder.setMaxDuration(mVideoMaxDuration);

        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        endVideoImmediately();
                        break;
                }
            }
        });
        // Not needed. mMediaRecorder.setPreviewDisplay(mPreview.getSurface());
    }

    // -----------------
    // Zoom and simpler stuff.


    @Override
    void setZoom(final float zoom, final PointF[] points, final boolean notify) {
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
    void setExposureCorrection(final float EVvalue, final float[] bounds,
                               final PointF[] points, final boolean notify) {
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
    void startAutoFocus(@Nullable final Gesture gesture, final PointF point) {
        // Must get width and height from the UI thread.
        int viewWidth = 0, viewHeight = 0;
        if (mPreview != null && mPreview.isReady()) {
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
                        viewWidthF, viewHeightF, computeSensorToViewOffset());
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
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        // TODO lock auto exposure and white balance for a while
                        mCameraCallbacks.dispatchOnFocusEnd(gesture, success, p);
                        mHandler.get().removeCallbacks(mPostFocusResetRunnable);
                        mHandler.get().postDelayed(mPostFocusResetRunnable, mPostFocusResetDelay);
                    }
                });
            }
        });
    }


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
    private List<Size> sizesFromList(List<Camera.Size> sizes) {
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
                mergePlaySound(old);
            }
        });
    }

    // -----------------
    // Additional helper info
}


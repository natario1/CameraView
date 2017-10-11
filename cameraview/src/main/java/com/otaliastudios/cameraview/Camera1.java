package com.otaliastudios.cameraview;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@SuppressWarnings("deprecation")
class Camera1 extends CameraController {

    private static final String TAG = Camera1.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private int mCameraId;
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private File mVideoFile;

    private int mSensorOffset;

    private final int mPostFocusResetDelay = 3000;
    private Runnable mPostFocusResetRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCameraAvailable()) return;
            mCamera.cancelAutoFocus();
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                params.setFocusAreas(null);
                params.setMeteringAreas(null);
                applyDefaultFocus(params); // Revert to internal focus.
                mCamera.setParameters(params);
            }
        }
    };

    private Mapper mMapper = new Mapper.Mapper1();
    private boolean mIsSetup = false;
    private boolean mIsCapturingImage = false;
    private boolean mIsCapturingVideo = false;
    private final Object mLock = new Object();


    Camera1(CameraView.CameraCallbacks callback) {
        super(callback);
    }

    /**
     * Preview surface is now available. If camera is open, set up.
     */
    @Override
    public void onSurfaceAvailable() {
        LOG.i("onSurfaceAvailable:", "Size is", mPreview.getSurfaceSize());
        if (!shouldSetup()) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!shouldSetup()) return;
                LOG.i("onSurfaceAvailable:", "Inside handler. About to bind.");
                try {
                    setup();
                } catch (Exception e) {
                    LOG.w("onSurfaceAvailable:", "Exception while binding camera to preview.", e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Preview surface did change its size. Compute a new preview size.
     * This requires stopping and restarting the preview.
     */
    @Override
    public void onSurfaceChanged() {
        LOG.i("onSurfaceChanged, size is", mPreview.getSurfaceSize());
        if (mIsSetup) {
            // Compute a new camera preview size.
            Size newSize = computePreviewSize();
            if (!newSize.equals(mPreviewSize)) {
                LOG.i("onSurfaceChanged:", "Computed a new preview size. Dispatching.");
                mPreviewSize = newSize;
                mCameraCallbacks.onCameraPreviewSizeChanged();
                synchronized (mLock) {
                    LOG.i("onSurfaceChanged:", "Stopping preview.");
                    mCamera.stopPreview();
                    LOG.i("onSurfaceChanged:", "Stopped preview.");
                    Camera.Parameters params = mCamera.getParameters();
                    params.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                    mCamera.setParameters(params);
                }
                boolean invertPreviewSizes = shouldFlipSizes();
                mPreview.setDesiredSize(
                        invertPreviewSizes ? mPreviewSize.getHeight() : mPreviewSize.getWidth(),
                        invertPreviewSizes ? mPreviewSize.getWidth() : mPreviewSize.getHeight()
                );
                LOG.i("onSurfaceChanged:", "Restarting preview.");
                mCamera.startPreview();
                LOG.i("onSurfaceChanged:", "Restarted preview.");
            }
        }
    }

    private boolean shouldSetup() {
        return isCameraAvailable() && mPreview != null && mPreview.isReady() && !mIsSetup;
    }

    // The act of binding an "open" camera to a "ready" preview.
    // These can happen at different times but we want to end up here.
    @WorkerThread
    private void setup() throws Exception {
        LOG.i("setup:", "Started");
        Object output = mPreview.getOutput();
        if (mPreview.getOutputClass() == SurfaceHolder.class) {
            mCamera.setPreviewDisplay((SurfaceHolder) output);
        } else {
            mCamera.setPreviewTexture((SurfaceTexture) output);
        }

        boolean invertPreviewSizes = shouldFlipSizes();
        mCaptureSize = computeCaptureSize();
        mPreviewSize = computePreviewSize();
        LOG.i("setup:", "Dispatching onCameraPreviewSizeChanged.");
        mCameraCallbacks.onCameraPreviewSizeChanged();
        mPreview.setDesiredSize(
                invertPreviewSizes ? mPreviewSize.getHeight() : mPreviewSize.getWidth(),
                invertPreviewSizes ? mPreviewSize.getWidth() : mPreviewSize.getHeight()
        );
        synchronized (mLock) {
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight()); // <- not allowed during preview
            params.setPictureSize(mCaptureSize.getWidth(), mCaptureSize.getHeight()); // <- allowed
            mCamera.setParameters(params);
        }
        LOG.i("setup:", "Starting preview with startPreview().");
        mCamera.startPreview();
        LOG.i("setup:", "Started preview with startPreview().");
        mIsSetup = true;
    }


    @WorkerThread
    @Override
    void onStart() throws Exception {
        if (isCameraAvailable()) {
            LOG.w("onStart:", "Camera not available. Should not happen.");
            onStop(); // Should not happen.
        }
        if (collectCameraId()) {
            mCamera = Camera.open(mCameraId);

            // Set parameters that might have been set before the camera was opened.
            synchronized (mLock) {
                LOG.i("onStart:", "Applying default parameters.");
                Camera.Parameters params = mCamera.getParameters();
                mExtraProperties = new ExtraProperties(params);
                mOptions = new CameraOptions(params);
                applyDefaultFocus(params);
                mergeFlash(params, Flash.DEFAULT);
                mergeLocation(params, null);
                mergeWhiteBalance(params, WhiteBalance.DEFAULT);
                params.setRecordingHint(mSessionType == SessionType.VIDEO);
                mCamera.setParameters(params);
            }

            // Try starting preview.
            mCamera.setDisplayOrientation(computeSensorToDisplayOffset()); // <- not allowed during preview
            if (shouldSetup()) setup();
            LOG.i("onStart:", "Ended");
        }
    }

    @WorkerThread
    @Override
    void onStop() throws Exception {
        Exception error = null;
        LOG.i("onStop:", "About to clean up.");
        mHandler.get().removeCallbacks(mPostFocusResetRunnable);
        if (mCamera != null) {
            LOG.i("onStop:", "Clean up.", "Ending video?", mIsCapturingVideo);
            if (mIsCapturingVideo) endVideo();

            try {
                LOG.i("onStop:", "Clean up.", "Stopping preview.");
                mCamera.stopPreview();
                LOG.i("onStop:", "Clean up.", "Stopped preview.");
            } catch (Exception e) {
                LOG.w("onStop:", "Clean up.", "Exception while stopping preview.");
                error = e;
            }

            try {
                LOG.i("onStop:", "Clean up.", "Releasing camera.");
                mCamera.release();
                LOG.i("onStop:", "Clean up.", "Released camera.");
            } catch (Exception e) {
                LOG.w("onStop:", "Clean up.", "Exception while releasing camera.");
                error = e;
            }
        }
        mExtraProperties = null;
        mOptions = null;
        mCamera = null;
        mPreviewSize = null;
        mCaptureSize = null;
        mIsSetup = false;

        if (error != null) throw error;
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
    void setSessionType(SessionType sessionType) {
        if (sessionType != mSessionType) {
            mSessionType = sessionType;
            if (isCameraAvailable()) {
                restart();
            }
        }
    }

    @Override
    void setLocation(Location location) {
        Location oldLocation = mLocation;
        mLocation = location;
        if (isCameraAvailable()) {
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeLocation(params, oldLocation)) mCamera.setParameters(params);
            }
        }
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
            if (collectCameraId() && isCameraAvailable()) {
                restart();
            }
        }
    }

    @Override
    void setWhiteBalance(WhiteBalance whiteBalance) {
        WhiteBalance old = mWhiteBalance;
        mWhiteBalance = whiteBalance;
        if (isCameraAvailable()) {
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeWhiteBalance(params, old)) mCamera.setParameters(params);
            }
        }
    }

    private boolean mergeWhiteBalance(Camera.Parameters params, WhiteBalance oldWhiteBalance) {
        if (mOptions.supports(mWhiteBalance)) {
            params.setWhiteBalance((String) mMapper.map(mWhiteBalance));
            return true;
        }
        mWhiteBalance = oldWhiteBalance;
        return false;
    }

    @Override
    void setHdr(Hdr hdr) {
        Hdr old = mHdr;
        mHdr = hdr;
        if (isCameraAvailable()) {
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeHdr(params, old)) mCamera.setParameters(params);
            }
        }
    }

    private boolean mergeHdr(Camera.Parameters params, Hdr oldHdr) {
        if (mOptions.supports(mHdr)) {
            params.setSceneMode((String) mMapper.map(mHdr));
            return true;
        }
        mHdr = oldHdr;
        return false;
    }


    @Override
    void setAudio(Audio audio) {
        if (mAudio != audio) {
            if (mIsCapturingVideo) {
                LOG.w("Changing audio mode while recording. Changes will take place starting from next video");
            }
            mAudio = audio;
        }
    }

    @Override
    void setFlash(Flash flash) {
        Flash old = mFlash;
        mFlash = flash;
        if (isCameraAvailable()) {
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeFlash(params, old)) mCamera.setParameters(params);
            }
        }
    }


    private boolean mergeFlash(Camera.Parameters params, Flash oldFlash) {
        if (mOptions.supports(mFlash)) {
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
        if (mIsCapturingVideo) {
            // TODO: actually any call to getParameters() could fail while recording a video.
            // See. https://stackoverflow.com/questions/14941625/correct-handling-of-exception-getparameters-failed-empty-parameters
            throw new IllegalStateException("Can't change video quality while recording a video.");
        }

        mVideoQuality = videoQuality;
        if (isCameraAvailable() && mSessionType == SessionType.VIDEO) {
            // Change capture size to a size that fits the video aspect ratio.
            Size oldSize = mCaptureSize;
            mCaptureSize = computeCaptureSize();
            if (!mCaptureSize.equals(oldSize)) {
                // New video quality triggers a new aspect ratio.
                // Go on and see if preview size should change also.
                synchronized (mLock) {
                    Camera.Parameters params = mCamera.getParameters();
                    params.setPictureSize(mCaptureSize.getWidth(), mCaptureSize.getHeight());
                    mCamera.setParameters(params);
                }
                onSurfaceChanged();
            }
            LOG.i("setVideoQuality:", "captureSize:", mCaptureSize);
            LOG.i("setVideoQuality:", "previewSize:", mPreviewSize);
        }
    }

    @Override
    boolean capturePicture() {
        if (mIsCapturingImage) return false;
        if (!isCameraAvailable()) return false;
        if (mSessionType == SessionType.VIDEO && mIsCapturingVideo) {
            if (!mOptions.isVideoSnapshotSupported()) return false;
        }

        // Set boolean to wait for image callback
        mIsCapturingImage = true;
        final int exifRotation = computeExifRotation();
        final boolean exifFlip = computeExifFlip();
        final int sensorToDisplay = computeSensorToDisplayOffset();
        synchronized (mLock) {
            Camera.Parameters params = mCamera.getParameters();
            params.setRotation(exifRotation);
            mCamera.setParameters(params);
        }
        // Is the final picture (decoded respecting EXIF) consistent with CameraView orientation?
        // We must consider exifOrientation to bring back the picture in the sensor world.
        // Then use sensorToDisplay to move to the display world, where CameraView lives.
        final boolean consistentWithView = (exifRotation + sensorToDisplay + 180) % 180 == 0;
        mCamera.takePicture(null, null, null,
                new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, final Camera camera) {
                        mIsCapturingImage = false;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // This is needed, read somewhere in the docs.
                                camera.startPreview();
                            }
                        });
                        mCameraCallbacks.processImage(data, consistentWithView, exifFlip);
                    }
                });
        return true;
    }


    @Override
    boolean captureSnapshot() {
        if (!isCameraAvailable()) return false;
        if (mIsCapturingImage) return false;
        // This won't work while capturing a video.
        // Switch to capturePicture.
        if (mIsCapturingVideo) {
            capturePicture();
            return false;
        }
        mIsCapturingImage = true;
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] data, Camera camera) {
                // Got to rotate the preview frame, since byte[] data here does not include
                // EXIF tags automatically set by camera. So either we add EXIF, or we rotate.
                // Adding EXIF to a byte array, unfortunately, is hard.
                Camera.Parameters params = mCamera.getParameters();
                final int sensorToDevice = computeExifRotation();
                final int sensorToDisplay = computeSensorToDisplayOffset();
                final boolean exifFlip = computeExifFlip();
                final boolean flip = sensorToDevice % 180 != 0;
                final int preWidth = mPreviewSize.getWidth();
                final int preHeight = mPreviewSize.getHeight();
                final int postWidth = flip ? preHeight : preWidth;
                final int postHeight = flip ? preWidth : preHeight;
                final int format = params.getPreviewFormat();
                WorkerHandler.run(new Runnable() {
                    @Override
                    public void run() {

                        final boolean consistentWithView = (sensorToDevice + sensorToDisplay + 180) % 180 == 0;
                        byte[] rotatedData = RotationHelper.rotate(data, preWidth, preHeight, sensorToDevice);
                        YuvImage yuv = new YuvImage(rotatedData, format, postWidth, postHeight, null);
                        mCameraCallbacks.processSnapshot(yuv, consistentWithView, exifFlip);
                        mIsCapturingImage = false;
                    }
                });
            }
        });
        return true;
    }

    @Override
    boolean shouldFlipSizes() {
        int offset = computeSensorToDisplayOffset();
        LOG.i("shouldFlipSizes:", "mDeviceOrientation=", mDeviceOrientation, "mSensorOffset=", mSensorOffset);
        LOG.i("shouldFlipSizes:", "sensorToDisplay=", offset);
        return offset % 180 != 0;
    }

    private boolean isCameraAvailable() {
        switch (mState) {
            // If we are stopped, don't.
            case STATE_STOPPED: return false;
            // If we are going to be closed, don't act on camera.
            // Even if mCamera != null, it might have been released.
            case STATE_STOPPING: return false;
            // If we are started, act as long as there is no stop/restart scheduled.
            // At this point mCamera should never be null.
            case STATE_STARTED: return !mScheduledForStop && !mScheduledForRestart;
            // If we are starting, theoretically we could act.
            // Just check that camera is available.
            case STATE_STARTING: return mCamera != null && !mScheduledForStop && !mScheduledForRestart;
        }
        return false;
    }


    // Internal:


    /**
     * Returns how much should the sensor image be rotated before being shown.
     * It is meant to be fed to Camera.setDisplayOrientation().
     */
    private int computeSensorToDisplayOffset() {
        if (mFacing == Facing.FRONT) {
            // or: (360 - ((mSensorOffset + mDisplayOffset) % 360)) % 360;
            return ((mSensorOffset - mDisplayOffset) + 360 + 180) % 360;
        } else {
            return (mSensorOffset - mDisplayOffset + 360) % 360;
        }
    }

    /**
     * Returns the orientation to be set as a exif tag. This is already managed by
     * the camera APIs as long as you call {@link Camera.Parameters#setRotation(int)}.
     * This ignores flipping for facing camera.
     */
    private int computeExifRotation() {
        if (mFacing == Facing.FRONT) {
            return (mSensorOffset - mDeviceOrientation + 360) % 360;
        } else {
            return (mSensorOffset + mDeviceOrientation) % 360;
        }
    }


    /**
     * Whether the exif tag should include a 'flip' operation.
     */
    private boolean computeExifFlip() {
        return mFacing == Facing.FRONT;
    }


    /**
     * This is called either on cameraView.start(), or when the underlying surface changes.
     * It is possible that in the first call the preview surface has not already computed its
     * dimensions.
     * But when it does, the {@link CameraPreview.SurfaceCallback} should be called,
     * and this should be refreshed.
     */
    private Size computeCaptureSize() {
        Camera.Parameters params = mCamera.getParameters();
        if (mSessionType == SessionType.PICTURE) {
            // Choose the max size.
            List<Size> captureSizes = sizesFromList(params.getSupportedPictureSizes());
            Size maxSize = Collections.max(captureSizes);
            LOG.i("size:", "computeCaptureSize:", "computed", maxSize);
            return Collections.max(captureSizes);
        } else {
            // Choose according to developer choice in setVideoQuality.
            // The Camcorder internally checks for cameraParameters.getSupportedVideoSizes() etc.
            // We want the picture size to be the max picture consistent with the video aspect ratio.
            List<Size> captureSizes = sizesFromList(params.getSupportedPictureSizes());
            CamcorderProfile profile = getCamcorderProfile(mVideoQuality);
            AspectRatio targetRatio = AspectRatio.of(profile.videoFrameWidth, profile.videoFrameHeight);
            LOG.i("size:", "computeCaptureSize:", "videoQuality:", mVideoQuality, "targetRatio:", targetRatio);
            return matchSize(captureSizes, targetRatio, new Size(0, 0), true);
        }
    }

    private Size computePreviewSize() {
        Camera.Parameters params = mCamera.getParameters();
        List<Size> previewSizes = sizesFromList(params.getSupportedPreviewSizes());
        AspectRatio targetRatio = AspectRatio.of(mCaptureSize.getWidth(), mCaptureSize.getHeight());
        Size biggerThan = mPreview.getSurfaceSize();
        LOG.i("size:", "computePreviewSize:", "targetRatio:", targetRatio, "surface size:", biggerThan);
        return matchSize(previewSizes, targetRatio, biggerThan, false);
    }


    // -----------------
    // Video recording stuff.


    @Override
    boolean startVideo(@NonNull File videoFile) {
        if (mIsCapturingVideo) return false;
        if (!isCameraAvailable()) return false;
        if (mSessionType == SessionType.VIDEO) {
            mVideoFile = videoFile;
            mIsCapturingVideo = true;
            initMediaRecorder();
            try {
                mMediaRecorder.prepare();
                mMediaRecorder.start();
                return true;
            } catch (Exception e) {
                LOG.e("Error while starting MediaRecorder. Swallowing.", e);
                mVideoFile = null;
                mCamera.lock();
                endVideo();
                return false;
            }
        } else {
            throw new IllegalStateException("Can't record video while session type is picture");
        }
    }

    @Override
    boolean endVideo() {
        if (mIsCapturingVideo) {
            mIsCapturingVideo = false;
            if (mMediaRecorder != null) {
                try {
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                } catch (Exception e) {
                    // This can happen if endVideo() is called right after startVideo().
                    // We don't care.
                    LOG.w("Error while closing media recorder. Swallowing", e);
                }
                mMediaRecorder = null;
            }
            if (mVideoFile != null) {
                mCameraCallbacks.dispatchOnVideoTaken(mVideoFile);
                mVideoFile = null;
            }
            return true;
        }
        return false;
    }


    private void initMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        CamcorderProfile profile = getCamcorderProfile(mVideoQuality);
        if (mAudio == Audio.ON) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setProfile(profile);
        } else {
            // Set all values contained in profile except audio settings
            mMediaRecorder.setOutputFormat(profile.fileFormat);
            mMediaRecorder.setVideoEncoder(profile.videoCodec);
            mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
            mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
            mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        }

        if (mLocation != null) {
            mMediaRecorder.setLocation((float) mLocation.getLatitude(),
                    (float) mLocation.getLongitude());
        }

        mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());
        mMediaRecorder.setOrientationHint(computeExifRotation());
        // Not needed. mMediaRecorder.setPreviewDisplay(mPreview.getSurface());
    }


    @NonNull
    private CamcorderProfile getCamcorderProfile(VideoQuality videoQuality) {
        switch (videoQuality) {
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

    // -----------------
    // Zoom and simpler stuff.


    @Override
    boolean setZoom(float zoom) {
        if (!isCameraAvailable()) return false;
        if (!mOptions.isZoomSupported()) return false;
        synchronized (mLock) {
            Camera.Parameters params = mCamera.getParameters();
            float max = params.getMaxZoom();
            params.setZoom((int) (zoom * max));
            mCamera.setParameters(params);
        }
        return true;
    }


    @Override
    boolean setExposureCorrection(float EVvalue) {
        if (!isCameraAvailable()) return false;
        if (!mOptions.isExposureCorrectionSupported()) return false;
        float max = mOptions.getExposureCorrectionMaxValue();
        float min = mOptions.getExposureCorrectionMinValue();
        EVvalue = EVvalue < min ? min : EVvalue > max ? max : EVvalue; // cap
        synchronized (mLock) {
            Camera.Parameters params = mCamera.getParameters();
            int indexValue = (int) (EVvalue / params.getExposureCompensationStep());
            params.setExposureCompensation(indexValue);
            mCamera.setParameters(params);
        }
        return true;
    }

    // -----------------
    // Tap to focus stuff.


    @Override
    boolean startAutoFocus(@Nullable final Gesture gesture, PointF point) {
        if (!isCameraAvailable()) return false;
        if (!mOptions.isAutoFocusSupported()) return false;
        final PointF p = new PointF(point.x, point.y); // copy.
        List<Camera.Area> meteringAreas2 = computeMeteringAreas(p.x, p.y);
        List<Camera.Area> meteringAreas1 = meteringAreas2.subList(0, 1);
        synchronized (mLock) {
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
        return true;
    }


    private List<Camera.Area> computeMeteringAreas(double viewClickX, double viewClickY) {
        // Event came in view coordinates. We must rotate to sensor coordinates.
        // First, rescale to the -1000 ... 1000 range.
        int displayToSensor = -computeSensorToDisplayOffset();
        double viewWidth = mPreview.getView().getWidth();
        double viewHeight = mPreview.getView().getHeight();
        viewClickX = -1000d + (viewClickX / viewWidth) * 2000d;
        viewClickY = -1000d + (viewClickY / viewHeight) * 2000d;

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


    private Rect computeMeteringArea(double centerX, double centerY, double size) {
        double delta = size / 2d;
        int top = (int) Math.max(centerY - delta, -1000);
        int bottom = (int) Math.min(centerY + delta, 1000);
        int left = (int) Math.max(centerX - delta, -1000);
        int right = (int) Math.min(centerX + delta, 1000);
        LOG.i("focus:", "computeMeteringArea:", "top:", top, "left:", left, "bottom:", bottom, "right:", right);
        return new Rect(left, top, right, bottom);
    }


    // -----------------
    // Size static stuff.


    /**
     * Returns a list of {@link Size} out of Camera.Sizes.
     */
    @Nullable
    private static List<Size> sizesFromList(List<Camera.Size> sizes) {
        if (sizes == null) return null;
        List<Size> result = new ArrayList<>(sizes.size());
        for (Camera.Size size : sizes) {
            result.add(new Size(size.width, size.height));
        }
        LOG.i("size:", "sizesFromList:", result);
        return result;
    }


    /**
     * Policy here is to return a size that is big enough to fit the surface size,
     * and possibly consistent with the target aspect ratio.
     * @param sizes list of possible sizes
     * @param targetRatio aspect ratio
     * @param biggerThan size representing the current surface size
     * @return chosen size
     */
    private static Size matchSize(List<Size> sizes, AspectRatio targetRatio, Size biggerThan, boolean biggestPossible) {
        if (sizes == null) return null;

        List<Size> consistent = new ArrayList<>(5);
        List<Size> bigEnoughAndConsistent = new ArrayList<>(5);

        final Size targetSize = biggerThan;
        for (Size size : sizes) {
            AspectRatio ratio = AspectRatio.of(size.getWidth(), size.getHeight());
            if (ratio.equals(targetRatio)) {
                consistent.add(size);
                if (size.getHeight() >= targetSize.getHeight() && size.getWidth() >= targetSize.getWidth()) {
                    bigEnoughAndConsistent.add(size);
                }
            }
        }

        LOG.i("size:", "matchSize:", "found consistent:", consistent.size());
        LOG.i("size:", "matchSize:", "found big enough and consistent:", bigEnoughAndConsistent.size());
        Size result;
        if (biggestPossible) {
            if (bigEnoughAndConsistent.size() > 0) {
                result = Collections.max(bigEnoughAndConsistent);
            } else if (consistent.size() > 0) {
                result = Collections.max(consistent);
            } else {
                result = Collections.max(sizes);
            }
        } else {
            if (bigEnoughAndConsistent.size() > 0) {
                result = Collections.min(bigEnoughAndConsistent);
            } else if (consistent.size() > 0) {
                result = Collections.max(consistent);
            } else {
                result = Collections.max(sizes);
            }
        }
        LOG.i("size", "matchSize:", "returning result", result);
        return result;
    }


}

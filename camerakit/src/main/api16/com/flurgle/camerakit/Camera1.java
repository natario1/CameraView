package com.flurgle.camerakit;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.flurgle.camerakit.CameraKit.Constants.FOCUS_CONTINUOUS;
import static com.flurgle.camerakit.CameraKit.Constants.FOCUS_OFF;
import static com.flurgle.camerakit.CameraKit.Constants.FOCUS_TAP;
import static com.flurgle.camerakit.CameraKit.Constants.FOCUS_TAP_WITH_MARKER;
import static com.flurgle.camerakit.CameraKit.Constants.SESSION_TYPE_PICTURE;
import static com.flurgle.camerakit.CameraKit.Constants.SESSION_TYPE_VIDEO;

@SuppressWarnings("deprecation")
class Camera1 extends CameraImpl {

    private static final String TAG = Camera1.class.getSimpleName();

    private static final int FOCUS_AREA_SIZE_DEFAULT = 200;
    private static final int FOCUS_METERING_AREA_WEIGHT_DEFAULT = 1000;
    private static final int DELAY_MILLIS_BEFORE_RESETTING_FOCUS = 3000;

    private int mCameraId;
    private Camera mCamera;
    private ExtraProperties mExtraProperties;
    private Size mPreviewSize;
    private Size mCaptureSize;
    private MediaRecorder mMediaRecorder;
    private File mVideoFile;
    private Camera.AutoFocusCallback mAutofocusCallback;

    private int mDisplayOffset;
    private int mDeviceOrientation;
    private int mSensorOffset;

    @ZoomMode private int mZoom;
    private double mLatitude;
    private double mLongitude;
    private boolean mFocusOnTap;

    private Handler mFocusHandler = new Handler();
    private ConstantMapper.MapperImpl mMapper = new ConstantMapper.Mapper1();
    private boolean mIsSetup = false;
    private boolean mIsCapturingImage = false;
    private boolean mIsCapturingVideo = false;
    private final Object mLock = new Object();


    Camera1(CameraView.CameraCallbacks callback, final PreviewImpl preview) {
        super(callback, preview);
    }

    /**
     * Preview surface is now available. If camera is open, set up.
     */
    @Override
    public void onSurfaceAvailable() {
        Log.e(TAG, "onSurfaceAvailable, size is "+mPreview.getSurfaceSize());
        if (shouldSetup()) setup();
    }

    /**
     * Preview surface did change its size. Compute a new preview size.
     * This requires stopping and restarting the preview.
     */
    @Override
    public void onSurfaceChanged() {
        Log.e(TAG, "onSurfaceChanged, size is "+mPreview.getSurfaceSize());
        if (mIsSetup) {
            // Compute a new camera preview size.
            Size newSize = computePreviewSize();
            if (!newSize.equals(mPreviewSize)) {
                mPreviewSize = newSize;
                mCameraListener.onCameraPreviewSizeChanged();
                synchronized (mLock) {
                    mCamera.stopPreview();
                    Camera.Parameters params = mCamera.getParameters();
                    params.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                    mCamera.setParameters(params);
                }
                boolean invertPreviewSizes = shouldFlipSizes();
                mPreview.setDesiredSize(
                        invertPreviewSizes ? mPreviewSize.getHeight() : mPreviewSize.getWidth(),
                        invertPreviewSizes ? mPreviewSize.getWidth() : mPreviewSize.getHeight()
                );
                mCamera.startPreview();
            }
        }
    }

    private boolean shouldSetup() {
        return isCameraOpened() && mPreview.isReady() && !mIsSetup;
    }

    // The act of binding an "open" camera to a "ready" preview.
    // These can happen at different times but we want to end up here.
    private void setup() {
        try {
            if (mPreview.getOutputClass() == SurfaceHolder.class) {
                mCamera.setPreviewDisplay(mPreview.getSurfaceHolder());
            } else {
                mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        boolean invertPreviewSizes = shouldFlipSizes(); // mDisplayOffset % 180 != 0;
        mCaptureSize = computeCaptureSize();
        mPreviewSize = computePreviewSize();
        mCameraListener.onCameraPreviewSizeChanged();
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
        mCamera.startPreview();
        mIsSetup = true;
    }


    @Override
    void start() {
        if (isCameraOpened()) stop();
        if (collectCameraId()) {
            mCamera = Camera.open(mCameraId);

            // Set parameters that might have been set before the camera was opened.
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                mergeFocus(params, CameraKit.Defaults.DEFAULT_FOCUS);
                mergeFlash(params, CameraKit.Defaults.DEFAULT_FLASH);
                mergeLocation(params, 0d, 0d);
                mergeWhiteBalance(params, CameraKit.Defaults.DEFAULT_WHITE_BALANCE);
                params.setRecordingHint(mSessionType == SESSION_TYPE_VIDEO);
                mCamera.setParameters(params);
            }

            // Try starting preview.
            mCamera.setDisplayOrientation(computeSensorToDisplayOffset()); // <- not allowed during preview
            if (shouldSetup()) setup();
            collectExtraProperties();
            mCameraListener.dispatchOnCameraOpened();
        }
    }


    @Override
    void stop() {
        mFocusHandler.removeCallbacksAndMessages(null);
        if (isCameraOpened()) {
            if (mIsCapturingVideo) endVideo();
            mCamera.stopPreview();
            mCamera.release();
            mCameraListener.dispatchOnCameraClosed();
        }
        mCamera = null;
        mPreviewSize = null;
        mCaptureSize = null;
        mIsSetup = false;
    }

    private boolean collectCameraId() {
        int internalFacing = mMapper.mapFacing(mFacing);
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
    void onDisplayOffset(int displayOrientation) {
        // I doubt this will ever change.
        this.mDisplayOffset = displayOrientation;
    }

    @Override
    void onDeviceOrientation(int deviceOrientation) {
        this.mDeviceOrientation = deviceOrientation;
    }


    @Override
    void setSessionType(@SessionType int sessionType) {
        if (sessionType != mSessionType) {
            mSessionType = sessionType;
            if (isCameraOpened()) {
                start();
            }
        }
    }

    @Override
    void setLocation(double latitude, double longitude) {
        double oldLat = mLatitude;
        double oldLong = mLongitude;
        mLatitude = latitude;
        mLongitude = longitude;
        if (isCameraOpened()) {
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeLocation(params, oldLat, oldLong)) mCamera.setParameters(params);
            }
        }
    }

    private boolean mergeLocation(Camera.Parameters params, double oldLatitude, double oldLongitude) {
        if (mLatitude != 0 && mLongitude != 0) {
            params.setGpsLatitude(mLatitude);
            params.setGpsLongitude(mLongitude);
            params.setGpsTimestamp(System.currentTimeMillis());
            params.setGpsProcessingMethod("Unknown");

            if (mIsCapturingVideo && mMediaRecorder != null) {
                mMediaRecorder.setLocation((float) mLatitude, (float) mLongitude);
            }
        }
        return true;
    }

    @Override
    void setFacing(@Facing int facing) {
        if (facing != mFacing) {
            mFacing = facing;
            if (collectCameraId() && isCameraOpened()) {
                start();
            }
        }
    }

    @Override
    void setWhiteBalance(@WhiteBalance int whiteBalance) {
        int old = mWhiteBalance;
        mWhiteBalance = whiteBalance;
        if (isCameraOpened()) {
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeWhiteBalance(params, old)) mCamera.setParameters(params);
            }
        }
    }

    private boolean mergeWhiteBalance(Camera.Parameters params, @WhiteBalance int oldWhiteBalance) {
        List<String> supported = params.getSupportedWhiteBalance();
        String internal = mMapper.mapWhiteBalance(mWhiteBalance);
        if (supported != null && supported.contains(internal)) {
            params.setWhiteBalance(internal);
            return true;
        }
        mWhiteBalance = oldWhiteBalance;
        return false;
    }

    @Override
    void setFlash(@Flash int flash) {
        int old = mFlash;
        mFlash = flash;
        if (isCameraOpened()) {
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeFlash(params, old)) mCamera.setParameters(params);
            }
        }
    }


    private boolean mergeFlash(Camera.Parameters params, @Flash int oldFlash) {
        List<String> flashes = params.getSupportedFlashModes();
        String internalFlash = mMapper.mapFlash(mFlash);
        if (flashes != null && flashes.contains(internalFlash)) {
            params.setFlashMode(internalFlash);
            return true;
        }
        mFlash = oldFlash;
        return false;
    }


    @Override
    void setFocus(@Focus int focus) {
        int old = mFocus;
        mFocus = focus;
        if (isCameraOpened()) {
            synchronized (mLock) {
                Camera.Parameters params = mCamera.getParameters();
                if (mergeFocus(params, old)) mCamera.setParameters(params);
            }
        }
        mFocusOnTap = mFocus == FOCUS_TAP || mFocus == FOCUS_TAP_WITH_MARKER;
    }


    private boolean mergeFocus(Camera.Parameters params, @Focus int oldFocus) {
        final List<String> modes = params.getSupportedFocusModes();
        switch (mFocus) {
            case FOCUS_CONTINUOUS:
            case FOCUS_TAP:
            case FOCUS_TAP_WITH_MARKER:
                if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    return true;
                }
                mFocus = oldFocus;
                return false;

            case FOCUS_OFF:
                if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                    return true;
                } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                    return true;
                } else if (modes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    return true;
                }
        }
        return false;
    }


    @Override
    void setZoomMode(@ZoomMode int zoom) {
        this.mZoom = zoom;
    }

    @Override
    void setVideoQuality(int videoQuality) {
        if (mIsCapturingVideo) {
            throw new IllegalStateException("Can't change video quality while recording a video.");
        }
        mVideoQuality = videoQuality;
        if (isCameraOpened() && mSessionType == CameraKit.Constants.SESSION_TYPE_VIDEO) {
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
            Log.e(TAG, "captureSize: "+mCaptureSize);
            Log.e(TAG, "previewSize: "+mPreviewSize);
        }
    }

    @Override
    void capturePicture() {
        if (mIsCapturingImage) return;
        if (!isCameraOpened()) return;
        if (mSessionType == SESSION_TYPE_VIDEO && mIsCapturingVideo) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (!parameters.isVideoSnapshotSupported()) return;
        }

        // Set boolean to wait for image callback
        mIsCapturingImage = true;
        final int exifRotation = computeExifRotation();
        final boolean exifFlip = computeExifFlip();
        final int sensorToDisplay = computeSensorToDisplayOffset();
        synchronized (mLock) {
            Camera.Parameters parameters = mCamera.getParameters();
            Log.e(TAG, "Setting exif rotation to "+exifRotation);
            parameters.setRotation(exifRotation);
            mCamera.setParameters(parameters);
        }
        // Is the final picture (decoded respecting EXIF) consistent with CameraView orientation?
        // We must consider exifOrientation to bring back the picture in the sensor world.
        // Then use sensorToDisplay to move to the display world, where CameraView lives.
        final boolean consistentWithView = (exifRotation + sensorToDisplay + 180) % 180 == 0;
        mCamera.takePicture(null, null, null,
                new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        mIsCapturingImage = false;
                        camera.startPreview(); // This is needed, read somewhere in the docs.
                        mCameraListener.processImage(data, consistentWithView, exifFlip);
                    }
                });
    }


    @Override
    void captureSnapshot() {
        if (!isCameraOpened()) return;
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
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        final boolean consistentWithView = (sensorToDevice + sensorToDisplay + 180) % 180 == 0;
                        byte[] rotatedData = RotationHelper.rotate(data, preWidth, preHeight, sensorToDevice);
                        YuvImage yuv = new YuvImage(rotatedData, format, postWidth, postHeight, null);
                        mCameraListener.processSnapshot(yuv, consistentWithView, exifFlip);
                        mIsCapturingImage = false;
                    }
                }).start();
            }
        });
    }

    @Override
    Size getCaptureSize() {
        return mCaptureSize;
    }

    @Override
    Size getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    boolean shouldFlipSizes() {
        return mSensorOffset % 180 != 0;
    }

    @Override
    boolean isCameraOpened() {
        return mCamera != null;
    }

    @Nullable
    @Override
    ExtraProperties getExtraProperties() {
        return mExtraProperties;
    }

    // Internal:


    /**
     * Returns how much should the sensor image be rotated before being shown.
     * It is meant to be fed to Camera.setDisplayOrientation().
     */
    private int computeSensorToDisplayOffset() {
        if (mFacing == CameraKit.Constants.FACING_FRONT) {
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
        if (mFacing == CameraKit.Constants.FACING_FRONT) {
            return (mSensorOffset - mDeviceOrientation + 360) % 360;
        } else {
            return (mSensorOffset + mDeviceOrientation) % 360;
        }
    }


    /**
     * Whether the exif tag should include a 'flip' operation.
     */
    private boolean computeExifFlip() {
        return mFacing == CameraKit.Constants.FACING_FRONT;
    }


    /**
     * This is called either on cameraView.start(), or when the underlying surface changes.
     * It is possible that in the first call the preview surface has not already computed its
     * dimensions.
     * But when it does, the {@link PreviewImpl.SurfaceCallback} should be called,
     * and this should be refreshed.
     */
    private Size computeCaptureSize() {
        Camera.Parameters params = mCamera.getParameters();
        if (mSessionType == SESSION_TYPE_PICTURE) {
            // Choose the max size.
            List<Size> captureSizes = sizesFromList(params.getSupportedPictureSizes());
            return Collections.max(captureSizes);
        } else {
            // Choose according to developer choice in setVideoQuality.
            // The Camcorder internally checks for cameraParameters.getSupportedVideoSizes() etc.
            // We want the picture size to be the max picture consistent with the video aspect ratio.
            List<Size> captureSizes = sizesFromList(params.getSupportedPictureSizes());
            CamcorderProfile profile = getCamcorderProfile(mVideoQuality);
            AspectRatio targetRatio = AspectRatio.of(profile.videoFrameWidth, profile.videoFrameHeight);
            return matchSize(captureSizes, targetRatio, new Size(0, 0), true);
        }
    }

    private Size computePreviewSize() {
        Camera.Parameters params = mCamera.getParameters();
        List<Size> previewSizes = sizesFromList(params.getSupportedPreviewSizes());
        AspectRatio targetRatio = AspectRatio.of(mCaptureSize.getWidth(), mCaptureSize.getHeight());
        return matchSize(previewSizes, targetRatio, mPreview.getSurfaceSize(), false);
    }


    private void collectExtraProperties() {
        Camera.Parameters params = mCamera.getParameters();
        mExtraProperties = new ExtraProperties(params.getVerticalViewAngle(),
                params.getHorizontalViewAngle());
    }


    // -----------------
    // Video recording stuff.


    @Override
    boolean startVideo(@NonNull File videoFile) {
        mVideoFile = videoFile;
        if (mIsCapturingVideo) return false;
        if (!isCameraOpened()) return false;
        Camera.Parameters params = mCamera.getParameters();
        params.setVideoStabilization(false);
        if (mSessionType == SESSION_TYPE_VIDEO) {
            mIsCapturingVideo = true;
            initMediaRecorder();
            try {
                mMediaRecorder.prepare();
            } catch (Exception e) {
                e.printStackTrace();
                mVideoFile = null;
                endVideo();
                return false;
            }
            mMediaRecorder.start();
            return true;
        } else {
            throw new IllegalStateException("Can't record video while session type is picture");
        }
    }

    @Override
    void endVideo() {
        if (mIsCapturingVideo) {
            mIsCapturingVideo = false;
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            if (mVideoFile != null) {
                mCameraListener.dispatchOnVideoTaken(mVideoFile);
                mVideoFile = null;
            }
        }
    }


    private void initMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        if (mLatitude != 0 && mLongitude != 0) {
            mMediaRecorder.setLocation((float) mLatitude, (float) mLongitude);
        }

        mMediaRecorder.setProfile(getCamcorderProfile(mVideoQuality));
        mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());
        mMediaRecorder.setOrientationHint(computeExifRotation());
        // Not needed. mMediaRecorder.setPreviewDisplay(mPreview.getSurface());
    }


    @NonNull
    private CamcorderProfile getCamcorderProfile(@VideoQuality int videoQuality) {
        switch (videoQuality) {
            case CameraKit.Constants.VIDEO_QUALITY_HIGHEST:
                return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_HIGH);

            case CameraKit.Constants.VIDEO_QUALITY_2160P:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                        CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_2160P)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_2160P);
                }
                // Don't break.

            case CameraKit.Constants.VIDEO_QUALITY_1080P:
                if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_1080P)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_1080P);
                }
                // Don't break.

            case CameraKit.Constants.VIDEO_QUALITY_720P:
                if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_720P)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_720P);
                }
                // Don't break.

            case CameraKit.Constants.VIDEO_QUALITY_480P:
                if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_480P);
                }
                // Don't break.

            case CameraKit.Constants.VIDEO_QUALITY_QVGA:
                if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_QVGA)) {
                    return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_QVGA);
                }
                // Don't break.

            case CameraKit.Constants.VIDEO_QUALITY_LOWEST:
            default:
                // Fallback to lowest.
                return CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_LOW);
        }
    }


    // -----------------
    // Tap to focus stuff.


    @Override
    void onTouchEvent(MotionEvent event) {
        if (!mFocusOnTap) return;
        if (mCamera == null) return;
        if (event.getAction() != MotionEvent.ACTION_UP) return;
        Rect rect = calculateFocusArea(event.getX(), event.getY());
        List<Camera.Area> meteringAreas = new ArrayList<>();
        meteringAreas.add(new Camera.Area(rect, getFocusMeteringAreaWeight()));

        synchronized (mLock) {
            Camera.Parameters parameters = mCamera.getParameters();
            boolean autofocusSupported = parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO);
            if (autofocusSupported) {
                if (parameters.getMaxNumFocusAreas() > 0) parameters.setFocusAreas(meteringAreas);
                if (parameters.getMaxNumMeteringAreas() > 0)
                    parameters.setMeteringAreas(meteringAreas);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(parameters);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        resetFocus(success, camera);
                    }
                });
            }
        }
    }


    private int getFocusAreaSize() {
        return FOCUS_AREA_SIZE_DEFAULT;
    }


    private int getFocusMeteringAreaWeight() {
        return FOCUS_METERING_AREA_WEIGHT_DEFAULT;
    }


    private void resetFocus(final boolean success, final Camera camera) {
        mFocusHandler.removeCallbacksAndMessages(null);
        mFocusHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.cancelAutoFocus();
                    synchronized (mLock) {
                        Camera.Parameters params = camera.getParameters();
                        if (!params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                            params.setFocusAreas(null);
                            params.setMeteringAreas(null);
                            mCamera.setParameters(params);
                        }
                    }

                    if (mAutofocusCallback != null) mAutofocusCallback.onAutoFocus(success, camera);
                }
            }
        }, DELAY_MILLIS_BEFORE_RESETTING_FOCUS);
    }


    private Rect calculateFocusArea(float x, float y) {
        int buffer = getFocusAreaSize() / 2;
        int centerX = calculateCenter(x, mPreview.getView().getWidth(), buffer);
        int centerY = calculateCenter(y, mPreview.getView().getHeight(), buffer);
        return new Rect(
                centerX - buffer,
                centerY - buffer,
                centerX + buffer,
                centerY + buffer
        );
    }


    private static int calculateCenter(float coord, int dimen, int buffer) {
        int normalized = (int) ((coord / dimen) * 2000 - 1000);
        if (Math.abs(normalized) + buffer > 1000) {
            if (normalized > 0) {
                return 1000 - buffer;
            } else {
                return -1000 + buffer;
            }
        } else {
            return normalized;
        }
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

        if (biggestPossible) {
            if (bigEnoughAndConsistent.size() > 0) return Collections.max(bigEnoughAndConsistent);
            if (consistent.size() > 0) return Collections.max(consistent);
            return Collections.max(sizes);
        } else {
            if (bigEnoughAndConsistent.size() > 0) return Collections.min(bigEnoughAndConsistent);
            if (consistent.size() > 0) return Collections.max(consistent);
            return Collections.max(sizes);
        }
    }


}

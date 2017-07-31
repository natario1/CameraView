package com.flurgle.camerakit;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

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

    private static final int FOCUS_AREA_SIZE_DEFAULT = 300;
    private static final int FOCUS_METERING_AREA_WEIGHT_DEFAULT = 1000;
    private static final int DELAY_MILLIS_BEFORE_RESETTING_FOCUS = 3000;

    private int mCameraId;
    private Camera mCamera;
    private Camera.Parameters mCameraParameters;
    private ExtraProperties mExtraProperties;
    private Camera.CameraInfo mCameraInfo;
    private Size mPreviewSize;
    private Size mCaptureSize;
    private MediaRecorder mMediaRecorder;
    private File mVideoFile;
    private Camera.AutoFocusCallback mAutofocusCallback;
    private boolean capturingImage = false;

    private int mDisplayOffset;
    private int mDeviceOrientation;
    private int mSensorOffset;

    @Facing private int mFacing;
    @Flash private int mFlash;
    @Focus private int mFocus;
    @ZoomMode private int mZoom;
    @VideoQuality private int mVideoQuality;
    @WhiteBalance private int mWhiteBalance;
    @SessionType private int mSessionType;
    private double mLatitude;
    private double mLongitude;

    private Handler mHandler = new Handler();
    private ConstantMapper.MapperImpl mMapper = new ConstantMapper.Mapper1();

    Camera1(CameraListener callback, PreviewImpl preview) {
        super(callback, preview);
        preview.setCallback(new PreviewImpl.OnPreviewSurfaceChangedCallback() {
            @Override
            public void onPreviewSurfaceChanged() {
                if (mCamera != null) {
                    setupPreview();
                    computeCameraSizes();
                    adjustCameraParameters();
                }
            }
        });

        mCameraInfo = new Camera.CameraInfo();

    }

    // CameraImpl:

    @Override
    void start() {
        setFacing(mFacing);
        openCamera();
        if (mPreview.isReady()) setupPreview();
        mCamera.startPreview();
    }

    @Override
    void stop() {
        if (mCamera != null) mCamera.stopPreview();
        mHandler.removeCallbacksAndMessages(null);
        releaseCamera();
    }

    /**
     * Sets the output stream for our preview.
     * To be called when preview is ready.
     */
    private void setupPreview() {
        try {
            if (mPreview.getOutputClass() == SurfaceHolder.class) {
                mCamera.setPreviewDisplay(mPreview.getSurfaceHolder());
            } else {
                mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                stop();
                start();
            }
        }
    }

    @Override
    void setLocation(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
        if (mCameraParameters != null) {
            mCameraParameters.setGpsLatitude(latitude);
            mCameraParameters.setGpsLongitude(longitude);
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    void setFacing(@Facing int facing) {
        int internalFacing = mMapper.mapFacing(facing);
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == internalFacing) {
                mSensorOffset = mCameraInfo.orientation;
                mCameraId = i;
                mFacing = facing;
                break;
            }
        }

        if (mFacing == facing && isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    void setWhiteBalance(@WhiteBalance int whiteBalance) {
        int old = mWhiteBalance;
        mWhiteBalance = whiteBalance;
        if (mCameraParameters != null) {
            List<String> supported = mCameraParameters.getSupportedWhiteBalance();
            String internal = mMapper.mapWhiteBalance(whiteBalance);
            if (supported != null && supported.contains(internal)) {
                mCameraParameters.setWhiteBalance(internal);
                mCamera.setParameters(mCameraParameters);
            } else {
                mWhiteBalance = old;
            }
        }
    }

    @Override
    void setFlash(@Flash int flash) {
        int old = mFlash;
        mFlash = flash;
        if (mCameraParameters != null) {
            List<String> flashes = mCameraParameters.getSupportedFlashModes();
            String internalFlash = mMapper.mapFlash(flash);
            if (flashes != null && flashes.contains(internalFlash)) {
                mCameraParameters.setFlashMode(internalFlash);
                mCamera.setParameters(mCameraParameters);
            } else {
                mFlash = old;
            }
        }
    }


    @Override
    void setFocus(@Focus int focus) {
        int old = mFocus;
        mFocus = focus;
        switch (focus) {
            case FOCUS_CONTINUOUS:
                if (mCameraParameters != null) {
                    detachFocusTapListener();
                    final List<String> modes = mCameraParameters.getSupportedFocusModes();
                    if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    } else {
                        mFocus = old;
                    }
                }
                break;

            case FOCUS_TAP:
            case FOCUS_TAP_WITH_MARKER:
                if (mCameraParameters != null) {
                    attachFocusTapListener();
                    final List<String> modes = mCameraParameters.getSupportedFocusModes();
                    if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    } else {
                        mFocus = old;
                    }
                }
                break;

            case FOCUS_OFF:
                if (mCameraParameters != null) {
                    detachFocusTapListener();
                    final List<String> modes = mCameraParameters.getSupportedFocusModes();
                    if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                    } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                    } else {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                }
                break;
        }
    }

    @Override
    void setZoom(@ZoomMode int zoom) {
        this.mZoom = zoom;
    }

    @Override
    void setVideoQuality(int videoQuality) {
        this.mVideoQuality = videoQuality;
    }

    @Override
    void captureImage() {
        switch (mSessionType) {
            case SESSION_TYPE_PICTURE:
                // Null check required for camera here as is briefly null when View is detached
                if (!capturingImage && mCamera != null) {
                    // Set boolean to wait for image callback
                    capturingImage = true;
                    Camera.Parameters parameters = mCamera.getParameters();
                    parameters.setRotation(computeExifOrientation());
                    mCamera.setParameters(parameters);
                    mCamera.takePicture(null, null, null,
                        new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                mCameraListener.onPictureTaken(data);
                                // Reset capturing state to allow photos to be taken
                                capturingImage = false;
                                camera.startPreview();
                            }
                        });
                } else {
                    Log.w(TAG, "Unable, waiting for picture to be taken");
                }
                break;

            case SESSION_TYPE_VIDEO:
                // If we are in a video session, camera captures are fast captures coming
                // from the preview stream.
                // TODO: will calculateCaptureRotation work here? It's the only usage. Test...
                mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        new Thread(new ProcessStillTask(data, camera, calculateCaptureRotation(), new ProcessStillTask.OnStillProcessedListener() {
                            @Override
                            public void onStillProcessed(final YuvImage yuv) {
                                mCameraListener.onPictureTaken(yuv);
                            }
                        })).start();
                    }
                });
                break;
        }
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

    private void openCamera() {
        if (mCamera != null) {
            releaseCamera();
        }

        mCamera = Camera.open(mCameraId);
        mCameraParameters = mCamera.getParameters();

        collectCameraProperties();
        computeCameraSizes();
        adjustCameraParameters();
        mCamera.setDisplayOrientation(computeCameraToDisplayOffset());
        mCameraListener.onCameraOpened();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mCameraParameters = null;
            mPreviewSize = null;
            mCaptureSize = null;
            mCameraListener.onCameraClosed();
        }
    }

    /**
     * Returns how much should the sensor image be rotated before being shown.
     * It is meant to be fed to Camera.setDisplayOrientation().
     */
    private int computeCameraToDisplayOffset() {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            // or: (360 - ((info.orientation + displayOrientation) % 360)) % 360;
            return ((mSensorOffset - mDisplayOffset) + 360 + 180) % 360;
        } else {
            return (mSensorOffset - mDisplayOffset + 360) % 360;
        }
    }

    /**
     * Returns the orientation to be set as a exif tag. This is already managed by
     * the camera APIs as long as you call {@link Camera.Parameters#setRotation(int)}.
     */
    private int computeExifOrientation() {
        return (mDeviceOrientation + mSensorOffset) % 360;
    }

    private int calculateCaptureRotation() {
        int previewRotation = computeCameraToDisplayOffset();
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //Front is flipped
            return (previewRotation + 180 + 2* mDisplayOffset + 720) % 360;
        } else {
            return previewRotation;
        }
    }


    /**
     * This is called either on cameraView.start(), or when the underlying surface changes.
     * It is possible that in the first call the preview surface has not already computed its
     * dimensions.
     * But when it does, the {@link PreviewImpl.OnPreviewSurfaceChangedCallback} should be called,
     * and this should be refreshed.
     */
    private void computeCameraSizes() {
        mCameraParameters.setRecordingHint(mSessionType == SESSION_TYPE_VIDEO);
        List<Size> previewSizes = sizesFromList(mCameraParameters.getSupportedPreviewSizes());
        if (mSessionType == SESSION_TYPE_PICTURE) {
            // Choose the max size.
            List<Size> captureSizes = sizesFromList(mCameraParameters.getSupportedPictureSizes());
            mCaptureSize = Collections.max(captureSizes);
        } else {
            // Choose according to developer choice in setVideoQuality.
            // The Camcorder internally checks for cameraParameters.getSupportedVideoSizes() etc.
            // So its output is our output.
            CamcorderProfile camcorderProfile = getCamcorderProfile(mVideoQuality);
            mCaptureSize = new Size(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
        }
        mPreviewSize = computePreviewSize(previewSizes, mCaptureSize, mPreview.getSurfaceSize());
    }


    private void adjustCameraParameters() {
        boolean invertPreviewSizes = shouldFlipSizes(); // mDisplayOffset % 180 != 0;
        mPreview.setDesiredSize(
                invertPreviewSizes ? getPreviewSize().getHeight() : getPreviewSize().getWidth(),
                invertPreviewSizes ? getPreviewSize().getWidth() : getPreviewSize().getHeight()
        );
        mCameraParameters.setPreviewSize(getPreviewSize().getWidth(), getPreviewSize().getHeight());
        mCameraParameters.setPictureSize(getCaptureSize().getWidth(), getCaptureSize().getHeight());
        // int rotation = calculateCaptureRotation();
        // mCameraParameters.setRotation(rotation);
        setFocus(mFocus);
        setFlash(mFlash);
        setLocation(mLatitude, mLongitude);
        mCamera.setParameters(mCameraParameters);
    }

    private void collectCameraProperties() {
        mExtraProperties = new ExtraProperties(mCameraParameters.getVerticalViewAngle(),
                mCameraParameters.getHorizontalViewAngle());
    }



    @Override
    void startVideo() {
        if (mSessionType == SESSION_TYPE_VIDEO) {
            initMediaRecorder();
            try {
                mMediaRecorder.prepare();
            } catch (Exception e) {
                e.printStackTrace();
                mVideoFile = null;
                endVideo();
                return;
            }
            mMediaRecorder.start();
        } else {
            throw new IllegalStateException("Can't record video while session type is picture");
        }
    }

    @Override
    void endVideo() {
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
        if (mVideoFile != null) {
            mCameraListener.onVideoTaken(mVideoFile);
            mVideoFile = null;
        }
    }


    private void initMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();

        mMediaRecorder.setCamera(mCamera);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);

        mMediaRecorder.setProfile(getCamcorderProfile(mVideoQuality));

        mVideoFile = new File(mPreview.getView().getContext().getExternalFilesDir(null), "video.mp4");
        mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());
        mMediaRecorder.setOrientationHint(computeCameraToDisplayOffset()); // TODO is this correct? Should we use exif orientation? Maybe not.
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


    void setTapToAutofocusListener(Camera.AutoFocusCallback callback) {
        if (this.mFocus != FOCUS_TAP) {
            throw new IllegalArgumentException("Please set the camera to FOCUS_TAP.");
        }

        this.mAutofocusCallback = callback;
    }

    private int getFocusAreaSize() {
        return FOCUS_AREA_SIZE_DEFAULT;
    }

    private int getFocusMeteringAreaWeight() {
        return FOCUS_METERING_AREA_WEIGHT_DEFAULT;
    }

    private void detachFocusTapListener() {
        mPreview.getView().setOnTouchListener(null);
    }

    private void attachFocusTapListener() {
        mPreview.getView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (mCamera != null) {
                        Camera.Parameters parameters = mCamera.getParameters();
                        String focusMode = parameters.getFocusMode();
                        Rect rect = calculateFocusArea(event.getX(), event.getY());
                        List<Camera.Area> meteringAreas = new ArrayList<>();
                        meteringAreas.add(new Camera.Area(rect, getFocusMeteringAreaWeight()));
                        if (parameters.getMaxNumFocusAreas() != 0 && focusMode != null &&
                            (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) ||
                            focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ||
                            focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ||
                            focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                        ) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                            parameters.setFocusAreas(meteringAreas);
                            if (parameters.getMaxNumMeteringAreas() > 0) {
                                parameters.setMeteringAreas(meteringAreas);
                            }
                            if(!parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                                return false; //cannot autoFocus
                            }
                            mCamera.setParameters(parameters);
                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    resetFocus(success, camera);
                                }
                            });
                        } else if (parameters.getMaxNumMeteringAreas() > 0) {
                            if(!parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                                return false; //cannot autoFocus
                            }
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                            parameters.setFocusAreas(meteringAreas);
                            parameters.setMeteringAreas(meteringAreas);

                            mCamera.setParameters(parameters);
                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    resetFocus(success, camera);
                                }
                            });
                        } else {
                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    if (mAutofocusCallback != null) {
                                        mAutofocusCallback.onAutoFocus(success, camera);
                                    }
                                }
                            });
                        }
                    }
                }
                return true;
            }
        });
    }

    private void resetFocus(final boolean success, final Camera camera) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.cancelAutoFocus();
                    Camera.Parameters params = camera.getParameters();
                    if (!params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        params.setFocusAreas(null);
                        params.setMeteringAreas(null);
                        camera.setParameters(params);
                    }

                    if (mAutofocusCallback != null) {
                        mAutofocusCallback.onAutoFocus(success, camera);
                    }
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
     * and possibly consistent with the capture size.
     * @param sizes list of possible sizes
     * @param captureSize size representing desired aspect ratio
     * @param surfaceSize size representing the current surface size. We'd like the returned value to be bigger.
     * @return chosen size
     */
    private static Size computePreviewSize(List<Size> sizes, Size captureSize, Size surfaceSize) {
        if (sizes == null) return null;

        List<Size> consistent = new ArrayList<>(5);
        List<Size> bigEnoughAndConsistent = new ArrayList<>(5);

        final AspectRatio targetRatio = AspectRatio.of(captureSize.getWidth(), captureSize.getHeight());
        final Size targetSize = surfaceSize;
        for (Size size : sizes) {
            AspectRatio ratio = AspectRatio.of(size.getWidth(), size.getHeight());
            if (ratio.equals(targetRatio)) {
                consistent.add(size);
                if (size.getHeight() >= targetSize.getHeight() && size.getWidth() >= targetSize.getWidth()) {
                    bigEnoughAndConsistent.add(size);
                }
            }
        }

        if (bigEnoughAndConsistent.size() > 0) return Collections.min(bigEnoughAndConsistent);
        if (consistent.size() > 0) return Collections.max(consistent);
        return Collections.max(sizes);
    }


}

package com.otaliastudios.cameraview;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.otaliastudios.cameraview.CameraConstants.*;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * TODO: README for gestures
 * TODO: deprecate setFocus, CONTINUOUS should be the default
 *
 */
public class CameraView extends FrameLayout {

    private final static String TAG = CameraView.class.getSimpleName();

    private Handler mWorkerHandler;

    private Handler getWorkerHandler() {
        synchronized (this) {
            if (mWorkerHandler == null) {
                HandlerThread workerThread = new HandlerThread("CameraViewWorker");
                workerThread.setDaemon(true);
                workerThread.start();
                mWorkerHandler = new Handler(workerThread.getLooper());
            }
        }
        return mWorkerHandler;
    }

    private void run(Runnable runnable) {
        getWorkerHandler().post(runnable);
    }

    private int mJpegQuality;
    private boolean mCropOutput;
    private CameraCallbacks mCameraCallbacks;
    private OrientationHelper mOrientationHelper;
    private CameraController mCameraController;
    private Preview mPreviewImpl;

    private GridLinesLayout mGridLinesLayout;
    private PinchGestureLayout mPinchGestureLayout;
    private TapGestureLayout mTapGestureLayout;
    private boolean mIsStarted;
    private boolean mKeepScreenOn;

    private float mZoomValue;
    private float mExposureCorrectionValue;

    private HashMap<Gesture, Integer> mGestureMap = new HashMap<>(4);

    public CameraView(@NonNull Context context) {
        super(context, null);
        init(context, null);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @SuppressWarnings("WrongConstant")
    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CameraView, 0, 0);
        int facing = a.getInteger(R.styleable.CameraView_cameraFacing, Defaults.DEFAULT_FACING);
        int flash = a.getInteger(R.styleable.CameraView_cameraFlash, Defaults.DEFAULT_FLASH);
        int sessionType = a.getInteger(R.styleable.CameraView_cameraSessionType, Defaults.DEFAULT_SESSION_TYPE);
        int whiteBalance = a.getInteger(R.styleable.CameraView_cameraWhiteBalance, Defaults.DEFAULT_WHITE_BALANCE);
        int videoQuality = a.getInteger(R.styleable.CameraView_cameraVideoQuality, Defaults.DEFAULT_VIDEO_QUALITY);
        int grid = a.getInteger(R.styleable.CameraView_cameraGrid, Defaults.DEFAULT_GRID);
        mJpegQuality = a.getInteger(R.styleable.CameraView_cameraJpegQuality, Defaults.DEFAULT_JPEG_QUALITY);
        mCropOutput = a.getBoolean(R.styleable.CameraView_cameraCropOutput, Defaults.DEFAULT_CROP_OUTPUT);
        int tapGesture = a.getInteger(R.styleable.CameraView_cameraGestureTap, Defaults.DEFAULT_GESTURE_ACTION_TAP);
        // int doubleTapGesture = a.getInteger(R.styleable.CameraView_cameraGestureDoubleTap, Defaults.DEFAULT_GESTURE_ACTION_DOUBLE_TAP);
        int longTapGesture = a.getInteger(R.styleable.CameraView_cameraGestureLongTap, Defaults.DEFAULT_GESTURE_ACTION_LONG_TAP);
        int pinchGesture = a.getInteger(R.styleable.CameraView_cameraGesturePinch, Defaults.DEFAULT_GESTURE_ACTION_PINCH);
        a.recycle();

        mCameraCallbacks = new CameraCallbacks();
        mPreviewImpl = new TextureViewPreview(context, this);
        mCameraController = new Camera1(mCameraCallbacks, mPreviewImpl);
        mGridLinesLayout = new GridLinesLayout(context);
        mPinchGestureLayout = new PinchGestureLayout(context);
        mTapGestureLayout = new TapGestureLayout(context);
        addView(mGridLinesLayout);
        addView(mPinchGestureLayout);
        addView(mTapGestureLayout);

        mIsStarted = false;
        setFacing(facing);
        setFlash(flash);
        setSessionType(sessionType);
        setVideoQuality(videoQuality);
        setWhiteBalance(whiteBalance);
        setGrid(grid);
        mapGesture(Gesture.TAP, tapGesture);
        // mapGesture(Gesture.DOUBLE_TAP, doubleTapGesture);
        mapGesture(Gesture.LONG_TAP, longTapGesture);
        mapGesture(Gesture.PINCH, pinchGesture);

        if (!isInEditMode()) {
            mOrientationHelper = new OrientationHelper(context) {
                @Override
                public void onDisplayOffsetChanged(int displayOffset) {
                    mCameraController.onDisplayOffset(displayOffset);
                    mPreviewImpl.onDisplayOffset(displayOffset);
                }

                @Override
                protected void onDeviceOrientationChanged(int deviceOrientation) {
                    mCameraController.onDeviceOrientation(deviceOrientation);
                    mPreviewImpl.onDeviceOrientation(deviceOrientation);
                }
            };
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            mOrientationHelper.enable(manager.getDefaultDisplay());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            mOrientationHelper.disable();
        }
        super.onDetachedFromWindow();
    }


    // Smart measuring behavior
    // ------------------------


    private String ms(int mode) {
        switch (mode) {
            case AT_MOST: return "AT_MOST";
            case EXACTLY: return "EXACTLY";
            case UNSPECIFIED: return "UNSPECIFIED";
        }
        return null;
    }

    /**
     * Measuring is basically controlled by layout params width and height.
     * The basic semantics are:
     *
     * - MATCH_PARENT: CameraView should completely fill this dimension, even if this might mean
     *                 not respecting the preview aspect ratio.
     * - WRAP_CONTENT: CameraView should try to adapt this dimension to respect the preview
     *                 aspect ratio.
     *
     * When both dimensions are MATCH_PARENT, CameraView will fill its
     * parent no matter the preview. Thanks to what happens in {@link Preview}, this acts like
     * a CENTER CROP scale type.
     *
     * When both dimensions are WRAP_CONTENT, CameraView will take the biggest dimensions that
     * fit the preview aspect ratio. This acts like a CENTER INSIDE scale type.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Size previewSize = getPreviewSize();
        if (previewSize == null) {
            Log.e(TAG, "onMeasure, surface is not ready. Calling default behavior.");
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // Let's which dimensions need to be adapted.
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthValue = MeasureSpec.getSize(widthMeasureSpec);
        final int heightValue = MeasureSpec.getSize(heightMeasureSpec);
        final boolean flip = mCameraController.shouldFlipSizes();
        final float previewWidth = flip ? previewSize.getHeight() : previewSize.getWidth();
        final float previewHeight = flip ? previewSize.getWidth() : previewSize.getHeight();

        // If MATCH_PARENT is interpreted as AT_MOST, transform to EXACTLY
        // to be consistent with our semantics.
        final ViewGroup.LayoutParams lp = getLayoutParams();
        if (widthMode == AT_MOST && lp.width == MATCH_PARENT) widthMode = EXACTLY;
        if (heightMode == AT_MOST && lp.height == MATCH_PARENT) heightMode = EXACTLY;
        Log.e(TAG, "onMeasure, requested dimensions are (" +
                widthValue + "[" + ms(widthMode) + "]x" +
                heightValue + "[" + ms(heightMode) + "])");
        Log.e(TAG, "onMeasure, previewSize is (" + previewWidth + "x" + previewHeight + ")");


        // If we have fixed dimensions (either 300dp or MATCH_PARENT), there's nothing we should do,
        // other than respect it. The preview will eventually be cropped at the sides (by PreviewImpl scaling)
        // except the case in which these fixed dimensions somehow fit exactly the preview aspect ratio.
        if (widthMode == EXACTLY && heightMode == EXACTLY) {
            Log.e(TAG, "onMeasure, both are MATCH_PARENT or fixed value. We adapt. This means CROP_INSIDE. " +
                    "(" + widthValue + "x" + heightValue + ")");
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // If both dimensions are free, with no limits, then our size will be exactly the
        // preview size. This can happen rarely, for example in scrollable containers.
        if (widthMode == UNSPECIFIED && heightMode == UNSPECIFIED) {
            Log.e(TAG, "onMeasure, both are completely free. We respect that and extend to the whole preview size. " +
                    "(" + previewWidth + "x" + previewHeight + ")");
            super.onMeasure(MeasureSpec.makeMeasureSpec((int) previewWidth, EXACTLY),
                    MeasureSpec.makeMeasureSpec((int) previewHeight, EXACTLY));
            return;
        }

        // It sure now that at least one dimension can be determined (either because EXACTLY or AT_MOST).
        // This starts to seem a pleasant situation.

        // If one of the dimension is completely free, take the other and fit the ratio.
        // One of the two might be AT_MOST, but we use the value anyway.
        float ratio = previewHeight / previewWidth;
        if (widthMode == UNSPECIFIED || heightMode == UNSPECIFIED) {
            boolean freeWidth = widthMode == UNSPECIFIED;
            int height, width;
            if (freeWidth) {
                height = heightValue;
                width = (int) (height / ratio);
            } else {
                width = widthValue;
                height = (int) (width * ratio);
            }
            Log.e(TAG, "onMeasure, one dimension was free, we adapted it to fit the aspect ratio. " +
                    "(" + width + "x" + height + ")");
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, EXACTLY));
            return;
        }

        // At this point both dimensions are either AT_MOST-AT_MOST, EXACTLY-AT_MOST or AT_MOST-EXACTLY.
        // Let's manage this sanely. If only one is EXACTLY, we can TRY to fit the aspect ratio,
        // but it is not guaranteed to succeed. It depends on the AT_MOST value of the other dimensions.
        if (widthMode == EXACTLY || heightMode == EXACTLY) {
            boolean freeWidth = widthMode == AT_MOST;
            int height, width;
            if (freeWidth) {
                height = heightValue;
                width = Math.min((int) (height / ratio), widthValue);
            } else {
                width = widthValue;
                height = Math.min((int) (width * ratio), heightValue);
            }
            Log.e(TAG, "onMeasure, one dimension was EXACTLY, another AT_MOST. We have TRIED to fit " +
                    "the aspect ratio, but it's not guaranteed. (" + width + "x" + height + ")");
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, EXACTLY));
            return;
        }

        // Last case, AT_MOST and AT_MOST. Here we can SURELY fit the aspect ratio by filling one
        // dimension and adapting the other.
        int height, width;
        float atMostRatio = heightValue / widthValue;
        if (atMostRatio >= ratio) {
            // We must reduce height.
            width = widthValue;
            height = (int) (width * ratio);
        } else {
            height = heightValue;
            width = (int) (height / ratio);
        }
        Log.e(TAG, "onMeasure, both dimension were AT_MOST. We fit the preview aspect ratio. " +
                "(" + width + "x" + height + ")");
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, EXACTLY),
                MeasureSpec.makeMeasureSpec(height, EXACTLY));
    }


    // Gesture APIs and touch control
    // ------------------------------


    /**
     * Maps a {@link Gesture} to a certain gesture action.
     * For example, you can assign zoom control to the pinch gesture by just calling:
     * <code>
     *     cameraView.mapGesture(Gesture.PINCH, CameraConstants.GESTURE_ACTION_ZOOM);
     * </code>
     *
     * Not all actions can be assigned to a certain gesture. For example, zoom control can't be
     * assigned to the Gesture.TAP gesture. Look at {@link Gesture} to know more.
     * This method returns false if they are not assignable.
     *
     * @param gesture which gesture to map
     * @param action which action should be assigned
     * @return true if this action could be assigned to this gesture
     */
    public boolean mapGesture(@NonNull Gesture gesture, @GestureAction int action) {
        if (gesture.isAssignableTo(action)) {
            mGestureMap.put(gesture, action);
            switch (gesture) {
                case PINCH:
                    mPinchGestureLayout.enable(mGestureMap.get(Gesture.PINCH) != GESTURE_ACTION_NONE);
                    break;
                case TAP:
                // case DOUBLE_TAP:
                case LONG_TAP:
                    mTapGestureLayout.enable(mGestureMap.get(Gesture.TAP) != GESTURE_ACTION_NONE ||
                            // mGestureMap.get(Gesture.DOUBLE_TAP) != GESTURE_ACTION_NONE ||
                            mGestureMap.get(Gesture.LONG_TAP) != GESTURE_ACTION_NONE);
                    break;
            }
            return true;
        }
        mapGesture(gesture, GESTURE_ACTION_NONE);
        return false;
    }


    /**
     * Clears any action mapped to the given gesture.
     * @param gesture which gesture to clear
     */
    public void clearGesture(@NonNull Gesture gesture) {
        mGestureMap.put(gesture, GESTURE_ACTION_NONE);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true; // Steal our own events.
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mCameraController.isCameraOpened()) return true;

        CameraOptions options = mCameraController.getCameraOptions(); // Non null
        if (mPinchGestureLayout.onTouchEvent(event)) {
            int action = mGestureMap.get(Gesture.PINCH);
            // This currently can be zoom or AE.
            // Camera can either support these or not.
            if (action == GESTURE_ACTION_ZOOM) {
                float oldValue = mZoomValue;
                float newValue = mPinchGestureLayout.scaleValue(oldValue, 0, 1);
                PointF[] points = mPinchGestureLayout.getPoints();
                if (mCameraController.setZoom(newValue)) {
                    mZoomValue = newValue;
                    mCameraCallbacks.dispatchOnZoomChanged(newValue, points);
                }

            } else if (action == GESTURE_ACTION_AE_CORRECTION) {
                float oldValue = mExposureCorrectionValue;
                float minValue = options.getExposureCorrectionMinValue();
                float maxValue = options.getExposureCorrectionMaxValue();
                float newValue = mPinchGestureLayout.scaleValue(oldValue, minValue, maxValue);
                PointF[] points = mPinchGestureLayout.getPoints();
                float[] bounds = new float[]{minValue, maxValue};
                if (mCameraController.setExposureCorrection(newValue)) {
                    mExposureCorrectionValue = newValue;
                    mCameraCallbacks.dispatchOnExposureCorrectionChanged(newValue, bounds, points);
                }
            }

        } else if (mTapGestureLayout.onTouchEvent(event)) {
            Gesture gesture = mTapGestureLayout.getGestureType();
            int action = mGestureMap.get(gesture);
            // This currently can be capture, focus or focusWithMaker.
            // Camera can either support these or not.
            if (action == GESTURE_ACTION_CAPTURE) {
                capturePicture();

            } else if (action == GESTURE_ACTION_FOCUS ||
                    action == GESTURE_ACTION_FOCUS_WITH_MARKER) {
                PointF point = mTapGestureLayout.getPoint();
                mCameraController.startAutoFocus(gesture, point); // This will call onFocusStart and onFocusEnd
            }
        }
        return true;
    }


    // Lifecycle APIs
    // --------------


    /**
     * Returns whether the camera has started showing its preview.
     * @return whether the camera has started
     */
    public boolean isStarted() {
        return mIsStarted;
    }


    /**
     * Starts the camera preview, if not started already.
     * This should be called onResume(), or when you are ready with permissions.
     */
    public void start() {
        if (mIsStarted || !isEnabled()) {
            // Already started, do nothing.
            return;
        }

        if (checkPermissions(getSessionType())) {
            mIsStarted = true;
            run(new Runnable() {
                @Override
                public void run() {
                    mCameraController.start();
                }
            });
        }
    }


    /**
     * Checks that we have appropriate permissions for this session type.
     * Throws if session = audio and manifest did not add the microphone permissions.
     * @return true if we can go on, false otherwise.
     */
    private boolean checkPermissions(@SessionType int sessionType) {
        checkPermissionsManifestOrThrow(sessionType);
        boolean api23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        int cameraCheck, audioCheck;
        if (!api23) {
            cameraCheck = PackageManager.PERMISSION_GRANTED;
            audioCheck = PackageManager.PERMISSION_GRANTED;
        } else {
            //noinspection all
            cameraCheck = getContext().checkSelfPermission(Manifest.permission.CAMERA);
            //noinspection all
            audioCheck = getContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        }
        switch (sessionType) {
            case SESSION_TYPE_VIDEO:
                if (cameraCheck != PackageManager.PERMISSION_GRANTED || audioCheck != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(true, true);
                    return false;
                }
                break;

            case SESSION_TYPE_PICTURE:
                if (cameraCheck != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(true, false);
                    return false;
                }
                break;
        }
        return true;
    }


    /**
     * If mSessionType == SESSION_TYPE_VIDEO we will ask for RECORD_AUDIO permission.
     * If the developer did not add this to its manifest, throw and fire warnings.
     * (Hoping this is not cought elsewhere... we should test).
     */
    private void checkPermissionsManifestOrThrow(@SessionType int sessionType) {
        if (sessionType == SESSION_TYPE_VIDEO) {
            try {
                PackageManager manager = getContext().getPackageManager();
                PackageInfo info = manager.getPackageInfo(getContext().getPackageName(), PackageManager.GET_PERMISSIONS);
                for (String requestedPermission : info.requestedPermissions) {
                    if (requestedPermission.equals(Manifest.permission.RECORD_AUDIO)) {
                        return;
                    }
                }
                String message = "When the session type is set to video, the RECORD_AUDIO permission " +
                        "should be added to the application manifest file.";
                Log.w(TAG, message);
                throw new IllegalStateException(message);
            } catch (PackageManager.NameNotFoundException e) {
                // Not possible.
            }
        }
    }


    /**
     * Stops the current preview, if any was started.
     * This should be called onPause().
     */
    public void stop() {
        if (!mIsStarted) {
            // Already stopped, do nothing.
            return;
        }
        mIsStarted = false;
        mCameraController.stop();
    }

    public void destroy() {
        mCameraCallbacks.clearListeners(); // Release inner listener.
        // This might be useless, but no time to think about it now.
        mWorkerHandler = null;
    }


    // Public APIs for parameters and controls
    // ---------------------------------------


    /**
     * Returns a {@link CameraOptions} instance holding supported options for this camera
     * session. This might change over time. It's better to hold a reference from
     * {@link CameraListener#onCameraOpened(CameraOptions)}.
     *
     * @return an options map, or null if camera was not opened
     */
    @Nullable
    public CameraOptions getCameraOptions() {
        return mCameraController.getCameraOptions();
    }


    /**
     * If present, returns a collection of extra properties from the current camera
     * session.
     * @return an ExtraProperties object.
     */
    @Nullable
    public ExtraProperties getExtraProperties() {
        return mCameraController.getExtraProperties();
    }


    /**
     * Sets exposure adjustment, in EV stops. A positive value will mean brighter picture.
     *
     * If camera is not opened, this will have no effect.
     * If {@link CameraOptions#isExposureCorrectionSupported()} is false, this will have no effect.
     * The provided value should be between the bounds returned by {@link CameraOptions}, or it will
     * be capped.
     *
     * @see CameraOptions#getExposureCorrectionMinValue()
     * @see CameraOptions#getExposureCorrectionMaxValue()
     *
     * @param EVvalue exposure correction value.
     */
    public void setExposureCorrection(float EVvalue) {
        CameraOptions options = getCameraOptions();
        if (options != null) {
            float min = options.getExposureCorrectionMinValue();
            float max = options.getExposureCorrectionMaxValue();
            if (EVvalue < min) EVvalue = min;
            if (EVvalue > max) EVvalue = max;
            if (mCameraController.setExposureCorrection(EVvalue)) {
                mExposureCorrectionValue = EVvalue;
            }
        }
    }


    /**
     * Returns the current exposure correction value, typically 0
     * at start-up.
     * @return the current exposure correction value
     */
    public float getExposureCorrection() {
        return mExposureCorrectionValue;
    }


    /**
     * Sets a zoom value. This is not guaranteed to be supported by the current device,
     * but you can take a look at {@link CameraOptions#isZoomSupported()}.
     * This will have no effect if called before the camera is opened.
     *
     * Zoom value should be between 0 and 1, where 1 will be the maximum available zoom.
     * If it's not, it will be capped.
     *
     * @param zoom value in [0,1]
     */
    public void setZoom(float zoom) {
        if (zoom < 0) zoom = 0;
        if (zoom > 1) zoom = 1;
        if (mCameraController.setZoom(zoom)) {
            mZoomValue = zoom;
        }
    }


    /**
     * Returns the current zoom value, something between 0 and 1.
     * @return the current zoom value
     */
    public float getZoom() {
        return mZoomValue;
    }


    /**
     * Controls the grids to be drawn over the current layout.
     *
     * @see CameraConstants#GRID_OFF
     * @see CameraConstants#GRID_3X3
     * @see CameraConstants#GRID_4X4
     *
     * @param gridMode desired grid mode
     */
    public void setGrid(@Grid int gridMode) {
        mGridLinesLayout.setGridMode(gridMode);
    }


    /**
     * Gets the current grid mode.
     * @return the current grid mode
     */
    @Grid
    public int getGrid() {
        return mGridLinesLayout.getGridMode();
    }


    /**
     * Set location coordinates to be found later in the jpeg EXIF header
     *
     * @param latitude current latitude
     * @param longitude current longitude
     */
    public void setLocation(double latitude, double longitude) {
        mCameraController.setLocation(latitude, longitude);
    }


    /**
     * Sets desired white balance to current camera session.
     *
     * @see CameraConstants#WHITE_BALANCE_AUTO
     * @see CameraConstants#WHITE_BALANCE_CLOUDY
     * @see CameraConstants#WHITE_BALANCE_DAYLIGHT
     * @see CameraConstants#WHITE_BALANCE_FLUORESCENT
     * @see CameraConstants#WHITE_BALANCE_INCANDESCENT
     *
     * @param whiteBalance desired white balance behavior.
     */
    public void setWhiteBalance(@WhiteBalance int whiteBalance) {
        mCameraController.setWhiteBalance(whiteBalance);
    }


    /**
     * Returns the current white balance behavior.
     * @return white balance value.
     */
    @WhiteBalance
    public int getWhiteBalance() {
        return mCameraController.getWhiteBalance();
    }


    /**
     * Sets which camera sensor should be used.
     *
     * @see CameraConstants#FACING_FRONT
     * @see CameraConstants#FACING_BACK
     *
     * @param facing a facing value.
     */
    public void setFacing(@Facing final int facing) {
        run(new Runnable() {
            @Override
            public void run() {
                mCameraController.setFacing(facing);
            }
        });
    }


    /**
     * Gets the facing camera currently being used.
     * @return a facing value.
     */
    @Facing
    public int getFacing() {
        return mCameraController.getFacing();
    }


    /**
     * Toggles the facing value between {@link CameraConstants#FACING_BACK}
     * and {@link CameraConstants#FACING_FRONT}.
     *
     * @return the new facing value
     */
    @Facing
    public int toggleFacing() {
        int facing = mCameraController.getFacing();
        switch (facing) {
            case FACING_BACK:
                setFacing(FACING_FRONT);
                break;

            case FACING_FRONT:
                setFacing(FACING_BACK);
                break;
        }

        return mCameraController.getFacing();
    }


    /**
     * Sets the flash mode.
     *
     * @see CameraConstants#FLASH_OFF
     * @see CameraConstants#FLASH_ON
     * @see CameraConstants#FLASH_AUTO
     * @see CameraConstants#FLASH_TORCH

     * @param flash desired flash mode.
     */
    public void setFlash(@Flash int flash) {
        mCameraController.setFlash(flash);
    }


    /**
     * Gets the current flash mode.
     * @return a flash mode
     */
    @Flash
    public int getFlash() {
        return mCameraController.getFlash();
    }


    /**
     * Toggles the flash mode between {@link CameraConstants#FLASH_OFF},
     * {@link CameraConstants#FLASH_ON} and {@link CameraConstants#FLASH_AUTO}, in this order.
     *
     * @return the new flash value
     */
    @Flash
    public int toggleFlash() {
        int flash = mCameraController.getFlash();
        switch (flash) {
            case FLASH_OFF:
                setFlash(FLASH_ON);
                break;

            case FLASH_ON:
                setFlash(FLASH_AUTO);
                break;

            case FLASH_AUTO:
            case FLASH_TORCH:
                setFlash(FLASH_OFF);
                break;
        }

        return mCameraController.getFlash();
    }


    /**
     * Starts an autofocus process at the given coordinates, with respect
     * to the view width and height.
     *
     * @param x should be between 0 and getWidth()
     * @param y should be between 0 and getHeight()
     */
    public void startAutoFocus(float x, float y) {
        if (x < 0 || x > getWidth()) throw new IllegalArgumentException("x should be >= 0 and <= getWidth()");
        if (y < 0 || y > getHeight()) throw new IllegalArgumentException("y should be >= 0 and <= getHeight()");
        mCameraController.startAutoFocus(null, new PointF(x, y));
    }


    /**
     * Set the current session type to either picture or video.
     * When sessionType is video,
     * - {@link #startCapturingVideo(File)} will not throw any exception
     * - {@link #capturePicture()} will fallback to {@link #captureSnapshot()}
     *
     * @see CameraConstants#SESSION_TYPE_PICTURE
     * @see CameraConstants#SESSION_TYPE_VIDEO
     *
     * @param sessionType desired session type.
     */
    public void setSessionType(@SessionType int sessionType) {

        if (sessionType == getSessionType() || !mIsStarted) {
            // Check did took place, or will happen on start().
            mCameraController.setSessionType(sessionType);

        } else if (checkPermissions(sessionType)) {
            // Camera is running. CameraImpl setSessionType will do the trick.
            mCameraController.setSessionType(sessionType);

        } else {
            // This means that the audio permission is being asked.
            // Stop the camera so it can be restarted by the developer onPermissionResult.
            // Developer must also set the session type again...
            // Not ideal but good for now.
            stop();
        }
    }


    /**
     * Gets the current session type.
     * @return the current session type
     */
    @SessionType
    public int getSessionType() {
        return mCameraController.getSessionType();
    }


    /**
     * Sets video recording quality. This is not guaranteed to be supported by current device.
     * If it's not, a lower quality will be chosen, until a supported one is found.
     * If sessionType is video, this might trigger a camera restart and a change in preview size.
     *
     * @see CameraConstants#VIDEO_QUALITY_LOWEST
     * @see CameraConstants#VIDEO_QUALITY_QVGA
     * @see CameraConstants#VIDEO_QUALITY_480P
     * @see CameraConstants#VIDEO_QUALITY_720P
     * @see CameraConstants#VIDEO_QUALITY_1080P
     * @see CameraConstants#VIDEO_QUALITY_2160P
     * @see CameraConstants#VIDEO_QUALITY_HIGHEST
     *
     * @param videoQuality requested video quality
     */
    public void setVideoQuality(@VideoQuality int videoQuality) {
        mCameraController.setVideoQuality(videoQuality);
    }


    /**
     * Gets the current video quality.
     * @return the current video quality
     */
    @VideoQuality
    public int getVideoQuality() {
        return mCameraController.getVideoQuality();
    }


    /**
     * Sets the JPEG compression quality for image outputs.
     * @param jpegQuality a 0-100 integer.
     */
    public void setJpegQuality(int jpegQuality) {
        if (jpegQuality <= 0 || jpegQuality > 100) {
            throw new IllegalArgumentException("JPEG quality should be > 0 and <= 0");
        }
        mJpegQuality = jpegQuality;
    }


    /**
     * Whether we should crop the picture output to match CameraView aspect ratio.
     * This is only relevant if CameraView dimensions were somehow constrained
     * (e.g. by fixed value or MATCH_PARENT) and do not match internal aspect ratio.
     *
     * Please note that this requires additional computations after the picture is taken.
     *
     * @param cropOutput whether to crop
     */
    public void setCropOutput(boolean cropOutput) {
        this.mCropOutput = cropOutput;
    }


    /**
     * Sets a {@link CameraListener} instance to be notified of all
     * interesting events that will happen during the camera lifecycle.
     *
     * @param cameraListener a listener for events.
     * @deprecated use {@link #addCameraListener(CameraListener)} instead.
     */
    @Deprecated
    public void setCameraListener(CameraListener cameraListener) {
        mCameraCallbacks.clearListeners();
        if (cameraListener != null) {
            mCameraCallbacks.addListener(cameraListener);
        }
    }


    /**
     * Adds a {@link CameraListener} instance to be notified of all
     * interesting events that happen during the camera lifecycle.
     *
     * @param cameraListener a listener for events.
     */
    public void addCameraListener(CameraListener cameraListener) {
        if (cameraListener != null) {
            mCameraCallbacks.addListener(cameraListener);
        }
    }


    /**
     * Remove a {@link CameraListener} that was previously registered.
     *
     * @param cameraListener a listener for events.
     */
    public void removeCameraListener(CameraListener cameraListener) {
        if (cameraListener != null) {
            mCameraCallbacks.removeListener(cameraListener);
        }
    }


    /**
     * Clears the list of {@link CameraListener} that are registered
     * to camera events.
     */
    public void clearCameraListeners() {
        mCameraCallbacks.clearListeners();
    }


    /**
     * Asks the camera to capture an image of the current scene.
     * This will trigger {@link CameraListener#onPictureTaken(byte[])} if a listener
     * was registered.
     *
     * Note that if sessionType is {@link CameraConstants#SESSION_TYPE_VIDEO}, this
     * might fall back to {@link #captureSnapshot()} (that is, we might capture a preview frame).
     *
     * @see #captureSnapshot()
     */
    public void capturePicture() {
        mCameraController.capturePicture();
    }


    /**
     * Asks the camera to capture a snapshot of the current preview.
     * This eventually triggers {@link CameraListener#onPictureTaken(byte[])} if a listener
     * was registered.
     *
     * The difference with {@link #capturePicture()} is that this capture is faster, so it might be
     * better on slower cameras, though the result can be generally blurry or low quality.
     *
     * @see #capturePicture()
     */
    public void captureSnapshot() {
        mCameraController.captureSnapshot();
    }


    /**
     * Starts recording a video with selected options, in a file called
     * "video.mp4" in the default folder.
     * This is discouraged, please use {@link #startCapturingVideo(File)} instead.
     *
     * @deprecated see {@link #startCapturingVideo(File)}
     */
    @Deprecated
    public void startCapturingVideo() {
        startCapturingVideo(null);
    }


    /**
     * Starts recording a video with selected options. Video will be written to the given file,
     * so callers should ensure they have appropriate permissions to write to the file.
     *
     * @param file a file where the video will be saved
     */
    public void startCapturingVideo(File file) {
        if (file == null) {
            file = new File(getContext().getExternalFilesDir(null), "video.mp4");
        }
        if (mCameraController.startVideo(file)) {
            mKeepScreenOn = getKeepScreenOn();
            if (!mKeepScreenOn) setKeepScreenOn(true);
        }
    }


    /**
     * Starts recording a video with selected options. Video will be written to the given file,
     * so callers should ensure they have appropriate permissions to write to the file.
     * Recording will be automatically stopped after durationMillis, unless
     * {@link #stopCapturingVideo()} is not called meanwhile.
     *
     * @param file a file where the video will be saved
     * @param durationMillis video max duration
     *
     * @throws IllegalArgumentException if durationMillis is less than 500 milliseconds
     */
    public void startCapturingVideo(File file, long durationMillis) {
        if (durationMillis < 500) {
            throw new IllegalArgumentException("Video duration can't be < 500 milliseconds");
        }
        startCapturingVideo(file);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                stopCapturingVideo();
            }
        }, durationMillis);
    }


    // TODO: pauseCapturingVideo and resumeCapturingVideo. There is mediarecorder.pause(), but API 24...


    /**
     * Stops capturing video, if there was a video record going on.
     * This will fire {@link CameraListener#onVideoTaken(File)}.
     */
    public void stopCapturingVideo() {
        mCameraController.endVideo();
        if (getKeepScreenOn() != mKeepScreenOn) setKeepScreenOn(mKeepScreenOn);
    }


    /**
     * Returns the size used for the preview,
     * or null if it hasn't been computed (for example if the surface is not ready).
     * @return a Size
     */
    @Nullable
    public Size getPreviewSize() {
        return mCameraController != null ? mCameraController.getPreviewSize() : null;
    }


    /**
     * Returns the size used for the capture,
     * or null if it hasn't been computed yet (for example if the surface is not ready).
     * @return a Size
     */
    @Nullable
    public Size getCaptureSize() {
        return mCameraController != null ? mCameraController.getCaptureSize() : null;
    }


    /**
     * Returns the size used for capturing snapshots.
     * This is equal to {@link #getPreviewSize()}.
     *
     * @return a Size
     */
    @Nullable
    public Size getSnapshotSize() {
        return getPreviewSize();
    }


    // If we end up here, we're in M.
    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions(boolean requestCamera, boolean requestAudio) {
        Activity activity = null;
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                activity = (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }

        List<String> permissions = new ArrayList<>();
        if (requestCamera) permissions.add(Manifest.permission.CAMERA);
        if (requestAudio) permissions.add(Manifest.permission.RECORD_AUDIO);
        if (activity != null) {
            activity.requestPermissions(permissions.toArray(new String[permissions.size()]),
                    PERMISSION_REQUEST_CODE);
        }
    }


    // Callbacks and dispatch
    // ----------------------


    class CameraCallbacks {

        private ArrayList<CameraListener> mListeners;
        private Handler uiHandler;


        CameraCallbacks() {
            mListeners = new ArrayList<>(2);
            uiHandler = new Handler(Looper.getMainLooper());
        }


        public void dispatchOnCameraOpened(final CameraOptions options) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onCameraOpened(options);
                    }
                }
            });
        }


        public void dispatchOnCameraClosed() {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onCameraClosed();
                    }
                }
            });
        }


        public void onCameraPreviewSizeChanged() {
            // Camera preview size, as returned by getPreviewSize(), has changed.
            // Request a layout pass for onMeasure() to do its stuff.
            // Potentially this will change CameraView size, which changes Surface size,
            // which triggers a new Preview size. But hopefully it will converge.
            requestLayout();
        }


        /**
         * What would be great here is to ensure the EXIF tag in the jpeg is consistent with what we expect,
         * and maybe add flipping when we have been using the front camera.
         * Unfortunately this is not easy, because
         * - You can't write EXIF data to a byte[] array, not with support library at least
         * - You don't know what byte[] is, see {@link android.hardware.Camera.Parameters#setRotation(int)}.
         *   Sometimes our rotation is encoded in the byte array, sometimes a rotated byte[] is returned.
         *   Depends on the hardware.
         *
         * So for now we ignore flipping.
         *
         * @param consistentWithView is the final image (decoded respecting EXIF data) consistent with
         *                           the view width and height? Or should we flip dimensions to have a
         *                           consistent measure?
         * @param flipHorizontally whether this picture should be flipped horizontally after decoding,
         *                         because it was taken with the front camera.
         */
        public void processImage(final byte[] jpeg, final boolean consistentWithView, final boolean flipHorizontally) {
            getWorkerHandler().post(new Runnable() {
                @Override
                public void run() {
                    byte[] jpeg2 = jpeg;
                    if (mCropOutput && mPreviewImpl.isCropping()) {
                        // If consistent, dimensions of the jpeg Bitmap and dimensions of getWidth(), getHeight()
                        // Live in the same reference system.
                        int w = consistentWithView ? getWidth() : getHeight();
                        int h = consistentWithView ? getHeight() : getWidth();
                        AspectRatio targetRatio = AspectRatio.of(w, h);
                        Log.e(TAG, "is Consistent? " + consistentWithView);
                        Log.e(TAG, "viewWidth? " + getWidth() + ", viewHeight? " + getHeight());
                        jpeg2 = CropHelper.cropToJpeg(jpeg, targetRatio, mJpegQuality);
                    }
                    dispatchOnPictureTaken(jpeg2);
                }
            });
        }


        public void processSnapshot(YuvImage yuv, boolean consistentWithView, boolean flipHorizontally) {
            byte[] jpeg;
            if (mCropOutput && mPreviewImpl.isCropping()) {
                int w = consistentWithView ? getWidth() : getHeight();
                int h = consistentWithView ? getHeight() : getWidth();
                AspectRatio targetRatio = AspectRatio.of(w, h);
                jpeg = CropHelper.cropToJpeg(yuv, targetRatio, mJpegQuality);
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, yuv.getWidth(), yuv.getHeight()), mJpegQuality, out);
                jpeg = out.toByteArray();
            }
            dispatchOnPictureTaken(jpeg);
        }


        private void dispatchOnPictureTaken(byte[] jpeg) {
            final byte[] data = jpeg;
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onPictureTaken(data);
                    }
                }
            });
        }


        public void dispatchOnVideoTaken(final File video) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onVideoTaken(video);
                    }
                }
            });
        }


        public void dispatchOnFocusStart(@Nullable final Gesture gesture, final PointF point) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (gesture != null && mGestureMap.get(gesture) == GESTURE_ACTION_FOCUS_WITH_MARKER) {
                        mTapGestureLayout.onFocusStart(point);
                    }

                    for (CameraListener listener : mListeners) {
                        listener.onFocusStart(point);
                    }
                }
            });
        }


        public void dispatchOnFocusEnd(@Nullable final Gesture gesture, final boolean success,
                                       final PointF point) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (gesture != null && mGestureMap.get(gesture) == GESTURE_ACTION_FOCUS_WITH_MARKER) {
                        mTapGestureLayout.onFocusEnd(success);
                    }

                    for (CameraListener listener : mListeners) {
                        listener.onFocusEnd(success, point);
                    }
                }
            });
        }


        public void dispatchOnZoomChanged(final float newValue, final PointF[] fingers) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onZoomChanged(newValue, new float[]{0, 1}, fingers);
                    }
                }
            });
        }


        public void dispatchOnExposureCorrectionChanged(final float newValue,
                                                        final float[] bounds,
                                                        final PointF[] fingers) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onExposureCorrectionChanged(newValue, bounds, fingers);
                    }
                }
            });
        }


        private void addListener(@NonNull CameraListener cameraListener) {
            mListeners.add(cameraListener);
        }


        private void removeListener(@NonNull CameraListener cameraListener) {
            mListeners.remove(cameraListener);
        }


        private void clearListeners() {
            mListeners.clear();
        }
    }


    // Deprecated stuff
    // ----------------


    /**
     * This does nothing.
     * @deprecated
     * @param focus no-op
     */
    @Deprecated
    public void setFocus(@Focus int focus) {
    }


    /**
     * This does nothing.
     * @return no-op
     * @deprecated
     */
    @Deprecated
    @Focus
    public int getFocus() {
        return FOCUS_FIXED;
    }


    /**
     * This does nothing.
     * @deprecated
     * @param method no-op
     */
    @Deprecated
    public void setCaptureMethod(@Method int method) {}


    /**
     * This does nothing.
     * @deprecated
     * @param permissions no-op
     */
    @Deprecated
    public void setPermissionPolicy(@Permissions int permissions) {}


    /**
     * Sets the zoom mode for the current session.
     *
     * @see CameraConstants#ZOOM_OFF
     * @see CameraConstants#ZOOM_PINCH
     *
     * @param zoom no-op
     * @deprecated use {@link #mapGesture(Gesture, int)} to map zoom control to gestures
     */
    @Deprecated
    public void setZoomMode(@ZoomMode int zoom) {
    }


    /**
     * Gets the current zoom mode.
     * @return no-op
     * @deprecated use {@link #mapGesture(Gesture, int)} to map zoom control to gestures
     */
    @ZoomMode
    @Deprecated
    public int getZoomMode() {
        return ZOOM_OFF;
    }

}

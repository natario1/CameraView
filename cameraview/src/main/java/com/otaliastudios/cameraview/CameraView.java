package com.otaliastudios.cameraview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.location.Location;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;


public class CameraView extends FrameLayout implements LifecycleObserver {

    private final static String TAG = CameraView.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    public final static int PERMISSION_REQUEST_CODE = 16;

    final static boolean DEFAULT_PLAY_SOUNDS = true;

    // Self managed parameters
    private boolean mPlaySounds;
    private HashMap<Gesture, GestureAction> mGestureMap = new HashMap<>(4);
    private Preview mPreview;

    // Components
    /* for tests */ CameraCallbacks mCameraCallbacks;
    private CameraPreview mCameraPreview;
    private OrientationHelper mOrientationHelper;
    private CameraController mCameraController;
    private MediaActionSound mSound;
    /* for tests */ List<CameraListener> mListeners = new CopyOnWriteArrayList<>();
    /* for tests */ List<FrameProcessor> mFrameProcessors = new CopyOnWriteArrayList<>();
    private Lifecycle mLifecycle;

    // Views
    GridLinesLayout mGridLinesLayout;
    PinchGestureLayout mPinchGestureLayout;
    TapGestureLayout mTapGestureLayout;
    ScrollGestureLayout mScrollGestureLayout;
    private boolean mKeepScreenOn;
    private boolean mExperimental;

    // Threading
    private Handler mUiHandler;
    private WorkerHandler mFrameProcessorsHandler;

    public CameraView(@NonNull Context context) {
        super(context, null);
        init(context, null);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    //region Init

    @SuppressWarnings("WrongConstant")
    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        setWillNotDraw(false);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CameraView, 0, 0);

        // Self managed
        boolean playSounds = a.getBoolean(R.styleable.CameraView_cameraPlaySounds, DEFAULT_PLAY_SOUNDS);
        mExperimental = a.getBoolean(R.styleable.CameraView_cameraExperimental, false);
        mPreview = Preview.fromValue(a.getInteger(R.styleable.CameraView_cameraPreview, Preview.DEFAULT.value()));

        // Camera controller params
        Facing facing = Facing.fromValue(a.getInteger(R.styleable.CameraView_cameraFacing, Facing.DEFAULT(context).value()));
        Flash flash = Flash.fromValue(a.getInteger(R.styleable.CameraView_cameraFlash, Flash.DEFAULT.value()));
        Grid grid = Grid.fromValue(a.getInteger(R.styleable.CameraView_cameraGrid, Grid.DEFAULT.value()));
        int gridColor = a.getColor(R.styleable.CameraView_cameraGrid, GridLinesLayout.DEFAULT_COLOR);
        WhiteBalance whiteBalance = WhiteBalance.fromValue(a.getInteger(R.styleable.CameraView_cameraWhiteBalance, WhiteBalance.DEFAULT.value()));
        Mode mode = Mode.fromValue(a.getInteger(R.styleable.CameraView_cameraMode, Mode.DEFAULT.value()));
        Hdr hdr = Hdr.fromValue(a.getInteger(R.styleable.CameraView_cameraHdr, Hdr.DEFAULT.value()));
        Audio audio = Audio.fromValue(a.getInteger(R.styleable.CameraView_cameraAudio, Audio.DEFAULT.value()));
        VideoCodec codec = VideoCodec.fromValue(a.getInteger(R.styleable.CameraView_cameraVideoCodec, VideoCodec.DEFAULT.value()));
        long videoMaxSize = (long) a.getFloat(R.styleable.CameraView_cameraVideoMaxSize, 0);
        int videoMaxDuration = a.getInteger(R.styleable.CameraView_cameraVideoMaxDuration, 0);
        int videoBitRate = a.getInteger(R.styleable.CameraView_cameraVideoBitRate, 0);
        int audioBitRate = a.getInteger(R.styleable.CameraView_cameraAudioBitRate, 0);

        // Picture size selector
        List<SizeSelector> pictureConstraints = new ArrayList<>(3);
        if (a.hasValue(R.styleable.CameraView_cameraPictureSizeMinWidth)) {
            pictureConstraints.add(SizeSelectors.minWidth(a.getInteger(R.styleable.CameraView_cameraPictureSizeMinWidth, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraPictureSizeMaxWidth)) {
            pictureConstraints.add(SizeSelectors.maxWidth(a.getInteger(R.styleable.CameraView_cameraPictureSizeMaxWidth, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraPictureSizeMinHeight)) {
            pictureConstraints.add(SizeSelectors.minHeight(a.getInteger(R.styleable.CameraView_cameraPictureSizeMinHeight, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraPictureSizeMaxHeight)) {
            pictureConstraints.add(SizeSelectors.maxHeight(a.getInteger(R.styleable.CameraView_cameraPictureSizeMaxHeight, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraPictureSizeMinArea)) {
            pictureConstraints.add(SizeSelectors.minArea(a.getInteger(R.styleable.CameraView_cameraPictureSizeMinArea, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraPictureSizeMaxArea)) {
            pictureConstraints.add(SizeSelectors.maxArea(a.getInteger(R.styleable.CameraView_cameraPictureSizeMaxArea, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraPictureSizeAspectRatio)) {
            //noinspection ConstantConditions
            pictureConstraints.add(SizeSelectors.aspectRatio(AspectRatio.parse(a.getString(R.styleable.CameraView_cameraPictureSizeAspectRatio)), 0));
        }

        if (a.getBoolean(R.styleable.CameraView_cameraPictureSizeSmallest, false)) pictureConstraints.add(SizeSelectors.smallest());
        if (a.getBoolean(R.styleable.CameraView_cameraPictureSizeBiggest, false)) pictureConstraints.add(SizeSelectors.biggest());
        SizeSelector pictureSelector = !pictureConstraints.isEmpty() ?
                SizeSelectors.and(pictureConstraints.toArray(new SizeSelector[0])) :
                SizeSelectors.biggest();

        // Video size selector
        List<SizeSelector> videoConstraints = new ArrayList<>(3);
        if (a.hasValue(R.styleable.CameraView_cameraVideoSizeMinWidth)) {
            videoConstraints.add(SizeSelectors.minWidth(a.getInteger(R.styleable.CameraView_cameraVideoSizeMinWidth, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraVideoSizeMaxWidth)) {
            videoConstraints.add(SizeSelectors.maxWidth(a.getInteger(R.styleable.CameraView_cameraVideoSizeMaxWidth, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraVideoSizeMinHeight)) {
            videoConstraints.add(SizeSelectors.minHeight(a.getInteger(R.styleable.CameraView_cameraVideoSizeMinHeight, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraVideoSizeMaxHeight)) {
            videoConstraints.add(SizeSelectors.maxHeight(a.getInteger(R.styleable.CameraView_cameraVideoSizeMaxHeight, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraVideoSizeMinArea)) {
            videoConstraints.add(SizeSelectors.minArea(a.getInteger(R.styleable.CameraView_cameraVideoSizeMinArea, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraVideoSizeMaxArea)) {
            videoConstraints.add(SizeSelectors.maxArea(a.getInteger(R.styleable.CameraView_cameraVideoSizeMaxArea, 0)));
        }
        if (a.hasValue(R.styleable.CameraView_cameraVideoSizeAspectRatio)) {
            //noinspection ConstantConditions
            videoConstraints.add(SizeSelectors.aspectRatio(AspectRatio.parse(a.getString(R.styleable.CameraView_cameraVideoSizeAspectRatio)), 0));
        }
        if (a.getBoolean(R.styleable.CameraView_cameraVideoSizeSmallest, false)) videoConstraints.add(SizeSelectors.smallest());
        if (a.getBoolean(R.styleable.CameraView_cameraVideoSizeBiggest, false)) videoConstraints.add(SizeSelectors.biggest());
        SizeSelector videoSelector = !videoConstraints.isEmpty() ?
                SizeSelectors.and(videoConstraints.toArray(new SizeSelector[0])) :
                SizeSelectors.biggest();

        // Gestures
        GestureAction tapGesture = GestureAction.fromValue(a.getInteger(R.styleable.CameraView_cameraGestureTap, GestureAction.DEFAULT_TAP.value()));
        GestureAction longTapGesture = GestureAction.fromValue(a.getInteger(R.styleable.CameraView_cameraGestureLongTap, GestureAction.DEFAULT_LONG_TAP.value()));
        GestureAction pinchGesture = GestureAction.fromValue(a.getInteger(R.styleable.CameraView_cameraGesturePinch, GestureAction.DEFAULT_PINCH.value()));
        GestureAction scrollHorizontalGesture = GestureAction.fromValue(a.getInteger(R.styleable.CameraView_cameraGestureScrollHorizontal, GestureAction.DEFAULT_SCROLL_HORIZONTAL.value()));
        GestureAction scrollVerticalGesture = GestureAction.fromValue(a.getInteger(R.styleable.CameraView_cameraGestureScrollVertical, GestureAction.DEFAULT_SCROLL_VERTICAL.value()));

        a.recycle();

        // Components
        mCameraCallbacks = new Callbacks();
        mCameraController = instantiateCameraController(mCameraCallbacks);
        mUiHandler = new Handler(Looper.getMainLooper());
        mFrameProcessorsHandler = WorkerHandler.get("FrameProcessorsWorker");

        // Views
        mGridLinesLayout = new GridLinesLayout(context);
        mPinchGestureLayout = new PinchGestureLayout(context);
        mTapGestureLayout = new TapGestureLayout(context);
        mScrollGestureLayout = new ScrollGestureLayout(context);
        addView(mGridLinesLayout);
        addView(mPinchGestureLayout);
        addView(mTapGestureLayout);
        addView(mScrollGestureLayout);

        // Apply self managed
        setPlaySounds(playSounds);

        // Apply camera controller params
        setFacing(facing);
        setFlash(flash);
        setMode(mode);
        setWhiteBalance(whiteBalance);
        setGrid(grid);
        setGridColor(gridColor);
        setHdr(hdr);
        setAudio(audio);
        setAudioBitRate(audioBitRate);
        setPictureSize(pictureSelector);
        setVideoSize(videoSelector);
        setVideoCodec(codec);
        setVideoMaxSize(videoMaxSize);
        setVideoMaxDuration(videoMaxDuration);
        setVideoBitRate(videoBitRate);

        // Apply gestures
        mapGesture(Gesture.TAP, tapGesture);
        mapGesture(Gesture.LONG_TAP, longTapGesture);
        mapGesture(Gesture.PINCH, pinchGesture);
        mapGesture(Gesture.SCROLL_HORIZONTAL, scrollHorizontalGesture);
        mapGesture(Gesture.SCROLL_VERTICAL, scrollVerticalGesture);

        if (!isInEditMode()) {
            mOrientationHelper = new OrientationHelper(context, mCameraCallbacks);
        }
    }

    protected CameraController instantiateCameraController(CameraCallbacks callbacks) {
        return new Camera1(callbacks);
    }

    protected CameraPreview instantiatePreview(Context context, ViewGroup container) {
        LOG.w("preview:", "isHardwareAccelerated:", isHardwareAccelerated());
        switch (mPreview) {
            case SURFACE:
                return new SurfaceCameraPreview(context, container, null);
            case TEXTURE: {
                if (isHardwareAccelerated()) {
                    // TextureView is not supported without hardware acceleration.
                    return new TextureCameraPreview(context, container, null);
                }
            }
            case GL_SURFACE: default: {
                mPreview = Preview.GL_SURFACE;
                return new GlCameraPreview(context, container, null);
            }
        }
    }

    /* for tests */ void instantiatePreview() {
        mCameraPreview = instantiatePreview(getContext(), this);
        mCameraController.setPreview(mCameraPreview);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mCameraPreview == null) {
            // isHardwareAccelerated will return the real value only after we are
            // attached. That's why we instantiate the preview here.
            instantiatePreview();
        }
        if (!isInEditMode()) {
            mOrientationHelper.enable(getContext());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            mOrientationHelper.disable();
        }
        super.onDetachedFromWindow();
    }

    //endregion

    //region Measuring behavior

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
     * parent no matter the preview. Thanks to what happens in {@link CameraPreview}, this acts like
     * a CENTER CROP scale type.
     *
     * When both dimensions are WRAP_CONTENT, CameraView will take the biggest dimensions that
     * fit the preview aspect ratio. This acts like a CENTER INSIDE scale type.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Size previewSize = mCameraController.getPreviewSize(CameraController.REF_VIEW);
        if (previewSize == null) {
            LOG.w("onMeasure:", "surface is not ready. Calling default behavior.");
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // Let's which dimensions need to be adapted.
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthValue = MeasureSpec.getSize(widthMeasureSpec);
        final int heightValue = MeasureSpec.getSize(heightMeasureSpec);
        final float previewWidth = previewSize.getWidth();
        final float previewHeight = previewSize.getHeight();

        // Pre-process specs
        final ViewGroup.LayoutParams lp = getLayoutParams();
        if (!mCameraPreview.supportsCropping()) {
            // We can't allow EXACTLY constraints in this case.
            if (widthMode == EXACTLY) widthMode = AT_MOST;
            if (heightMode == EXACTLY) heightMode = AT_MOST;
        } else {
            // If MATCH_PARENT is interpreted as AT_MOST, transform to EXACTLY
            // to be consistent with our semantics (and our docs).
            if (widthMode == AT_MOST && lp.width == MATCH_PARENT) widthMode = EXACTLY;
            if (heightMode == AT_MOST && lp.height == MATCH_PARENT) heightMode = EXACTLY;
        }

        LOG.i("onMeasure:", "requested dimensions are", "(" + widthValue + "[" + ms(widthMode) + "]x" +
                heightValue + "[" + ms(heightMode) + "])");
        LOG.i("onMeasure:",  "previewSize is", "(" + previewWidth + "x" + previewHeight + ")");

        // (1) If we have fixed dimensions (either 300dp or MATCH_PARENT), there's nothing we should do,
        // other than respect it. The preview will eventually be cropped at the sides (by PreviewImpl scaling)
        // except the case in which these fixed dimensions manage to fit exactly the preview aspect ratio.
        if (widthMode == EXACTLY && heightMode == EXACTLY) {
            LOG.w("onMeasure:", "both are MATCH_PARENT or fixed value. We adapt.",
                    "This means CROP_CENTER.", "(" + widthValue + "x" + heightValue + ")");
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // (2) If both dimensions are free, with no limits, then our size will be exactly the
        // preview size. This can happen rarely, for example in 2d scrollable containers.
        if (widthMode == UNSPECIFIED && heightMode == UNSPECIFIED) {
            LOG.i("onMeasure:", "both are completely free.",
                    "We respect that and extend to the whole preview size.",
                    "(" + previewWidth + "x" + previewHeight + ")");
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec((int) previewWidth, EXACTLY),
                    MeasureSpec.makeMeasureSpec((int) previewHeight, EXACTLY));
            return;
        }

        // It's sure now that at least one dimension can be determined (either because EXACTLY or AT_MOST).
        // This starts to seem a pleasant situation.

        // (3) If one of the dimension is completely free (e.g. in a scrollable container),
        // take the other and fit the ratio.
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
            LOG.i("onMeasure:", "one dimension was free, we adapted it to fit the aspect ratio.",
                    "(" + width + "x" + height + ")");
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, EXACTLY));
            return;
        }

        // (4) At this point both dimensions are either AT_MOST-AT_MOST, EXACTLY-AT_MOST or AT_MOST-EXACTLY.
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
            LOG.i("onMeasure:", "one dimension was EXACTLY, another AT_MOST.",
                    "We have TRIED to fit the aspect ratio, but it's not guaranteed.",
                    "(" + width + "x" + height + ")");
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, EXACTLY));
            return;
        }

        // (5) Last case, AT_MOST and AT_MOST. Here we can SURELY fit the aspect ratio by
        // filling one dimension and adapting the other.
        int height, width;
        float atMostRatio = (float) heightValue / (float) widthValue;
        if (atMostRatio >= ratio) {
            // We must reduce height.
            width = widthValue;
            height = (int) (width * ratio);
        } else {
            height = heightValue;
            width = (int) (height / ratio);
        }
        LOG.i("onMeasure:", "both dimension were AT_MOST.",
                "We fit the preview aspect ratio.",
                "(" + width + "x" + height + ")");
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, EXACTLY),
                MeasureSpec.makeMeasureSpec(height, EXACTLY));
    }

    //endregion

    //region Gesture APIs

    /**
     * Maps a {@link Gesture} to a certain gesture action.
     * For example, you can assign zoom control to the pinch gesture by just calling:
     * <code>
     *     cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
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
    public boolean mapGesture(@NonNull Gesture gesture, @NonNull GestureAction action) {
        GestureAction none = GestureAction.NONE;
        if (gesture.isAssignableTo(action)) {
            mGestureMap.put(gesture, action);
            switch (gesture) {
                case PINCH:
                    mPinchGestureLayout.enable(mGestureMap.get(Gesture.PINCH) != none);
                    break;
                case TAP:
                // case DOUBLE_TAP:
                case LONG_TAP:
                    mTapGestureLayout.enable(
                            mGestureMap.get(Gesture.TAP) != none ||
                            // mGestureMap.get(Gesture.DOUBLE_TAP) != none ||
                            mGestureMap.get(Gesture.LONG_TAP) != none);
                    break;
                case SCROLL_HORIZONTAL:
                case SCROLL_VERTICAL:
                    mScrollGestureLayout.enable(
                            mGestureMap.get(Gesture.SCROLL_HORIZONTAL) != none ||
                            mGestureMap.get(Gesture.SCROLL_VERTICAL) != none);
                    break;
            }
            return true;
        }
        mapGesture(gesture, none);
        return false;
    }


    /**
     * Clears any action mapped to the given gesture.
     * @param gesture which gesture to clear
     */
    public void clearGesture(@NonNull Gesture gesture) {
        mapGesture(gesture, GestureAction.NONE);
    }


    /**
     * Returns the action currently mapped to the given gesture.
     *
     * @param gesture which gesture to inspect
     * @return mapped action
     */
    @NonNull
    public GestureAction getGestureAction(@NonNull Gesture gesture) {
        return mGestureMap.get(gesture);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true; // Steal our own events.
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isOpened()) return true;

        // Pass to our own GestureLayouts
        CameraOptions options = mCameraController.getCameraOptions(); // Non null
        if (mPinchGestureLayout.onTouchEvent(event)) {
            LOG.i("onTouchEvent", "pinch!");
            onGesture(mPinchGestureLayout, options);
        } else if (mScrollGestureLayout.onTouchEvent(event)) {
            LOG.i("onTouchEvent", "scroll!");
            onGesture(mScrollGestureLayout, options);
        } else if (mTapGestureLayout.onTouchEvent(event)) {
            LOG.i("onTouchEvent", "tap!");
            onGesture(mTapGestureLayout, options);
        }
        return true;
    }


    // Some gesture layout detected a gesture. It's not known at this moment:
    // (1) if it was mapped to some action (we check here)
    // (2) if it's supported by the camera (CameraController checks)
    private void onGesture(GestureLayout source, @NonNull CameraOptions options) {
        Gesture gesture = source.getGestureType();
        GestureAction action = mGestureMap.get(gesture);
        PointF[] points = source.getPoints();
        float oldValue, newValue;
        switch (action) {

            case CAPTURE:
                mCameraController.takePicture();
                break;

            case FOCUS:
            case FOCUS_WITH_MARKER:
                mCameraController.startAutoFocus(gesture, points[0]);
                break;

            case ZOOM:
                oldValue = mCameraController.getZoomValue();
                newValue = source.scaleValue(oldValue, 0, 1);
                if (newValue != oldValue) {
                    mCameraController.setZoom(newValue, points, true);
                }
                break;

            case EXPOSURE_CORRECTION:
                oldValue = mCameraController.getExposureCorrectionValue();
                float minValue = options.getExposureCorrectionMinValue();
                float maxValue = options.getExposureCorrectionMaxValue();
                newValue = source.scaleValue(oldValue, minValue, maxValue);
                if (newValue != oldValue) {
                    float[] bounds = new float[]{minValue, maxValue};
                    mCameraController.setExposureCorrection(newValue, bounds, points, true);
                }
                break;
        }
    }

    //endregion

    //region Lifecycle APIs

    /**
     * Returns whether the camera has started showing its preview.
     * @return whether the camera has started
     */
    public boolean isOpened() {
        return mCameraController.getState() >= CameraController.STATE_STARTED;
    }

    private boolean isClosed() {
        return mCameraController.getState() == CameraController.STATE_STOPPED;
    }

    /**
     * Sets the lifecycle owner for this view. This means you don't need
     * to call {@link #open()}, {@link #close()} or {@link #destroy()} at all.
     *
     * @param owner the owner activity or fragment
     */
    public void setLifecycleOwner(@NonNull LifecycleOwner owner) {
        if (mLifecycle != null) mLifecycle.removeObserver(this);
        mLifecycle = owner.getLifecycle();
        mLifecycle.addObserver(this);
    }


    /**
     * Starts the camera preview, if not started already.
     * This should be called onResume(), or when you are ready with permissions.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void open() {
        if (!isEnabled()) return;
        if (mCameraPreview != null) mCameraPreview.onResume();

        if (checkPermissions(getMode(), getAudio())) {
            // Update display orientation for current CameraController
            mOrientationHelper.enable(getContext());
            mCameraController.setDisplayOffset(mOrientationHelper.getDisplayOffset());
            mCameraController.start();
        }
    }


    /**
     * Checks that we have appropriate permissions for this session type.
     * Throws if session = audio and manifest did not add the microphone permissions.     
     * @param mode the sessionType to be checked
     * @param audio the audio setting to be checked
     * @return true if we can go on, false otherwise.
     */
    @SuppressLint("NewApi")
    protected boolean checkPermissions(@NonNull Mode mode, @NonNull Audio audio) {
        checkPermissionsManifestOrThrow(mode, audio);
        // Manifest is OK at this point. Let's check runtime permissions.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        Context c = getContext();
        boolean needsCamera = true;
        boolean needsAudio = audio == Audio.ON;

        needsCamera = needsCamera && c.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
        needsAudio = needsAudio && c.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;

        if (needsCamera || needsAudio) {
            requestPermissions(needsCamera, needsAudio);
            return false;
        }
        return true;
    }


    /**
     * If mSessionType == SESSION_TYPE_VIDEO we will ask for RECORD_AUDIO permission.
     * If the developer did not add this to its manifest, throw and fire warnings.
     * (Hoping this is not caught elsewhere... we should test).
     */
    private void checkPermissionsManifestOrThrow(@NonNull Mode mode, @NonNull Audio audio) {
        if (mode == Mode.VIDEO && audio == Audio.ON) {
            try {
                PackageManager manager = getContext().getPackageManager();
                PackageInfo info = manager.getPackageInfo(getContext().getPackageName(), PackageManager.GET_PERMISSIONS);
                for (String requestedPermission : info.requestedPermissions) {
                    if (requestedPermission.equals(Manifest.permission.RECORD_AUDIO)) {
                        return;
                    }
                }
                LOG.e("Permission error:", "When the session type is set to video,",
                        "the RECORD_AUDIO permission should be added to the app manifest file.");
                throw new IllegalStateException(CameraLogger.lastMessage);
            } catch (PackageManager.NameNotFoundException e) {
                // Not possible.
            }
        }
    }


    /**
     * Stops the current preview, if any was started.
     * This should be called onPause().
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void close() {
        mCameraController.stop();
        if (mCameraPreview != null) mCameraPreview.onPause();
    }


    /**
     * Destroys this instance, releasing immediately
     * the camera resource.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroy() {
        clearCameraListeners();
        clearFrameProcessors();
        mCameraController.destroy();
        if (mCameraPreview != null) mCameraPreview.onDestroy();
    }

    //endregion

    //region Public APIs for controls


    /**
     * Shorthand for the appropriate set* method.
     * For example, if control is a {@link Grid}, this calls {@link #setGrid(Grid)}.
     *
     * @param control desired value
     */
    public void set(@NonNull Control control) {
        if (control instanceof Audio) {
            setAudio((Audio) control);
        } else if (control instanceof Facing) {
            setFacing((Facing) control);
        } else if (control instanceof Flash) {
            setFlash((Flash) control);
        } else if (control instanceof Grid) {
            setGrid((Grid) control);
        } else if (control instanceof Hdr) {
            setHdr((Hdr) control);
        } else if (control instanceof Mode) {
            setMode((Mode) control);
        } else if (control instanceof WhiteBalance) {
            setWhiteBalance((WhiteBalance) control);
        } else if (control instanceof VideoCodec) {
            setVideoCodec((VideoCodec) control);
        } else if (control instanceof Preview) {
            setPreview((Preview) control);
        }
    }


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
            float[] bounds = new float[]{min, max};
            mCameraController.setExposureCorrection(EVvalue, bounds, null, false);
        }
    }


    /**
     * Returns the current exposure correction value, typically 0
     * at start-up.
     * @return the current exposure correction value
     */
    public float getExposureCorrection() {
        return mCameraController.getExposureCorrectionValue();
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
        mCameraController.setZoom(zoom, null, false);
    }


    /**
     * Returns the current zoom value, something between 0 and 1.
     * @return the current zoom value
     */
    public float getZoom() {
        return mCameraController.getZoomValue();
    }


    /**
     * Controls the grids to be drawn over the current layout.
     *
     * @see Grid#OFF
     * @see Grid#DRAW_3X3
     * @see Grid#DRAW_4X4
     * @see Grid#DRAW_PHI
     *
     * @param gridMode desired grid mode
     */
    public void setGrid(@NonNull Grid gridMode) {
        mGridLinesLayout.setGridMode(gridMode);
    }


    /**
     * Gets the current grid mode.
     * @return the current grid mode
     */
    @NonNull
    public Grid getGrid() {
        return mGridLinesLayout.getGridMode();
    }


    /**
     * Controls the color of the grid lines that will be drawn
     * over the current layout.
     *
     * @param color a resolved color
     */
    public void setGridColor(@ColorInt int color) {
        mGridLinesLayout.setGridColor(color);
    }

    /**
     * Returns the current grid color.
     * @return the current grid color
     */
    public int getGridColor() {
        return mGridLinesLayout.getGridColor();
    }

    /**
     * Controls the grids to be drawn over the current layout.
     *
     * @see Hdr#OFF
     * @see Hdr#ON
     *
     * @param hdr desired hdr value
     */
    public void setHdr(@NonNull Hdr hdr) {
        mCameraController.setHdr(hdr);
    }


    /**
     * Controls the preview engine. Should only be called
     * if this CameraView was never added to any window
     * (like if you created it programmatically).
     * Otherwise, it has no effect.
     *
     * @see Preview#SURFACE
     * @see Preview#TEXTURE
     * @see Preview#GL_SURFACE
     *
     * @param preview desired preview engine
     */
    public void setPreview(@NonNull Preview preview) {
        mPreview = preview;
    }


    /**
     * Gets the current hdr value.
     * @return the current hdr value
     */
    @NonNull
    public Hdr getHdr() {
        return mCameraController.getHdr();
    }


    /**
     * Set location coordinates to be found later in the EXIF header
     *
     * @param latitude current latitude
     * @param longitude current longitude
     */
    public void setLocation(double latitude, double longitude) {
        Location location = new Location("Unknown");
        location.setTime(System.currentTimeMillis());
        location.setAltitude(0);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        mCameraController.setLocation(location);
    }


    /**
     * Set location values to be found later in the EXIF header
     *
     * @param location current location
     */
    public void setLocation(@Nullable Location location) {
        mCameraController.setLocation(location);
    }


    /**
     * Retrieves the location previously applied with setLocation().
     *
     * @return the current location, if any.
     */
    @Nullable
    public Location getLocation() {
        return mCameraController.getLocation();
    }


    /**
     * Sets desired white balance to current camera session.
     *
     * @see WhiteBalance#AUTO
     * @see WhiteBalance#INCANDESCENT
     * @see WhiteBalance#FLUORESCENT
     * @see WhiteBalance#DAYLIGHT
     * @see WhiteBalance#CLOUDY
     *
     * @param whiteBalance desired white balance behavior.
     */
    public void setWhiteBalance(@NonNull WhiteBalance whiteBalance) {
        mCameraController.setWhiteBalance(whiteBalance);
    }


    /**
     * Returns the current white balance behavior.
     * @return white balance value.
     */
    @NonNull
    public WhiteBalance getWhiteBalance() {
        return mCameraController.getWhiteBalance();
    }


    /**
     * Sets which camera sensor should be used.
     *
     * @see Facing#FRONT
     * @see Facing#BACK
     *
     * @param facing a facing value.
     */
    public void setFacing(@NonNull Facing facing) {
        mCameraController.setFacing(facing);
    }


    /**
     * Gets the facing camera currently being used.
     * @return a facing value.
     */
    @NonNull
    public Facing getFacing() {
        return mCameraController.getFacing();
    }


    /**
     * Toggles the facing value between {@link Facing#BACK}
     * and {@link Facing#FRONT}.
     *
     * @return the new facing value
     */
    public Facing toggleFacing() {
        Facing facing = mCameraController.getFacing();
        switch (facing) {
            case BACK:
                setFacing(Facing.FRONT);
                break;

            case FRONT:
                setFacing(Facing.BACK);
                break;
        }

        return mCameraController.getFacing();
    }


    /**
     * Sets the flash mode.
     *
     * @see Flash#OFF
     * @see Flash#ON
     * @see Flash#AUTO
     * @see Flash#TORCH

     * @param flash desired flash mode.
     */
    public void setFlash(@NonNull Flash flash) {
        mCameraController.setFlash(flash);
    }


    /**
     * Gets the current flash mode.
     * @return a flash mode
     */
    @NonNull
    public Flash getFlash() {
        return mCameraController.getFlash();
    }


    /**
     * Controls the audio mode.
     *
     * @see Audio#OFF
     * @see Audio#ON
     *
     * @param audio desired audio value
     */
    public void setAudio(@NonNull Audio audio) {

        if (audio == getAudio() || isClosed()) {
            // Check did took place, or will happen on start().
            mCameraController.setAudio(audio);

        } else if (checkPermissions(getMode(), audio)) {
            // Camera is running. Pass.
            mCameraController.setAudio(audio);

        } else {
            // This means that the audio permission is being asked.
            // Stop the camera so it can be restarted by the developer onPermissionResult.
            // Developer must also set the audio value again...
            // Not ideal but good for now.
            close();
        }
    }


    /**
     * Gets the current audio value.
     * @return the current audio value
     */
    @NonNull
    public Audio getAudio() {
        return mCameraController.getAudio();
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
     * <strong>ADVANCED FEATURE</strong> - sets a size selector for the preview stream.
     * The {@link SizeSelector} will be invoked with the list of available sizes, and the first
     * acceptable size will be accepted and passed to the internal engine & surface.
     *
     * This is typically NOT NEEDED. The default size selector is already smart enough to respect
     * the picture/video output aspect ratio, and be bigger than the surface so that there is no
     * upscaling. If all you want is set an aspect ratio, use {@link #setPictureSize(SizeSelector)}
     * and {@link #setVideoSize(SizeSelector)}.
     *
     * When size changes, the {@link CameraView} is remeasured so any WRAP_CONTENT dimension
     * is recomputed accordingly.
     *
     * See the {@link SizeSelectors} class for handy utilities for creating selectors.
     *
     * @param selector a size selector
     */
    public void setPreviewSize(@NonNull SizeSelector selector) {
        mCameraController.setPreviewSizeSelector(selector);
    }


    /**
     * Set the current session type to either picture or video.
     *
     * @see Mode#PICTURE
     * @see Mode#VIDEO
     *
     * @param mode desired session type.
     */
    public void setMode(@NonNull Mode mode) {

        if (mode == getMode() || isClosed()) {
            // Check did took place, or will happen on start().
            mCameraController.setMode(mode);

        } else if (checkPermissions(mode, getAudio())) {
            // Camera is running. CameraImpl setMode will do the trick.
            mCameraController.setMode(mode);

        } else {
            // This means that the audio permission is being asked.
            // Stop the camera so it can be restarted by the developer onPermissionResult.
            // Developer must also set the session type again...
            // Not ideal but good for now.
            close();
        }
    }


    /**
     * Gets the current mode.
     * @return the current mode
     */
    @NonNull
    public Mode getMode() {
        return mCameraController.getMode();
    }


    /**
     * Sets a capture size selector for picture mode.
     * The {@link SizeSelector} will be invoked with the list of available sizes, and the first
     * acceptable size will be accepted and passed to the internal engine.
     * See the {@link SizeSelectors} class for handy utilities for creating selectors.
     *
     * @param selector a size selector
     */
    public void setPictureSize(@NonNull SizeSelector selector) {
        mCameraController.setPictureSizeSelector(selector);
    }


    /**
     * Sets a capture size selector for video mode.
     * The {@link SizeSelector} will be invoked with the list of available sizes, and the first
     * acceptable size will be accepted and passed to the internal engine.
     * See the {@link SizeSelectors} class for handy utilities for creating selectors.
     *
     * @param selector a size selector
     */
    public void setVideoSize(@NonNull SizeSelector selector) {
        mCameraController.setVideoSizeSelector(selector);
    }

    /**
     * Sets the bit rate in bits per second for video capturing.
     * Will be used by both {@link #takeVideo(File)} and {@link #takeVideoSnapshot(File)}.
     *
     * @param bitRate desired bit rate
     */
    public void setVideoBitRate(int bitRate) {
        mCameraController.setVideoBitRate(bitRate);
    }

    /**
     * Returns the current video bit rate.
     * @return current bit rate
     */
    public int getVideoBitRate() {
        return mCameraController.getVideoBitRate();
    }

    /**
     * Sets the bit rate in bits per second for audio capturing.
     * Will be used by both {@link #takeVideo(File)} and {@link #takeVideoSnapshot(File)}.
     *
     * @param bitRate desired bit rate
     */
    public void setAudioBitRate(int bitRate) {
        mCameraController.setAudioBitRate(bitRate);
    }

    /**
     * Returns the current audio bit rate.
     * @return current bit rate
     */
    public int getAudioBitRate() {
        return mCameraController.getAudioBitRate();
    }

    /**
     * Adds a {@link CameraListener} instance to be notified of all
     * interesting events that happen during the camera lifecycle.
     *
     * @param cameraListener a listener for events.
     */
    public void addCameraListener(@NonNull CameraListener cameraListener) {
        mListeners.add(cameraListener);
    }


    /**
     * Remove a {@link CameraListener} that was previously registered.
     *
     * @param cameraListener a listener for events.
     */
    public void removeCameraListener(@NonNull CameraListener cameraListener) {
        mListeners.remove(cameraListener);
    }


    /**
     * Clears the list of {@link CameraListener} that are registered
     * to camera events.
     */
    public void clearCameraListeners() {
        mListeners.clear();
    }


    /**
     * Adds a {@link FrameProcessor} instance to be notified of
     * new frames in the preview stream.
     *
     * @param processor a frame processor.
     */
    public void addFrameProcessor(@Nullable FrameProcessor processor) {
        if (processor != null) {
            mFrameProcessors.add(processor);
        }
    }


    /**
     * Remove a {@link FrameProcessor} that was previously registered.
     *
     * @param processor a frame processor
     */
    public void removeFrameProcessor(@Nullable FrameProcessor processor) {
        if (processor != null) {
            mFrameProcessors.remove(processor);
        }
    }


    /**
     * Clears the list of {@link FrameProcessor} that have been registered
     * to preview frames.
     */
    public void clearFrameProcessors() {
        mFrameProcessors.clear();
    }


    /**
     * Asks the camera to capture an image of the current scene.
     * This will trigger {@link CameraListener#onPictureTaken(PictureResult)} if a listener
     * was registered.
     *
     * Note that if sessionType is {@link Mode#VIDEO}, this
     * might fall back to {@link #takePictureSnapshot()} (that is, we might capture a preview frame).
     *
     * @see #takePictureSnapshot()
     */
    public void takePicture() {
        mCameraController.takePicture();
    }


    /**
     * Asks the camera to capture a snapshot of the current preview.
     * This eventually triggers {@link CameraListener#onPictureTaken(PictureResult)} if a listener
     * was registered.
     *
     * The difference with {@link #takePicture()} is that this capture is faster, so it might be
     * better on slower cameras, though the result can be generally blurry or low quality.
     *
     * @see #takePicture()
     */
    public void takePictureSnapshot() {
        if (getWidth() == 0 || getHeight() == 0) return;
        mCameraController.takePictureSnapshot(AspectRatio.of(getWidth(), getHeight()));
    }


    /**
     * Starts recording a video. Video will be written to the given file,
     * so callers should ensure they have appropriate permissions to write to the file.
     *
     * @param file a file where the video will be saved
     */
    public void takeVideo(@NonNull File file) {
        mCameraController.takeVideo(file);
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                mKeepScreenOn = getKeepScreenOn();
                if (!mKeepScreenOn) setKeepScreenOn(true);
            }
        });
    }

    /**
     * Starts recording a fast, low quality video snapshot. Video will be written to the given file,
     * so callers should ensure they have appropriate permissions to write to the file.
     *
     * Throws an exception if API level is below 18, or if the preview being used is not
     * {@link Preview#GL_SURFACE}.
     *
     * @param file a file where the video will be saved
     */
    public void takeVideoSnapshot(@NonNull File file) {
        if (getWidth() == 0 || getHeight() == 0) return;
        mCameraController.takeVideoSnapshot(file, AspectRatio.of(getWidth(), getHeight()));
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                mKeepScreenOn = getKeepScreenOn();
                if (!mKeepScreenOn) setKeepScreenOn(true);
            }
        });
    }


    /**
     * Starts recording a video. Video will be written to the given file,
     * so callers should ensure they have appropriate permissions to write to the file.
     * Recording will be automatically stopped after the given duration, overriding
     * temporarily any duration limit set by {@link #setVideoMaxDuration(int)}.
     *
     * @param file a file where the video will be saved
     * @param durationMillis recording max duration
     *
     */
    public void takeVideo(@Nullable File file, int durationMillis) {
        final int old = getVideoMaxDuration();
        addCameraListener(new CameraListener() {
            @Override
            public void onVideoTaken(@NonNull VideoResult result) {
                setVideoMaxDuration(old);
                removeCameraListener(this);
            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                super.onCameraError(exception);
                if (exception.getReason() == CameraException.REASON_VIDEO_FAILED) {
                    setVideoMaxDuration(old);
                    removeCameraListener(this);
                }
            }
        });
        setVideoMaxDuration(durationMillis);
        takeVideo(file);
    }

    /**
     * Starts recording a fast, low quality video snapshot. Video will be written to the given file,
     * so callers should ensure they have appropriate permissions to write to the file.
     * Recording will be automatically stopped after the given duration, overriding
     * temporarily any duration limit set by {@link #setVideoMaxDuration(int)}.
     *
     * Throws an exception if API level is below 18, or if the preview being used is not
     * {@link Preview#GL_SURFACE}.
     *
     * @param file a file where the video will be saved
     * @param durationMillis recording max duration
     *
     */
    public void takeVideoSnapshot(@Nullable File file, int durationMillis) {
        final int old = getVideoMaxDuration();
        addCameraListener(new CameraListener() {
            @Override
            public void onVideoTaken(@NonNull VideoResult result) {
                setVideoMaxDuration(old);
                removeCameraListener(this);
            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                super.onCameraError(exception);
                if (exception.getReason() == CameraException.REASON_VIDEO_FAILED) {
                    setVideoMaxDuration(old);
                    removeCameraListener(this);
                }
            }
        });
        setVideoMaxDuration(durationMillis);
        takeVideoSnapshot(file);
    }


    // TODO: pauseVideo and resumeVideo? There is mediarecorder.pause(), but API 24...


    /**
     * Stops capturing video or video snapshots being recorded, if there was any.
     * This will fire {@link CameraListener#onVideoTaken(VideoResult)}.
     */
    public void stopVideo() {
        mCameraController.stopVideo();
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (getKeepScreenOn() != mKeepScreenOn) setKeepScreenOn(mKeepScreenOn);
            }
        });
    }


    /**
     * Returns the size used for snapshots, or null if it hasn't been computed
     * (for example if the surface is not ready). This is the preview size, rotated to match
     * the output orientation, and cropped to the visible part.
     *
     * @return the size of snapshots
     */
    @Nullable
    public Size getSnapshotSize() {
        if (getWidth() == 0 || getHeight() == 0) return null;

        // Get the preview size and crop according to the current view size.
        // It's better to do calculations in the REF_VIEW reference, and then flip if needed.
        Size preview = mCameraController.getPreviewSize(CameraController.REF_VIEW);
        AspectRatio viewRatio = AspectRatio.of(getWidth(), getHeight());
        Rect crop = CropHelper.computeCrop(preview, viewRatio);
        Size cropSize = new Size(crop.width(), crop.height());
        if (mCameraController.flip(CameraController.REF_VIEW, CameraController.REF_OUTPUT)) {
            return cropSize.flip();
        } else {
            return cropSize;
        }
    }


    /**
     * Returns the size used for pictures taken with {@link #takePicture()},
     * or null if it hasn't been computed (for example if the surface is not ready),
     * or null if we are in video mode.
     *
     * The size is rotated to match the output orientation.
     *
     * @return the size of pictures
     */
    @Nullable
    public Size getPictureSize() {
        return mCameraController.getPictureSize(CameraController.REF_OUTPUT);
    }


    /**
     * Returns the size used for videos taken with {@link #takeVideo(File)},
     * or null if it hasn't been computed (for example if the surface is not ready),
     * or null if we are in picture mode.
     *
     * The size is rotated to match the output orientation.
     *
     * @return the size of videos
     */
    @Nullable
    public Size getVideoSize() {
        return mCameraController.getVideoSize(CameraController.REF_OUTPUT);
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


    @SuppressLint("NewApi")
    private void playSound(int soundType) {
        if (mPlaySounds) {
            if (mSound == null) mSound = new MediaActionSound();
            mSound.play(soundType);
        }
    }


    /**
     * Controls whether CameraView should play sound effects on certain
     * events (picture taken, focus complete). Note that:
     * - On API level {@literal <} 16, this flag is always false
     * - Camera1 will always play the shutter sound when taking pictures
     *
     * @param playSounds whether to play sound effects
     */
    public void setPlaySounds(boolean playSounds) {
        mPlaySounds = playSounds && Build.VERSION.SDK_INT >= 16;
        mCameraController.setPlaySounds(playSounds);
    }


    /**
     * Gets the current sound effect behavior.
     *
     * @see #setPlaySounds(boolean)
     * @return whether sound effects are supported
     */
    public boolean getPlaySounds() {
        return mPlaySounds;
    }


    /**
     * Sets the encoder for video recordings.
     * Defaults to {@link VideoCodec#DEVICE_DEFAULT}.
     *
     * @see VideoCodec#DEVICE_DEFAULT
     * @see VideoCodec#H_263
     * @see VideoCodec#H_264
     *
     * @param codec requested video codec
     */
    public void setVideoCodec(@NonNull VideoCodec codec) {
        mCameraController.setVideoCodec(codec);
    }


    /**
     * Gets the current encoder for video recordings.
     * @return the current video codec
     */
    @NonNull
    public VideoCodec getVideoCodec() {
        return mCameraController.getVideoCodec();
    }


    /**
     * Sets the maximum size in bytes for recorded video files.
     * Once this size is reached, the recording will automatically stop.
     * Defaults to unlimited size. Use 0 or negatives to disable.
     *
     * @param videoMaxSizeInBytes The maximum video size in bytes
     */
    public void setVideoMaxSize(long videoMaxSizeInBytes) {
        mCameraController.setVideoMaxSize(videoMaxSizeInBytes);
    }


    /**
     * Returns the maximum size in bytes for recorded video files, or 0
     * if no size was set.
     *
     * @see #setVideoMaxSize(long)
     * @return the maximum size in bytes
     */
    public long getVideoMaxSize() {
        return mCameraController.getVideoMaxSize();
    }


    /**
     * Sets the maximum duration in milliseconds for video recordings.
     * Once this duration is reached, the recording will automatically stop.
     * Defaults to unlimited duration. Use 0 or negatives to disable.
     *
     * @param videoMaxDurationMillis The maximum video duration in milliseconds
     */
    public void setVideoMaxDuration(int videoMaxDurationMillis) {
        mCameraController.setVideoMaxDuration(videoMaxDurationMillis);
    }


    /**
     * Returns the maximum duration in milliseconds for video recordings, or 0
     * if no limit was set.
     *
     * @see #setVideoMaxDuration(int)
     * @return the maximum duration in milliseconds
     */
    public int getVideoMaxDuration() {
        return mCameraController.getVideoMaxDuration();
    }


    /**
     * Returns true if the camera is currently recording a video
     * @return boolean indicating if the camera is recording a video
     */
    public boolean isTakingVideo() {
        return mCameraController.isTakingVideo();
    }


    /**
     * Returns true if the camera is currently capturing a picture
     * @return boolean indicating if the camera is capturing a picture
     */
    public boolean isTakingPicture() {
        return mCameraController.isTakingPicture();
    }

    //endregion

    //region Callbacks and dispatching

    interface CameraCallbacks extends OrientationHelper.Callback {
        void dispatchOnCameraOpened(CameraOptions options);
        void dispatchOnCameraClosed();
        void onCameraPreviewSizeChanged();
        void onShutter(boolean shouldPlaySound);
        void dispatchOnVideoTaken(VideoResult result);
        void dispatchOnPictureTaken(PictureResult result);
        void dispatchOnFocusStart(@Nullable Gesture trigger, @NonNull PointF where);
        void dispatchOnFocusEnd(@Nullable Gesture trigger, boolean success, @NonNull PointF where);
        void dispatchOnZoomChanged(final float newValue, @Nullable final PointF[] fingers);
        void dispatchOnExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers);
        void dispatchFrame(Frame frame);
        void dispatchError(CameraException exception);
    }

    private class Callbacks implements CameraCallbacks {

        private CameraLogger mLogger = CameraLogger.create(CameraCallbacks.class.getSimpleName());

        Callbacks() {}

        @Override
        public void dispatchOnCameraOpened(final CameraOptions options) {
            mLogger.i("dispatchOnCameraOpened", options);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onCameraOpened(options);
                    }
                }
            });
        }

        @Override
        public void dispatchOnCameraClosed() {
            mLogger.i("dispatchOnCameraClosed");
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onCameraClosed();
                    }
                }
            });
        }

        @Override
        public void onCameraPreviewSizeChanged() {
            mLogger.i("onCameraPreviewSizeChanged");
            // Camera preview size has changed.
            // Request a layout pass for onMeasure() to do its stuff.
            // Potentially this will change CameraView size, which changes Surface size,
            // which triggers a new Preview size. But hopefully it will converge.
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestLayout();
                }
            });
        }

        @Override
        public void onShutter(boolean shouldPlaySound) {
            if (shouldPlaySound && mPlaySounds) {
                //noinspection all
                playSound(MediaActionSound.SHUTTER_CLICK);
            }
        }

        @Override
        public void dispatchOnPictureTaken(final PictureResult result) {
            mLogger.i("dispatchOnPictureTaken");
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onPictureTaken(result);
                    }
                }
            });
        }

        @Override
        public void dispatchOnVideoTaken(final VideoResult video) {
            mLogger.i("dispatchOnVideoTaken", video);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onVideoTaken(video);
                    }
                }
            });
        }

        @Override
        public void dispatchOnFocusStart(@Nullable final Gesture gesture, @NonNull final PointF point) {
            mLogger.i("dispatchOnFocusStart", gesture, point);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (gesture != null && mGestureMap.get(gesture) == GestureAction.FOCUS_WITH_MARKER) {
                        mTapGestureLayout.onFocusStart(point);
                    }

                    for (CameraListener listener : mListeners) {
                        listener.onFocusStart(point);
                    }
                }
            });
        }

        @Override
        public void dispatchOnFocusEnd(@Nullable final Gesture gesture, final boolean success,
                                       @NonNull final PointF point) {
            mLogger.i("dispatchOnFocusEnd", gesture, success, point);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (success && mPlaySounds) {
                        //noinspection all
                        playSound(MediaActionSound.FOCUS_COMPLETE);
                    }

                    if (gesture != null && mGestureMap.get(gesture) == GestureAction.FOCUS_WITH_MARKER) {
                        mTapGestureLayout.onFocusEnd(success);
                    }

                    for (CameraListener listener : mListeners) {
                        listener.onFocusEnd(success, point);
                    }
                }
            });
        }

        @Override
        public void onDeviceOrientationChanged(int deviceOrientation) {
            mLogger.i("onDeviceOrientationChanged", deviceOrientation);
            mCameraController.setDeviceOrientation(deviceOrientation);
            int displayOffset = mOrientationHelper.getDisplayOffset();
            final int value = (deviceOrientation + displayOffset) % 360;
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onOrientationChanged(value);
                    }
                }
            });
        }

        @Override
        public void dispatchOnZoomChanged(final float newValue, @Nullable final PointF[] fingers) {
            mLogger.i("dispatchOnZoomChanged", newValue);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onZoomChanged(newValue, new float[]{0, 1}, fingers);
                    }
                }
            });
        }

        @Override
        public void dispatchOnExposureCorrectionChanged(final float newValue,
                                                        @NonNull final float[] bounds,
                                                        @Nullable final PointF[] fingers) {
            mLogger.i("dispatchOnExposureCorrectionChanged", newValue);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onExposureCorrectionChanged(newValue, bounds, fingers);
                    }
                }
            });
        }

        @Override
        public void dispatchFrame(final Frame frame) {
            if (mFrameProcessors.isEmpty()) {
                // Mark as released. This instance will be reused.
                frame.release();
            } else {
                mLogger.v("dispatchFrame:", frame.getTime(), "processors:", mFrameProcessors.size());
                mFrameProcessorsHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (FrameProcessor processor : mFrameProcessors) {
                            processor.process(frame);
                        }
                        frame.release();
                    }
                });
            }
        }

        @Override
        public void dispatchError(final CameraException exception) {
            mLogger.i("dispatchError", exception);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onCameraError(exception);
                    }
                }
            });
        }
    }

    //endregion

    //region deprecated APIs

    //endregion
}

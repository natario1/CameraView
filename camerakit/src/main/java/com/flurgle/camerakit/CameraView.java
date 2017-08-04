package com.flurgle.camerakit;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.flurgle.camerakit.CameraKit.Constants.FACING_BACK;
import static com.flurgle.camerakit.CameraKit.Constants.FACING_FRONT;
import static com.flurgle.camerakit.CameraKit.Constants.FLASH_AUTO;
import static com.flurgle.camerakit.CameraKit.Constants.FLASH_OFF;
import static com.flurgle.camerakit.CameraKit.Constants.FLASH_ON;
import static com.flurgle.camerakit.CameraKit.Constants.FLASH_TORCH;
import static com.flurgle.camerakit.CameraKit.Constants.SESSION_TYPE_PICTURE;
import static com.flurgle.camerakit.CameraKit.Constants.SESSION_TYPE_VIDEO;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * The CameraView implements the LifecycleObserver interface for ease of use. To take advantage of
 * this, simply call the following from any LifecycleOwner:
 * <pre>
 * {@code
 * protected void onCreate(@Nullable Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     setContentView(R.layout.my_view);
 *     ...
 *     getLifecycle().addObserver(mCameraView);
 * }
 * }
 * </pre>
 */
public class CameraView extends FrameLayout implements LifecycleObserver {

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

    @Facing private int mFacing;
    @Flash private int mFlash;
    @Focus private int mFocus;
    // @Method private int mMethod;
    @ZoomMode private int mZoom;
    // @Permissions private int mPermissions;
    @SessionType private int mSessionType;
    @VideoQuality private int mVideoQuality;
    @WhiteBalance private int mWhiteBalance;
    private int mJpegQuality;
    private boolean mCropOutput;
    private int mDisplayOffset;
    private CameraCallbacks mCameraCallbacks;
    private OrientationHelper mOrientationHelper;
    private CameraImpl mCameraImpl;
    private PreviewImpl mPreviewImpl;

    private Lifecycle mLifecycle;
    private FocusMarkerLayout mFocusMarkerLayout;
    private boolean mIsStarted;

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
        mFacing = a.getInteger(R.styleable.CameraView_cameraFacing, CameraKit.Defaults.DEFAULT_FACING);
        mFlash = a.getInteger(R.styleable.CameraView_cameraFlash, CameraKit.Defaults.DEFAULT_FLASH);
        mFocus = a.getInteger(R.styleable.CameraView_cameraFocus, CameraKit.Defaults.DEFAULT_FOCUS);
        mSessionType = a.getInteger(R.styleable.CameraView_cameraSessionType, CameraKit.Defaults.DEFAULT_SESSION_TYPE);
        mZoom = a.getInteger(R.styleable.CameraView_cameraZoomMode, CameraKit.Defaults.DEFAULT_ZOOM);
        mWhiteBalance = a.getInteger(R.styleable.CameraView_cameraWhiteBalance, CameraKit.Defaults.DEFAULT_WHITE_BALANCE);
        mVideoQuality = a.getInteger(R.styleable.CameraView_cameraVideoQuality, CameraKit.Defaults.DEFAULT_VIDEO_QUALITY);
        mJpegQuality = a.getInteger(R.styleable.CameraView_cameraJpegQuality, CameraKit.Defaults.DEFAULT_JPEG_QUALITY);
        mCropOutput = a.getBoolean(R.styleable.CameraView_cameraCropOutput, CameraKit.Defaults.DEFAULT_CROP_OUTPUT);
        a.recycle();

        mCameraCallbacks = new CameraCallbacks();
        mPreviewImpl = new TextureViewPreview(context, this);
        mCameraImpl = new Camera1(mCameraCallbacks, mPreviewImpl);
        mFocusMarkerLayout = new FocusMarkerLayout(context);
        addView(mFocusMarkerLayout);

        mIsStarted = false;
        setFacing(mFacing);
        setFlash(mFlash);
        setFocus(mFocus);
        setSessionType(mSessionType);
        setZoomMode(mZoom);
        setVideoQuality(mVideoQuality);
        setWhiteBalance(mWhiteBalance);

        if (!isInEditMode()) {
            mOrientationHelper = new OrientationHelper(context) {
                @Override
                public void onDisplayOffsetChanged(int displayOffset) {
                    mDisplayOffset = displayOffset;
                    mCameraImpl.onDisplayOffset(displayOffset);
                    mPreviewImpl.onDisplayOffset(displayOffset);
                }

                @Override
                protected void onDeviceOrientationChanged(int deviceOrientation) {
                    mCameraImpl.onDeviceOrientation(deviceOrientation);
                    mPreviewImpl.onDeviceOrientation(deviceOrientation);
                }
            };

            /* focusMarkerLayout.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent motionEvent) {
                    int action = motionEvent.getAction();
                    if (action == MotionEvent.ACTION_UP && mFocus == CameraKit.Constants.FOCUS_TAP_WITH_MARKER) {
                        focusMarkerLayout.focus(motionEvent.getX(), motionEvent.getY());
                    }

                    mPreviewImpl.getView().dispatchTouchEvent(motionEvent);
                    return true;
                }
            }); */
        }
        mLifecycle = null;
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
     * parent no matter the preview. Thanks to what happens in {@link PreviewImpl}, this acts like
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
        final boolean flip = mCameraImpl.shouldFlipSizes();
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


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true; // Steal our own events.
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // And dispatch to everyone.
        mFocusMarkerLayout.onTouchEvent(event); // For drawing focus marker.
        mCameraImpl.onTouchEvent(event); // For focus behavior.
        return true;
    }

    /**
     * Returns whether the camera has started showing its preview.
     * @return whether the camera has started
     */
    public boolean isStarted() {
        return mIsStarted;
    }


    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mLifecycle != null && mLifecycle.getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            // Potentially update the UI
            if (enabled) {
                start();
            } else {
                stop();
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume(LifecycleOwner owner) {
        mLifecycle = owner.getLifecycle();
        start();
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause(LifecycleOwner owner) {
        mLifecycle = owner.getLifecycle();
        stop();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy(LifecycleOwner owner) {
        mLifecycle = owner.getLifecycle();
        destroy();
    }

    public void start() {
        if (mIsStarted || !isEnabled()) {
            // Already started, do nothing.
            return;
        }

        if (checkPermissions(mSessionType)) {
            mIsStarted = true;
            run(new Runnable() {
                @Override
                public void run() {
                    mCameraImpl.start();
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
        int cameraCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
        int audioCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO);
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

    public void stop() {
        if (!mIsStarted) {
            // Already stopped, do nothing.
            return;
        }
        mIsStarted = false;
        mCameraImpl.stop();
    }

    public void destroy() {
        mCameraCallbacks.clearListeners(); // Release inner listener.
        // This might be useless, but no time to think about it now.
        mWorkerHandler = null;
    }


    /**
     * If present, returns a collection of extra properties from the current camera
     * session.
     * @return an ExtraProperties object.
     */
    @Nullable
    public ExtraProperties getExtraProperties() {
        return mCameraImpl.getExtraProperties();
    }


    /**
     * Set location coordinates to be found later in the jpeg EXIF header
     * @param latitude current latitude
     * @param longitude current longitude
     */
    public void setLocation(double latitude, double longitude) {
        mCameraImpl.setLocation(latitude, longitude);
    }

    /**
     * Sets desired white balance to current camera session.
     * @param whiteBalance desired white balance behavior.
     */
    public void setWhiteBalance(@WhiteBalance int whiteBalance) {
        mWhiteBalance = whiteBalance;
        mCameraImpl.setWhiteBalance(whiteBalance);
    }

    /**
     * Returns the current white balance behavior.
     * @return white balance value.
     */
    @WhiteBalance
    public int getWhiteBalance() {
        return mWhiteBalance;
    }


    /**
     * Sets which camera sensor should be used.
     *
     * @see CameraKit.Constants#FACING_FRONT
     * @see CameraKit.Constants#FACING_BACK
     *
     * @param facing a facing value.
     */
    public void setFacing(@Facing final int facing) {
        this.mFacing = facing;
        run(new Runnable() {
            @Override
            public void run() {
                mCameraImpl.setFacing(facing);
            }
        });
    }


    /**
     * Gets the facing camera currently being used.
     * @return a facing value.
     */
    @Facing
    public int getFacing() {
        return mFacing;
    }


    /**
     * Toggles the facing value between {@link CameraKit.Constants#FACING_BACK}
     * and {@link CameraKit.Constants#FACING_FRONT}.
     *
     * @return the new facing value
     */
    @Facing
    public int toggleFacing() {
        switch (mFacing) {
            case FACING_BACK:
                setFacing(FACING_FRONT);
                break;

            case FACING_FRONT:
                setFacing(FACING_BACK);
                break;
        }

        return mFacing;
    }


    /**
     * Sets the flash mode.
     *
     * @see CameraKit.Constants#FLASH_OFF
     * @see CameraKit.Constants#FLASH_ON
     * @see CameraKit.Constants#FLASH_AUTO
     * @see CameraKit.Constants#FLASH_TORCH

     * @param flash desired flash mode.
     */
    public void setFlash(@Flash int flash) {
        this.mFlash = flash;
        mCameraImpl.setFlash(flash);
    }


    /**
     * Gets the current flash mode.
     * @return a flash mode
     */
    @Flash
    public int getFlash() {
        return mFlash;
    }


    /**
     * Toggles the flash mode between {@link CameraKit.Constants#FLASH_OFF},
     * {@link CameraKit.Constants#FLASH_ON}, {@link CameraKit.Constants#FLASH_AUTO} and
     * {@link CameraKit.Constants#FOCUS_OFF}, in this order.
     *
     * @return the new flash value
     */
    @Flash
    public int toggleFlash() {
        switch (mFlash) {
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

        return mFlash;
    }


    /**
     * Sets the current focus behavior.
     *
     * @see CameraKit.Constants#FOCUS_CONTINUOUS
     * @see CameraKit.Constants#FOCUS_OFF
     * @see CameraKit.Constants#FOCUS_TAP
     * @see CameraKit.Constants#FOCUS_TAP_WITH_MARKER

     * @param focus a Focus value.
     */
    public void setFocus(@Focus int focus) {
        mFocus = focus;
        mFocusMarkerLayout.setEnabled(focus == CameraKit.Constants.FOCUS_TAP_WITH_MARKER);
        if (focus == CameraKit.Constants.FOCUS_TAP_WITH_MARKER) {
            focus = CameraKit.Constants.FOCUS_TAP;
        }
        mCameraImpl.setFocus(focus);
    }


    /**
     * Gets the current focus behavior.
     * @return a focus behavior
     */
    @Focus
    public int getFocus() {
        return mFocus;
    }


    /**
     * This does nothing.
     * @deprecated
     */
    @Deprecated
    public void setCaptureMethod(@Method int method) {}


    /**
     * This does nothing.
     * @deprecated
     */
    @Deprecated
    public void setPermissionPolicy(@Permissions int permissions) {}


    /**
     * Set the current session type to either picture or video.
     * When sessionType is video,
     * - {@link #startCapturingVideo(File)} will not throw any exception
     * - {@link #captureImage()} will fallback to {@link #captureSnapshot()}
     *
     * @see CameraKit.Constants#SESSION_TYPE_PICTURE
     * @see CameraKit.Constants#SESSION_TYPE_VIDEO
     *
     * @param sessionType desired session type.
     */
    public void setSessionType(@SessionType int sessionType) {

        if (sessionType == mSessionType || !mIsStarted) {
            // Check did took place, or will happen on start().
            mSessionType = sessionType;
            mCameraImpl.setSessionType(sessionType);

        } else if (checkPermissions(sessionType)) {
            // Camera is running. CameraImpl setSessionType will do the trick.
            mSessionType = sessionType;
            mCameraImpl.setSessionType(sessionType);

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
        return mSessionType;
    }


    /**
     * Sets the zoom mode for the current session.
     *
     * @see CameraKit.Constants#ZOOM_OFF
     * @see CameraKit.Constants#ZOOM_PINCH
     *
     * @param zoom the zoom mode
     */
    public void setZoomMode(@ZoomMode int zoom) {
        this.mZoom = zoom;
        mCameraImpl.setZoomMode(mZoom);
    }


    /**
     * Gets the current zoom mode.
     * @return the current zoom mode
     */
    @ZoomMode
    public int getZoomMode() {
        return mZoom;
    }


    public void setVideoQuality(@VideoQuality int videoQuality) {
        this.mVideoQuality = videoQuality;
        mCameraImpl.setVideoQuality(mVideoQuality);
    }





    public void setJpegQuality(int jpegQuality) {
        this.mJpegQuality = jpegQuality;
    }

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
     * Note that if sessionType is {@link CameraKit.Constants#SESSION_TYPE_VIDEO}, this
     * might fall back to {@link #captureSnapshot()} (that is, we might capture a preview frame).
     *
     * @see #captureSnapshot()
     */
    public void captureImage() {
        mCameraImpl.captureImage();
    }


    /**
     * Asks the camera to capture a snapshot of the current preview.
     * This eventually triggers {@link CameraListener#onPictureTaken(byte[])} if a listener
     * was registered.
     *
     * The difference with {@link #captureImage()} is that this capture is faster, so it might be
     * better on slower cameras, though the result can be generally blurry or low quality.
     *
     * @see #captureImage()
     */
    public void captureSnapshot() {
        mCameraImpl.captureSnapshot();
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
        mCameraImpl.startVideo(file);
    }


    /**
     * Stops capturing video, if there was a video record going on.
     * This will fire {@link CameraListener#onVideoTaken(File)}.
     */
    public void stopCapturingVideo() {
        mCameraImpl.endVideo();
    }


    /**
     * Returns the size used for the preview,
     * or null if it hasn't been computed (for example if the surface is not ready).
     * @return a Size
     */
    @Nullable
    public Size getPreviewSize() {
        return mCameraImpl != null ? mCameraImpl.getPreviewSize() : null;
    }


    /**
     * Returns the size used for the capture,
     * or null if it hasn't been computed yet (for example if the surface is not ready).
     * @return a Size
     */
    @Nullable
    public Size getCaptureSize() {
        return mCameraImpl != null ? mCameraImpl.getCaptureSize() : null;
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
            ActivityCompat.requestPermissions(activity,
                    permissions.toArray(new String[permissions.size()]),
                    CameraKit.Constants.PERMISSION_REQUEST_CODE);
        }
    }

    class CameraCallbacks {

        private ArrayList<CameraListener> mListeners;
        private Handler uiHandler;


        CameraCallbacks() {
            mListeners = new ArrayList<>(2);
            uiHandler = new Handler(Looper.getMainLooper());
        }


        public void dispatchOnCameraOpened() {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (CameraListener listener : mListeners) {
                        listener.onCameraOpened();
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


}

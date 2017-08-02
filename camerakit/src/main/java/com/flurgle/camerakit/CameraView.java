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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
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

    private Handler sWorkerHandler;

    private Handler getWorkerHandler() {
        synchronized (this) {
            if (sWorkerHandler == null) {
                HandlerThread workerThread = new HandlerThread("CameraViewWorker");
                workerThread.setDaemon(true);
                workerThread.start();
                sWorkerHandler = new Handler(workerThread.getLooper());
            }
        }
        return sWorkerHandler;
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
    private boolean mAdjustViewBounds;
    private CameraListenerWrapper mCameraListener;
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
        mAdjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, CameraKit.Defaults.DEFAULT_ADJUST_VIEW_BOUNDS);
        a.recycle();

        mCameraListener = new CameraListenerWrapper();
        mPreviewImpl = new TextureViewPreview(context, this);
        mCameraImpl = new Camera1(mCameraListener, mPreviewImpl);
        mFocusMarkerLayout = new FocusMarkerLayout(context);
        addView(mFocusMarkerLayout);

        mIsStarted = false;
        setFacing(mFacing);
        setFlash(mFlash);
        setFocus(mFocus);
        setSessionType(mSessionType);
        setZoom(mZoom);
        setVideoQuality(mVideoQuality);
        setWhiteBalance(mWhiteBalance);

        if (!isInEditMode()) {
            mOrientationHelper = new OrientationHelper(context) {
                @Override
                public void onDisplayOffsetChanged(int displayOffset) {
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


    /**
     * If adjustViewBounds was set AND one of the dimensions is set to WRAP_CONTENT,
     * CameraView will adjust that dimensions to fit the preview aspect ratio as returned by
     * {@link #getPreviewSize()}.
     *
     * If this is not true, the surface will adapt to the dimension specified in the layout file.
     * Having fixed dimensions means that, very likely, what the user sees is different from what
     * the final picture will be. This is also due to what happens in {@link PreviewImpl#refreshScale()}.
     *
     * If this is a problem, you can use {@link #setCropOutput(boolean)} set to true.
     * In that case, the final image will have the same aspect ratio of the preview.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mAdjustViewBounds) {
            Log.e(TAG, "onMeasure, adjustViewBounds=false");
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        Size previewSize = getPreviewSize();
        if (previewSize == null) { // Early measure.
            Log.e(TAG, "onMeasure, early measure");
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        boolean wwc = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
        boolean hwc = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        boolean flip = mCameraImpl.shouldFlipSizes();
        float previewWidth = flip ? previewSize.getHeight() : previewSize.getWidth();
        float previewHeight = flip ? previewSize.getWidth() : previewSize.getHeight();
        float parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        float parentWidth = MeasureSpec.getSize(widthMeasureSpec); // mode = AT_MOST
        Log.e(TAG, "onMeasure, parent size is "+new Size((int)parentWidth, (int)parentHeight)); // 1080x1794
        Log.e(TAG, "onMeasure, surface size is "+new Size((int)previewWidth, (int)previewHeight)); // 1600x1200

        if (wwc && hwc) {
            // If both dimensions are WRAP_CONTENT, let's try to fit the preview size perfectly
            // without cropping.
            // TODO: This should actually be a flag, like scaleMode, that replaces cropOutput and adjustViewBounds.
            // This is like a fitCenter.
            float targetRatio = previewHeight / previewWidth;
            float currentRatio = parentHeight / parentWidth;
            if (currentRatio > targetRatio) {
                // View is too tall. Must reduce height.
                int newHeight = (int) (parentWidth * targetRatio);
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY));
            } else {
                // View is too wide. Must reduce width.
                int newWidth = (int) (parentHeight / targetRatio);
                super.onMeasure(MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
            }

        } else if (wwc) {
            // Legacy behavior, with just a WC dimension. This is dangerous because the final size
            // might be bigger than the available size, resulting in part of the surface getting cropped.
            // Think for example of a 4:3 preview in a 16:9 screen, with width=MP and height=WC.
            // This is like a cropCenter.
            float height = MeasureSpec.getSize(heightMeasureSpec);
            float ratio = height / previewWidth;
            int width = (int) (previewHeight * ratio);
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec);

        } else if (hwc) {
            float width = MeasureSpec.getSize(widthMeasureSpec);
            float ratio = width / previewHeight;
            int height = (int) (previewWidth * ratio);
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
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
        mCameraListener = new CameraListenerWrapper(); // Release inner listener.
        // This might be useless, but no time to think about it now.
        sWorkerHandler = null;
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



    public void setVideoQuality(@VideoQuality int videoQuality) {
        this.mVideoQuality = videoQuality;
        mCameraImpl.setVideoQuality(mVideoQuality);
    }

    public void setZoom(@ZoomMode int zoom) {
        this.mZoom = zoom;
        mCameraImpl.setZoom(mZoom);
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
     */
    public void setCameraListener(CameraListener cameraListener) {
        this.mCameraListener.wrapListener(cameraListener);
    }

    public void captureImage() {
        mCameraImpl.captureImage();
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

    class CameraListenerWrapper extends CameraListener {

        private CameraListener mWrappedListener;

        @Override
        public void onCameraOpened() {
            super.onCameraOpened();
            if (mWrappedListener == null) return;
            mWrappedListener.onCameraOpened();
        }

        @Override
        public void onCameraClosed() {
            super.onCameraClosed();
            if (mWrappedListener == null) return;
            mWrappedListener.onCameraClosed();
        }

        @Override
        public void onPictureTaken(byte[] jpeg) {
            super.onPictureTaken(jpeg);
            if (mWrappedListener == null) return;
            if (mCropOutput) {
                AspectRatio outputRatio = AspectRatio.of(getWidth(), getHeight());
                mWrappedListener.onPictureTaken(new CenterCrop(jpeg, outputRatio, mJpegQuality).getJpeg());
            } else {
                mWrappedListener.onPictureTaken(jpeg);
            }
        }

        void processYuvImage(YuvImage yuv) {
            if (mWrappedListener == null) return;
            if (mCropOutput) {
                AspectRatio outputRatio = AspectRatio.of(getWidth(), getHeight());
                mWrappedListener.onPictureTaken(new CenterCrop(yuv, outputRatio, mJpegQuality).getJpeg());
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, yuv.getWidth(), yuv.getHeight()), mJpegQuality, out);
                mWrappedListener.onPictureTaken(out.toByteArray());
            }
        }

        @Override
        public void onVideoTaken(File video) {
            super.onVideoTaken(video);
            mWrappedListener.onVideoTaken(video);
        }

        private void wrapListener(@Nullable CameraListener cameraListener) {
            mWrappedListener = cameraListener;
        }
    }


}

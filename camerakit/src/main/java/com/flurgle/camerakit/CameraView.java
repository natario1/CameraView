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
import static com.flurgle.camerakit.CameraKit.Constants.PERMISSIONS_PICTURE;
import static com.flurgle.camerakit.CameraKit.Constants.PERMISSIONS_VIDEO;

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

    private static Handler sWorkerHandler;

    private static Handler getWorkerHandler() {
        if (sWorkerHandler == null) {
            HandlerThread workerThread = new HandlerThread("CameraViewWorker");
            workerThread.setDaemon(true);
            workerThread.start();
            sWorkerHandler = new Handler(workerThread.getLooper());
        }
        return sWorkerHandler;
    }

    private void run(Runnable runnable) {
        getWorkerHandler().post(runnable);
    }

    @Facing private int mFacing;
    @Flash private int mFlash;
    @Focus private int mFocus;
    @Method private int mMethod;
    @Zoom private int mZoom;
    @Permissions private int mPermissions;
    @VideoQuality private int mVideoQuality;
    private int mJpegQuality;
    private boolean mCropOutput;
    private boolean mAdjustViewBounds;
    private CameraListenerMiddleWare mCameraListener;
    private OrientationHelper mOrientationHelper;
    private CameraImpl mCameraImpl;
    private PreviewImpl mPreviewImpl;

    private Lifecycle mLifecycle;
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
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CameraView, 0, 0);
            try {
                mFacing = a.getInteger(R.styleable.CameraView_cameraFacing, CameraKit.Defaults.DEFAULT_FACING);
                mFlash = a.getInteger(R.styleable.CameraView_cameraFlash, CameraKit.Defaults.DEFAULT_FLASH);
                mFocus = a.getInteger(R.styleable.CameraView_cameraFocus, CameraKit.Defaults.DEFAULT_FOCUS);
                mMethod = a.getInteger(R.styleable.CameraView_cameraCaptureMethod, CameraKit.Defaults.DEFAULT_METHOD);
                mZoom = a.getInteger(R.styleable.CameraView_cameraZoom, CameraKit.Defaults.DEFAULT_ZOOM);
                mPermissions = a.getInteger(R.styleable.CameraView_cameraPermissionPolicy, CameraKit.Defaults.DEFAULT_PERMISSIONS);
                mVideoQuality = a.getInteger(R.styleable.CameraView_cameraVideoQuality, CameraKit.Defaults.DEFAULT_VIDEO_QUALITY);
                mJpegQuality = a.getInteger(R.styleable.CameraView_cameraJpegQuality, CameraKit.Defaults.DEFAULT_JPEG_QUALITY);
                mCropOutput = a.getBoolean(R.styleable.CameraView_cameraCropOutput, CameraKit.Defaults.DEFAULT_CROP_OUTPUT);
                mAdjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, CameraKit.Defaults.DEFAULT_ADJUST_VIEW_BOUNDS);
            } finally {
                a.recycle();
            }
        }

        mCameraListener = new CameraListenerMiddleWare();

        mPreviewImpl = new TextureViewPreview(context, this);
        mCameraImpl = new Camera1(mCameraListener, mPreviewImpl);

        mIsStarted = false;
        setFacing(mFacing);
        setFlash(mFlash);
        setFocus(mFocus);
        setCaptureMethod(mMethod);
        setZoom(mZoom);
        setPermissionPolicy(mPermissions);
        setVideoQuality(mVideoQuality);

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

            final FocusMarkerLayout focusMarkerLayout = new FocusMarkerLayout(getContext());
            addView(focusMarkerLayout);
            focusMarkerLayout.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent motionEvent) {
                    int action = motionEvent.getAction();
                    if (action == MotionEvent.ACTION_UP && mFocus == CameraKit.Constants.FOCUS_TAP_WITH_MARKER) {
                        focusMarkerLayout.focus(motionEvent.getX(), motionEvent.getY());
                    }

                    mPreviewImpl.getView().dispatchTouchEvent(motionEvent);
                    return true;
                }
            });
        }
        mLifecycle = null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            mOrientationHelper.enable(display);
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
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        Size previewSize = getPreviewSize();
        if (previewSize == null) {
            // Early measure.
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        boolean wwc = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
        boolean hwc = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        boolean flip = mCameraImpl.shouldFlipSizes();
        float previewWidth = flip ? previewSize.getHeight() : previewSize.getWidth();
        float previewHeight = flip ? previewSize.getWidth() : previewSize.getHeight();

        if (wwc && hwc) {
            // If both dimensions are WRAP_CONTENT, let's try to fit the preview size perfectly
            // without cropping.
            // TODO: This should actually be a flag, like scaleMode, that replaces cropOutput and adjustViewBounds.
            // This is like a fitCenter.

            // preview: 1600x1200
            // parent:  1080x1794
            float parentHeight = MeasureSpec.getSize(heightMeasureSpec); // 1794.
            float parentWidth = MeasureSpec.getSize(widthMeasureSpec); //   1080. mode = AT_MOST
            float targetRatio = previewHeight / previewWidth;
            float currentRatio = parentHeight / parentWidth;
            Log.e(TAG, "parentHeight="+parentHeight+", parentWidth="+parentWidth);
            Log.e(TAG, "previewHeight="+previewHeight+", previewWidth="+previewWidth);
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
        checkPermissionPolicyOrThrow();
        int cameraCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
        int audioCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO);
        switch (mPermissions) {
            case PERMISSIONS_VIDEO:
                if (cameraCheck != PackageManager.PERMISSION_GRANTED || audioCheck != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(true, true);
                    return;
                }
                break;

            case PERMISSIONS_PICTURE:
                if (cameraCheck != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(true, false);
                    return;
                }
                break;
        }

        mIsStarted = true;
        run(new Runnable() {
            @Override
            public void run() {
                mCameraImpl.start();
            }
        });
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
        // This might be useless, but no time to think about it now.
        sWorkerHandler = null;
    }

    @Nullable
    public CameraProperties getCameraProperties() {
        return mCameraImpl.getCameraProperties();
    }

    @Facing
    public int getFacing() {
        return mFacing;
    }

    public void setFacing(@Facing final int facing) {
        this.mFacing = facing;

        run(new Runnable() {
            @Override
            public void run() {
                mCameraImpl.setFacing(facing);
            }
        });
    }

    public void setFlash(@Flash int flash) {
        this.mFlash = flash;
        mCameraImpl.setFlash(flash);
    }

    @Flash
    public int getFlash() {
        return mFlash;
    }

    public void setFocus(@Focus int focus) {
        this.mFocus = focus;
        if (this.mFocus == CameraKit.Constants.FOCUS_TAP_WITH_MARKER) {
            mCameraImpl.setFocus(CameraKit.Constants.FOCUS_TAP);
            return;
        }

        mCameraImpl.setFocus(mFocus);
    }

    public void setCaptureMethod(@Method int method) {
        this.mMethod = method;
        mCameraImpl.setMethod(mMethod);
    }

    public void setZoom(@Zoom int zoom) {
        this.mZoom = zoom;
        mCameraImpl.setZoom(mZoom);
    }

    /**
     * Sets permission policy.
     * @param permissions desired policy, either picture or video.
     */
    public void setPermissionPolicy(@Permissions int permissions) {
        this.mPermissions = permissions;
        checkPermissionPolicyOrThrow();
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

    public void setCameraListener(CameraListener cameraListener) {
        this.mCameraListener.setCameraListener(cameraListener);
    }

    public void captureImage() {
        mCameraImpl.captureImage();
    }

    public void startRecordingVideo() {
        mCameraImpl.startVideo();
    }

    public void stopRecordingVideo() {
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

    private class CameraListenerMiddleWare extends CameraListener {

        private CameraListener mCameraListener;

        @Override
        public void onCameraOpened() {
            super.onCameraOpened();
            getCameraListener().onCameraOpened();
        }

        @Override
        public void onCameraClosed() {
            super.onCameraClosed();
            getCameraListener().onCameraClosed();
        }

        @Override
        public void onPictureTaken(byte[] jpeg) {
            super.onPictureTaken(jpeg);
            if (mCropOutput) {
                AspectRatio outputRatio = AspectRatio.of(getWidth(), getHeight());
                getCameraListener().onPictureTaken(new CenterCrop(jpeg, outputRatio, mJpegQuality).getJpeg());
            } else {
                getCameraListener().onPictureTaken(jpeg);
            }
        }

        @Override
        public void onPictureTaken(YuvImage yuv) {
            super.onPictureTaken(yuv);
            if (mCropOutput) {
                AspectRatio outputRatio = AspectRatio.of(getWidth(), getHeight());
                getCameraListener().onPictureTaken(new CenterCrop(yuv, outputRatio, mJpegQuality).getJpeg());
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, yuv.getWidth(), yuv.getHeight()), mJpegQuality, out);
                getCameraListener().onPictureTaken(out.toByteArray());
            }
        }

        @Override
        public void onVideoTaken(File video) {
            super.onVideoTaken(video);
            getCameraListener().onVideoTaken(video);
        }

        public void setCameraListener(@Nullable CameraListener cameraListener) {
            this.mCameraListener = cameraListener;
        }

        @NonNull
        public CameraListener getCameraListener() {
            return mCameraListener != null ? mCameraListener : new CameraListener() {
            };
        }

    }

    /**
     * If mPermissions == PERMISSIONS_VIDEO we will ask for RECORD_AUDIO permission.
     * If the developer did not add this to its manifest, throw and fire warnings.
     * (Hoping this is not cought elsewhere... we should test).
     */
    private void checkPermissionPolicyOrThrow() {
        if (mPermissions == PERMISSIONS_VIDEO) {
            try {
                PackageManager manager = getContext().getPackageManager();
                PackageInfo info = manager.getPackageInfo(getContext().getPackageName(), PackageManager.GET_PERMISSIONS);
                for (String requestedPermission : info.requestedPermissions) {
                    if (requestedPermission.equals(Manifest.permission.RECORD_AUDIO)) {
                        return;
                    }
                }
                String message = "When the permission policy is PERMISSION_VIDEO, the RECORD_AUDIO permission " +
                        "should be added to the application manifest file.";
                Log.w(TAG, message);
                throw new IllegalStateException(message);
            } catch (PackageManager.NameNotFoundException e) {
                // Not possible.
            }
        }
    }

}

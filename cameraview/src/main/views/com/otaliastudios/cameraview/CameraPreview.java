package com.otaliastudios.cameraview;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

/**
 * A CameraPreview takes in input stream from the {@link CameraController}, and streams it
 * into an output surface that belongs to the view hierarchy.
 *
 * @param <T> the type of view which hosts the content surface
 * @param <Output> the type of output, either {@link android.view.SurfaceHolder} or {@link android.graphics.SurfaceTexture}
 */
abstract class CameraPreview<T extends View, Output> {

    protected final static CameraLogger LOG = CameraLogger.create(CameraPreview.class.getSimpleName());

    // Used for testing.
    Task<Void> mCropTask = new Task<>();

    // This is used to notify CameraController to recompute its camera Preview size.
    // After that, CameraView will need a new layout pass to adapt to the Preview size.
    interface SurfaceCallback {
        void onSurfaceAvailable();
        void onSurfaceChanged();
        void onSurfaceDestroyed();
    }

    private SurfaceCallback mSurfaceCallback;
    private T mView;
    protected boolean mCropping;

    // These are the surface dimensions in REF_VIEW.
    protected int mOutputSurfaceWidth;
    protected int mOutputSurfaceHeight;

    // These are the preview stream dimensions, in REF_VIEW.
    protected int mInputStreamWidth;
    protected int mInputStreamHeight;
    protected boolean mInputFlipped;

    CameraPreview(@NonNull Context context, @NonNull ViewGroup parent, @Nullable SurfaceCallback callback) {
        mView = onCreateView(context, parent);
        mSurfaceCallback = callback;
    }

    @NonNull
    protected abstract T onCreateView(@NonNull Context context, @NonNull ViewGroup parent);

    @NonNull
    final T getView() {
        return mView;
    }

    // Only used in tests
    @NonNull
    abstract View getRootView();

    @NonNull
    abstract Class<Output> getOutputClass();

    @NonNull
    abstract Output getOutput();

    // As far as I can see, these are the actual preview dimensions, as set in CameraParameters.
    // This is called by the CameraImpl.
    // These must be alredy rotated, if needed, to be consistent with surface/view sizes.
    void setStreamSize(int width, int height, boolean wasFlipped) {
        LOG.i("setStreamSize:", "desiredW=", width, "desiredH=", height);
        mInputStreamWidth = width;
        mInputStreamHeight = height;
        mInputFlipped = wasFlipped;
        if (mInputStreamWidth > 0 && mInputStreamHeight > 0) {
            crop();
        }
    }

    @NonNull
    final Size getStreamSize() {
        return new Size(mInputStreamWidth, mInputStreamHeight);
    }

    @NonNull
    final Size getSurfaceSize() {
        return new Size(mOutputSurfaceWidth, mOutputSurfaceHeight);
    }

    final void setSurfaceCallback(@NonNull SurfaceCallback callback) {
        mSurfaceCallback = callback;
        // If surface already available, dispatch.
        if (mOutputSurfaceWidth != 0 || mOutputSurfaceHeight != 0) {
            mSurfaceCallback.onSurfaceAvailable();
        }
    }


    @SuppressWarnings("WeakerAccess")
    protected final void dispatchOnSurfaceAvailable(int width, int height) {
        LOG.i("dispatchOnSurfaceAvailable:", "w=", width, "h=", height);
        mOutputSurfaceWidth = width;
        mOutputSurfaceHeight = height;
        if (mOutputSurfaceWidth > 0 && mOutputSurfaceHeight > 0) {
            crop();
        }
        mSurfaceCallback.onSurfaceAvailable();
    }


    // As far as I can see, these are the view/surface dimensions.
    // This is called by subclasses.
    @SuppressWarnings("WeakerAccess")
    protected final void dispatchOnSurfaceSizeChanged(int width, int height) {
        LOG.i("dispatchOnSurfaceSizeChanged:", "w=", width, "h=", height);
        if (width != mOutputSurfaceWidth || height != mOutputSurfaceHeight) {
            mOutputSurfaceWidth = width;
            mOutputSurfaceHeight = height;
            if (width > 0 && height > 0) {
                crop();
            }
            mSurfaceCallback.onSurfaceChanged();
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected final void dispatchOnSurfaceDestroyed() {
        mOutputSurfaceWidth = 0;
        mOutputSurfaceHeight = 0;
        mSurfaceCallback.onSurfaceDestroyed();
    }

    // Public for mockito (CameraViewTest)
    public void onResume() {}

    // Public for mockito (CameraViewTest)
    public void onPause() {}

    // Public for mockito (CameraViewTest)
    public void onDestroy() {}

    final boolean hasSurface() {
        return mOutputSurfaceWidth > 0 && mOutputSurfaceHeight > 0;
    }

    /**
     * Here we must crop the visible part by applying a > 1 scale to one of our
     * dimensions. This way our internal aspect ratio (mOutputSurfaceWidth / mOutputSurfaceHeight)
     * will match the preview size aspect ratio (mInputStreamWidth / mInputStreamHeight).
     *
     * There might still be some absolute difference (e.g. same ratio but bigger / smaller).
     * However that should be already managed by the framework.
     */
    protected void crop() {
        // The base implementation does not support cropping.
        mCropTask.start();
        mCropTask.end(null);
    }

    boolean supportsCropping() {
        return false;
    }

    /**
     * Whether we are cropping the output.
     * If false, this means that the output image will match the visible bounds.
     * @return true if cropping
     */
    /* not final for tests */ boolean isCropping() {
        return mCropping;
    }
}

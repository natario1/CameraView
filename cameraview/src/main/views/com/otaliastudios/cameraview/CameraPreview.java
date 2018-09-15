package com.otaliastudios.cameraview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

abstract class CameraPreview<T extends View, Output> {

    protected final static CameraLogger LOG = CameraLogger.create(CameraPreview.class.getSimpleName());

    // Used for testing.
    Task<Void> mCropTask = new Task<>();

    // This is used to notify CameraController to recompute its camera Preview size.
    // After that, CameraView will need a new layout pass to adapt to the Preview size.
    interface SurfaceCallback {
        void onSurfaceAvailable();
        void onSurfaceChanged();
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

    CameraPreview(Context context, ViewGroup parent, SurfaceCallback callback) {
        mView = onCreateView(context, parent);
        mSurfaceCallback = callback;
    }

    @NonNull
    protected abstract T onCreateView(Context context, ViewGroup parent);

    @NonNull
    final T getView() {
        return mView;
    }

    abstract Class<Output> getOutputClass();

    abstract Output getOutput();

    // As far as I can see, these are the actual preview dimensions, as set in CameraParameters.
    // This is called by the CameraImpl.
    // These must be alredy rotated, if needed, to be consistent with surface/view sizes.
    void setInputStreamSize(int width, int height) {
        LOG.i("setInputStreamSize:", "desiredW=", width, "desiredH=", height);
        mInputStreamWidth = width;
        mInputStreamHeight = height;
        crop();
    }

    final Size getInputStreamSize() {
        return new Size(mInputStreamWidth, mInputStreamHeight);
    }

    final Size getOutputSurfaceSize() {
        return new Size(mOutputSurfaceWidth, mOutputSurfaceHeight);
    }

    final void setSurfaceCallback(SurfaceCallback callback) {
        mSurfaceCallback = callback;
        // If surface already available, dispatch.
        if (mOutputSurfaceWidth != 0 || mOutputSurfaceHeight != 0) {
            mSurfaceCallback.onSurfaceAvailable();
        }
    }


    protected final void onSurfaceAvailable(int width, int height) {
        LOG.i("onSurfaceAvailable:", "w=", width, "h=", height);
        mOutputSurfaceWidth = width;
        mOutputSurfaceHeight = height;
        crop();
        mSurfaceCallback.onSurfaceAvailable();
    }


    // As far as I can see, these are the view/surface dimensions.
    // This is called by subclasses.
    protected final void onSurfaceSizeChanged(int width, int height) {
        LOG.i("onSurfaceSizeChanged:", "w=", width, "h=", height);
        if (width != mOutputSurfaceWidth || height != mOutputSurfaceHeight) {
            mOutputSurfaceWidth = width;
            mOutputSurfaceHeight = height;
            crop();
            mSurfaceCallback.onSurfaceChanged();
        }
    }

    protected final void onSurfaceDestroyed() {
        mOutputSurfaceWidth = 0;
        mOutputSurfaceHeight = 0;
    }

    void onResume() {}

    void onPause() {}

    void onDestroy() {}

    final boolean isReady() {
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

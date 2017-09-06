package com.otaliastudios.cameraview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

abstract class Preview<T extends View, Output> {

    protected final static CameraLogger LOG = CameraLogger.create(Preview.class.getSimpleName());

    // Used for testing.
    Task<Void> mCropTask = new Task<Void>();

    // This is used to notify CameraImpl to recompute its camera Preview size.
    // After that, CameraView will need a new layout pass to adapt to the Preview size.
    interface SurfaceCallback {
        void onSurfaceAvailable();
        void onSurfaceChanged();
    }

    private SurfaceCallback mSurfaceCallback;
    private T mView;

    // As far as I can see, these are the view/surface dimensions.
    // This live in the 'View' orientation.
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    // As far as I can see, these are the actual preview dimensions, as set in CameraParameters.
    private int mDesiredWidth;
    private int mDesiredHeight;

    Preview(Context context, ViewGroup parent) {
        mView = onCreateView(context, parent);
    }

    @NonNull
    protected abstract T onCreateView(Context context, ViewGroup parent);

    abstract Surface getSurface();

    @NonNull
    final T getView() {
        return mView;
    }

    abstract Class<Output> getOutputClass();

    abstract Output getOutput();

    abstract boolean isReady();

    // As far as I can see, these are the actual preview dimensions, as set in CameraParameters.
    // This is called by the CameraImpl.
    // These must be alredy rotated, if needed, to be consistent with surface/view sizes.
    void setDesiredSize(int width, int height) {
        LOG.i("setDesiredSize:", "desiredW=", width, "desiredH=", height);
        this.mDesiredWidth = width;
        this.mDesiredHeight = height;
        crop();
    }

    final Size getDesiredSize() {
        return new Size(mDesiredWidth, mDesiredHeight);
    }

    final Size getSurfaceSize() {
        return new Size(mSurfaceWidth, mSurfaceHeight);
    }

    final void setSurfaceCallback(SurfaceCallback callback) {
        mSurfaceCallback = callback;
    }


    protected final void onSurfaceAvailable(int width, int height) {
        LOG.i("onSurfaceAvailable:", "w=", width, "h=", height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        crop();
        mSurfaceCallback.onSurfaceAvailable();
    }


    // As far as I can see, these are the view/surface dimensions.
    // This is called by subclasses.
    protected final void onSurfaceSizeChanged(int width, int height) {
        LOG.i("onSurfaceSizeChanged:", "w=", width, "h=", height);
        if (width != mSurfaceWidth || height != mSurfaceHeight) {
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            crop();
            mSurfaceCallback.onSurfaceChanged();
        }
    }


    protected final void onSurfaceDestroyed() {
        mSurfaceWidth = 0;
        mSurfaceHeight = 0;
    }


    /**
     * Here we must crop the visible part by applying a > 1 scale to one of our
     * dimensions. This way our internal aspect ratio (mSurfaceWidth / mSurfaceHeight)
     * will match the preview size aspect ratio (mDesiredWidth / mDesiredHeight).
     *
     * There might still be some absolute difference (e.g. same ratio but bigger / smaller).
     * However that should be already managed by the framework.
     */
    private final void crop() {
        mCropTask.start();
        getView().post(new Runnable() {
            @Override
            public void run() {
                if (mDesiredHeight == 0 || mDesiredWidth == 0 ||
                        mSurfaceHeight == 0 || mSurfaceWidth == 0) {
                    mCropTask.end(null);
                    return;
                }

                float scaleX = 1f, scaleY = 1f;
                AspectRatio current = AspectRatio.of(mSurfaceWidth, mSurfaceHeight);
                AspectRatio target = AspectRatio.of(mDesiredWidth, mDesiredHeight);
                if (current.toFloat() >= target.toFloat()) {
                    // We are too short. Must increase height.
                    scaleY = current.toFloat() / target.toFloat();
                } else {
                    // We must increase width.
                    scaleX = target.toFloat() / current.toFloat();
                }
                getView().setScaleX(scaleX);
                getView().setScaleY(scaleY);
                LOG.i("crop:", "applied scaleX=", scaleX);
                LOG.i("crop:", "applied scaleY=", scaleY);
                mCropTask.end(null);
            }
        });
    }


    /**
     * Whether we are cropping the output.
     * If false, this means that the output image will match the visible bounds.
     * @return true if cropping
     */
    final boolean isCropping() {
        // Account for some error
        return getView().getScaleX() > 1.02f || getView().getScaleY() > 1.02f;
    }
}

package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

abstract class Preview {

    // This is used to notify CameraImpl to recompute its camera Preview size.
    // After that, CameraView will need a new layout pass to adapt to the Preview size.
    interface SurfaceCallback {
        void onSurfaceAvailable();
        void onSurfaceChanged();
    }

    private SurfaceCallback mSurfaceCallback;

    // As far as I can see, these are the view/surface dimensions.
    // This live in the 'View' orientation.
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    // As far as I can see, these are the actual preview dimensions, as set in CameraParameters.
    private int mDesiredWidth;
    private int mDesiredHeight;

    Preview(Context context, ViewGroup parent) {}

    abstract Surface getSurface();
    abstract View getView();
    abstract Class getOutputClass();
    abstract boolean isReady();
    SurfaceHolder getSurfaceHolder() {
        return null;
    }
    SurfaceTexture getSurfaceTexture() {
        return null;
    }


    // As far as I can see, these are the actual preview dimensions, as set in CameraParameters.
    // This is called by the CameraImpl.
    // These must be alredy rotated, if needed, to be consistent with surface/view sizes.
    void setDesiredSize(int width, int height) {
        this.mDesiredWidth = width;
        this.mDesiredHeight = height;
        crop();
    }

    final Size getSurfaceSize() {
        return new Size(mSurfaceWidth, mSurfaceHeight);
    }

    final void setSurfaceCallback(SurfaceCallback callback) {
        mSurfaceCallback = callback;
    }


    protected final void onSurfaceAvailable(int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        crop();
        mSurfaceCallback.onSurfaceAvailable();
    }


    // As far as I can see, these are the view/surface dimensions.
    // This is called by subclasses.
    protected final void onSurfaceSizeChanged(int width, int height) {
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
        crop();
    }


    /**
     * As far as I can see, this extends either width or height of the surface,
     * to match the desired aspect ratio.
     * This means that the external part of the surface will be cropped by the outer view.
     */
    private final void crop() {
        getView().post(new Runnable() {
            @Override
            public void run() {
                if (mDesiredWidth != 0 && mDesiredHeight != 0) {
                    AspectRatio aspectRatio = AspectRatio.of(mDesiredWidth, mDesiredHeight);
                    float targetHeight = (float) mSurfaceWidth / aspectRatio.toFloat();
                    float scale = 1;
                    if (mSurfaceHeight > 0) {
                        scale = targetHeight / (float) mSurfaceHeight;
                    }

                    if (scale > 1) {
                        getView().setScaleX(1f);
                        getView().setScaleY(scale);
                    } else {
                        getView().setScaleX(1f / scale);
                        getView().setScaleY(1f);
                    }
                }
            }
        });
    }


    /**
     * Whether we are cropping the output.
     * If false, this means that the output image will match the visible bounds.
     * @return true if cropping
     */
    final boolean isCropping() {
        return getView().getScaleX() > 1 || getView().getScaleY() > 1;
    }
}

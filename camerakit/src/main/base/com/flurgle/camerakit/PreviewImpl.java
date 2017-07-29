package com.flurgle.camerakit;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

abstract class PreviewImpl {

    interface OnSurfaceChangedCallback {
        void onSurfaceChanged();
    }

    private OnSurfaceChangedCallback mOnSurfaceChangedCallback;

    // As far as I can see, these are the view/surface dimensions.
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    // As far as I can see, these are the actual preview dimensions, as set in CameraParameters.
    private int mDesiredWidth;
    private int mDesiredHeight;

    void setCallback(OnSurfaceChangedCallback callback) {
        mOnSurfaceChangedCallback = callback;
    }

    abstract Surface getSurface();

    abstract View getView();

    abstract Class getOutputClass();

    protected void onDisplayOffset(int displayOrientation) {}
    protected void onDeviceOrientation(int deviceOrientation) {}

    abstract boolean isReady();

    protected void dispatchSurfaceChanged() {
        mOnSurfaceChangedCallback.onSurfaceChanged();
    }

    SurfaceHolder getSurfaceHolder() {
        return null;
    }

    SurfaceTexture getSurfaceTexture() {
        return null;
    }

    // As far as I can see, these are the view/surface dimensions.
    // This is called by subclasses.
    protected void setSurfaceSize(int width, int height) {
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        refreshScale(); // Refresh true preview size to adjust scaling
    }

    // As far as I can see, these are the actual preview dimensions, as set in CameraParameters.
    // This is called by the CameraImpl.
    void setDesiredSize(int width, int height) {
        this.mDesiredWidth = width;
        this.mDesiredHeight = height;
        refreshScale();
    }

    Size getSurfaceSize() {
        return new Size(mSurfaceWidth, mSurfaceHeight);
    }

    /**
     * As far as I can see, this extends either width or height of the surface,
     * to match the desired aspect ratio.
     * This means that the external part of the surface will be cropped by the outer view.
     */
    protected void refreshScale() {
        getView().post(new Runnable() {
            @Override
            public void run() {
                if (mDesiredWidth != 0 && mDesiredHeight != 0) {
                    AspectRatio aspectRatio = AspectRatio.of(mDesiredWidth, mDesiredHeight);
                    int targetHeight = (int) (getView().getWidth() * aspectRatio.toFloat());
                    float scaleY;
                    if (getView().getHeight() > 0) {
                        scaleY = (float) targetHeight / (float) getView().getHeight();
                    } else {
                        scaleY = 1;
                    }

                    if (scaleY > 1) {
                        getView().setScaleX(1);
                        getView().setScaleY(scaleY);
                    } else {
                        getView().setScaleX(1 / scaleY);
                        getView().setScaleY(1);
                    }
                }
            }
        });
    }
}

package com.otaliastudios.cameraview.preview;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.CameraEngine;
import com.otaliastudios.cameraview.internal.utils.Task;
import com.otaliastudios.cameraview.size.Size;

/**
 * A CameraPreview takes in input stream from the {@link CameraEngine}, and streams it
 * into an output surface that belongs to the view hierarchy.
 *
 * @param <T> the type of view which hosts the content surface
 * @param <Output> the type of output, either {@link android.view.SurfaceHolder} or {@link android.graphics.SurfaceTexture}
 */
public abstract class CameraPreview<T extends View, Output> {

    protected final static CameraLogger LOG = CameraLogger.create(CameraPreview.class.getSimpleName());

    /**
     * This is used to notify CameraEngine to recompute its camera Preview size.
     * After that, CameraView will need a new layout pass to adapt to the Preview size.
     */
    public interface SurfaceCallback {

        /**
         * Called when the surface is available.
         */
        void onSurfaceAvailable();

        /**
         * Called when the surface has changed.
         */
        void onSurfaceChanged();

        /**
         * Called when the surface was destroyed.
         */
        void onSurfaceDestroyed();
    }

    @VisibleForTesting Task<Void> mCropTask = new Task<>();
    private SurfaceCallback mSurfaceCallback;
    private T mView;
    boolean mCropping;

    // These are the surface dimensions in REF_VIEW.
    int mOutputSurfaceWidth;
    int mOutputSurfaceHeight;

    // These are the preview stream dimensions, in REF_VIEW.
    int mInputStreamWidth;
    int mInputStreamHeight;

    /**
     * Creates a new preview.
     * @param context a context
     * @param parent where to inflate our view
     * @param callback the callback
     */
    public CameraPreview(@NonNull Context context, @NonNull ViewGroup parent, @Nullable SurfaceCallback callback) {
        mView = onCreateView(context, parent);
        mSurfaceCallback = callback;
    }

    /**
     * Sets a callback to be notified of surface events (creation, change, destruction)
     * @param callback a callback
     */
    public final void setSurfaceCallback(@Nullable SurfaceCallback callback) {
        mSurfaceCallback = callback;
        // If surface already available, dispatch.
        if (mOutputSurfaceWidth != 0 || mOutputSurfaceHeight != 0) {
            if (callback != null) callback.onSurfaceAvailable();
        }
    }

    /**
     * Called at creation time. Implementors should inflate the hierarchy into the
     * parent ViewGroup, and return the View that actually hosts the surface.
     *
     * @param context a context
     * @param parent where to inflate
     * @return the view hosting the Surface
     */
    @NonNull
    protected abstract T onCreateView(@NonNull Context context, @NonNull ViewGroup parent);

    /**
     * Returns the view hosting the Surface.
     * @return the view
     */
    @NonNull
    public final T getView() {
        return mView;
    }

    /**
     * For testing purposes, should return the root view that was inflated into the
     * parent during {@link #onCreateView(Context, ViewGroup)}.
     * @return the root view
     */
    @SuppressWarnings("unused")
    @VisibleForTesting
    @NonNull
    abstract View getRootView();

    /**
     * Returns the output surface object (for example a SurfaceHolder
     * or a SurfaceTexture).
     * @return the surface object
     */
    @NonNull
    public abstract Output getOutput();

    /**
     * Returns the type of the output returned by {@link #getOutput()}.
     * @return the output type
     */
    @NonNull
    public abstract Class<Output> getOutputClass();

    /**
     * Called to notify the preview of the input stream size. The width and height must be
     * rotated before calling this, if needed, to be consistent with the VIEW reference.
     *
     * @param width width of the preview stream, in view coordinates
     * @param height height of the preview stream, in view coordinates
     */
    public void setStreamSize(int width, int height) {
        LOG.i("setStreamSize:", "desiredW=", width, "desiredH=", height);
        mInputStreamWidth = width;
        mInputStreamHeight = height;
        if (mInputStreamWidth > 0 && mInputStreamHeight > 0) {
            crop(mCropTask);
        }
    }

    /**
     * Returns the current input stream size, in view coordinates.
     * @return the current input stream size
     */
    @SuppressWarnings("unused")
    @NonNull
    final Size getStreamSize() {
        return new Size(mInputStreamWidth, mInputStreamHeight);
    }

    /**
     * Returns the current output surface size, in view coordinates.
     * @return the current output surface size.
     */
    @NonNull
    public final Size getSurfaceSize() {
        return new Size(mOutputSurfaceWidth, mOutputSurfaceHeight);
    }

    /**
     * Whether we have a valid surface already.
     * @return whether we have a surface
     */
    public final boolean hasSurface() {
        return mOutputSurfaceWidth > 0 && mOutputSurfaceHeight > 0;
    }

    /**
     * Subclasses can call this to notify that the surface is available.
     * @param width surface width
     * @param height surface height
     */
    @SuppressWarnings("WeakerAccess")
    protected final void dispatchOnSurfaceAvailable(int width, int height) {
        LOG.i("dispatchOnSurfaceAvailable:", "w=", width, "h=", height);
        mOutputSurfaceWidth = width;
        mOutputSurfaceHeight = height;
        if (mOutputSurfaceWidth > 0 && mOutputSurfaceHeight > 0) {
            crop(mCropTask);
        }
        mSurfaceCallback.onSurfaceAvailable();
    }

    /**
     * Subclasses can call this to notify that the surface has changed.
     * @param width surface width
     * @param height surface height
     */
    @SuppressWarnings("WeakerAccess")
    protected final void dispatchOnSurfaceSizeChanged(int width, int height) {
        LOG.i("dispatchOnSurfaceSizeChanged:", "w=", width, "h=", height);
        if (width != mOutputSurfaceWidth || height != mOutputSurfaceHeight) {
            mOutputSurfaceWidth = width;
            mOutputSurfaceHeight = height;
            if (width > 0 && height > 0) {
                crop(mCropTask);
            }
            mSurfaceCallback.onSurfaceChanged();
        }
    }

    /**
     * Subclasses can call this to notify that the surface has been destroyed.
     */
    @SuppressWarnings("WeakerAccess")
    protected final void dispatchOnSurfaceDestroyed() {
        mOutputSurfaceWidth = 0;
        mOutputSurfaceHeight = 0;
        mSurfaceCallback.onSurfaceDestroyed();
    }

    /**
     * Called by the hosting {@link com.otaliastudios.cameraview.CameraView},
     * this is a lifecycle event.
     */
    public void onResume() {}

    /**
     * Called by the hosting {@link com.otaliastudios.cameraview.CameraView},
     * this is a lifecycle event.
     */
    public void onPause() {}

    /**
     * Called by the hosting {@link com.otaliastudios.cameraview.CameraView},
     * this is a lifecycle event.
     */
    public void onDestroy() {}

    /**
     * Here we must crop the visible part by applying a > 1 scale to one of our
     * dimensions. This way our internal aspect ratio (mOutputSurfaceWidth / mOutputSurfaceHeight)
     * will match the preview size aspect ratio (mInputStreamWidth / mInputStreamHeight).
     *
     * There might still be some absolute difference (e.g. same ratio but bigger / smaller).
     * However that should be already managed by the framework.
     */
    protected void crop(@NonNull Task<Void> task) {
        // The base implementation does not support cropping.
        task.start();
        task.end(null);
    }

    /**
     * Whether this preview implementation supports cropping.
     * The base implementation does not, but it is strongly recommended to do so.
     * @return true if cropping is supported
     */
    public boolean supportsCropping() {
        return false;
    }

    /**
     * Whether we are currently cropping the output.
     * If false, this means that the output image will match the visible bounds.
     * @return true if cropping
     */
    public boolean isCropping() {
        return mCropping;
    }
}

package com.otaliastudios.cameraview.preview;

import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.CameraEngine;
import com.otaliastudios.cameraview.engine.offset.Reference;
import com.otaliastudios.cameraview.size.Size;

/**
 * A CameraPreview takes in input stream from the {@link CameraEngine}, and streams it
 * into an output surface that belongs to the view hierarchy.
 *
 * @param <T> the type of view which hosts the content surface
 * @param <Output> the type of output, either {@link android.view.SurfaceHolder}
 *                 or {@link android.graphics.SurfaceTexture}
 */
public abstract class CameraPreview<T extends View, Output> {

    protected final static CameraLogger LOG
            = CameraLogger.create(CameraPreview.class.getSimpleName());

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

    protected interface CropCallback {
        void onCrop();
    }

    @VisibleForTesting CropCallback mCropCallback;
    private SurfaceCallback mSurfaceCallback;
    private T mView;
    @SuppressWarnings("WeakerAccess")
    protected boolean mCropping;

    // These are the surface dimensions in REF_VIEW.
    @SuppressWarnings("WeakerAccess")
    protected int mOutputSurfaceWidth;
    @SuppressWarnings("WeakerAccess")
    protected int mOutputSurfaceHeight;

    // These are the preview stream dimensions, in REF_VIEW.
    @SuppressWarnings("WeakerAccess")
    protected int mInputStreamWidth;
    @SuppressWarnings("WeakerAccess")
    protected int mInputStreamHeight;

    // The rotation, if any, to be applied when drawing.
    @SuppressWarnings("WeakerAccess")
    protected int mDrawRotation;

    /**
     * Creates a new preview.
     * @param context a context
     * @param parent where to inflate our view
     */
    public CameraPreview(@NonNull Context context, @NonNull ViewGroup parent) {
        mView = onCreateView(context, parent);
    }

    /**
     * Sets a callback to be notified of surface events (creation, change, destruction)
     * @param callback a callback
     */
    public void setSurfaceCallback(@Nullable SurfaceCallback callback) {
        if (hasSurface() && mSurfaceCallback != null) {
            mSurfaceCallback.onSurfaceDestroyed();
        }
        mSurfaceCallback = callback;
        if (hasSurface() && mSurfaceCallback != null) {
            mSurfaceCallback.onSurfaceAvailable();
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
    @NonNull
    public abstract View getRootView();

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
     * @param width width of the preview stream, in view coordinates
     * @param height height of the preview stream, in view coordinates
     */
    public void setStreamSize(int width, int height) {
        LOG.i("setStreamSize:", "desiredW=", width, "desiredH=", height);
        mInputStreamWidth = width;
        mInputStreamHeight = height;
        if (mInputStreamWidth > 0 && mInputStreamHeight > 0) {
            crop(mCropCallback);
        }
    }

    /**
     * Returns the current input stream size, in view coordinates.
     * @return the current input stream size
     */
    @VisibleForTesting
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
            crop(mCropCallback);
        }
        if (mSurfaceCallback != null) {
            mSurfaceCallback.onSurfaceAvailable();
        }
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
                crop(mCropCallback);
            }
            if (mSurfaceCallback != null) {
                mSurfaceCallback.onSurfaceChanged();
            }
        }
    }

    /**
     * Subclasses can call this to notify that the surface has been destroyed.
     */
    @SuppressWarnings("WeakerAccess")
    protected final void dispatchOnSurfaceDestroyed() {
        mOutputSurfaceWidth = 0;
        mOutputSurfaceHeight = 0;
        if (mSurfaceCallback != null) {
            mSurfaceCallback.onSurfaceDestroyed();
        }
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
    @CallSuper
    public void onDestroy() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            onDestroyView();
        } else {
            // Do this on the UI thread and wait.
            Handler ui = new Handler(Looper.getMainLooper());
            final TaskCompletionSource<Void> task = new TaskCompletionSource<>();
            ui.post(new Runnable() {
                @Override
                public void run() {
                    onDestroyView();
                    task.setResult(null);
                }
            });
            try { Tasks.await(task.getTask()); } catch (Exception ignore) {}
        }
    }

    /**
     * At this point we undo the work that was done during
     * {@link #onCreateView(Context, ViewGroup)}, which basically means removing the root view
     * from the hierarchy.
     */
    @SuppressWarnings("WeakerAccess")
    @UiThread
    protected void onDestroyView() {
        View root = getRootView();
        ViewParent parent = root.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(root);
        }
    }

    /**
     * Here we must crop the visible part by applying a scale greater than 1 to one of our
     * dimensions. This way our internal aspect ratio (mOutputSurfaceWidth / mOutputSurfaceHeight)
     * will match the preview size aspect ratio (mInputStreamWidth / mInputStreamHeight).
     *
     * There might still be some absolute difference (e.g. same ratio but bigger / smaller).
     * However that should be already managed by the framework.
     *
     * @param callback the callback
     */
    protected void crop(@Nullable CropCallback callback) {
        // The base implementation does not support cropping.
        if (callback != null) callback.onCrop();
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


    /**
     * Should be called after {@link #setStreamSize(int, int)}!
     *
     * Sets the rotation, if any, to be applied when drawing.
     * Sometimes we don't need this:
     * - In Camera1, the buffer producer sets our Surface size and rotates it based on the value
     *   that we pass to {@link android.hardware.Camera.Parameters#setDisplayOrientation(int)},
     *   so the stream that comes in is already rotated (if we apply SurfaceTexture transform).
     * - In Camera2, for {@link android.view.SurfaceView} based previews, apparently it just works
     *   out of the box. The producer might be doing something similar.
     *
     * But in all the other Camera2 cases, we need to apply this rotation when drawing the surface.
     * Seems that Camera1 can correctly rotate the stream/transform to {@link Reference#VIEW},
     * while Camera2, that does not have any rotation API, will only rotate to {@link Reference#BASE}.
     * That's why in Camera2 this angle is set as the offset between BASE and VIEW.
     *
     * @param drawRotation the rotation in degrees
     */
    public void setDrawRotation(int drawRotation) {
        mDrawRotation = drawRotation;
    }
}

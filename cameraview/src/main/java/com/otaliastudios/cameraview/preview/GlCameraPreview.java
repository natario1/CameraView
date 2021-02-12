package com.otaliastudios.cameraview.preview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.R;
import com.otaliastudios.cameraview.internal.GlTextureDrawer;
import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.filter.NoFilter;
import com.otaliastudios.cameraview.size.AspectRatio;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * - The android camera will stream image to the given {@link SurfaceTexture}.
 *
 * - in the SurfaceTexture constructor we pass the GL texture handle that we have created.
 *
 * - The SurfaceTexture is linked to the Camera1Engine object. The camera will pass down
 *   buffers of data with a specified size (that is, the Camera1Engine preview size).
 *   For this reason we don't have to specify surfaceTexture.setDefaultBufferSize()
 *   (like we do, for example, in Snapshot1PictureRecorder).
 *
 * - When SurfaceTexture.updateTexImage() is called, it will fetch the latest texture image from the
 *   camera stream and assign it to the GL texture that was passed.
 *   Now the GL texture must be drawn using draw* APIs. The SurfaceTexture will also give us
 *   the transformation matrix to be applied.
 *
 * - The easy way to render an OpenGL texture is using the {@link GLSurfaceView} class.
 *   It manages the GL context, hosts a surface and runs a separated rendering thread that will
 *   perform the rendering.
 *
 * - As per docs, we ask the GLSurfaceView to delegate rendering to us, using
 *   {@link GLSurfaceView#setRenderer(GLSurfaceView.Renderer)}. We request a render on the
 *   SurfaceView anytime the SurfaceTexture notifies that it has new data available
 *   (see OnFrameAvailableListener below).
 *
 * - So in short:
 *   - The SurfaceTexture has buffers of data of mInputStreamSize
 *   - The SurfaceView hosts a view (and a surface) of size mOutputSurfaceSize.
 *     These are determined by the CameraView.onMeasure method.
 *   - We have a GL rich texture to be drawn (in the given method and thread).
 *
 * This class will provide rendering callbacks to anyone who registers a
 * {@link RendererFrameCallback}. Callbacks are guaranteed to be called on the renderer thread,
 * which means that we can fetch the GL context that was created and is managed
 * by the {@link GLSurfaceView}.
 */
public class GlCameraPreview extends CameraPreview<GLSurfaceView, SurfaceTexture>
        implements FilterCameraPreview, RendererCameraPreview {

    private boolean mDispatched;
    private SurfaceTexture mInputSurfaceTexture;
    private GlTextureDrawer mOutputTextureDrawer;
    // A synchronized set was not enough to avoid crashes, probably due to external classes
    // removing the callback while this set is being iterated. CopyOnWriteArraySet solves this.
    private final Set<RendererFrameCallback> mRendererFrameCallbacks = new CopyOnWriteArraySet<>();
    @VisibleForTesting float mCropScaleX = 1F;
    @VisibleForTesting float mCropScaleY = 1F;
    private View mRootView;
    private Filter mCurrentFilter;

    public GlCameraPreview(@NonNull Context context, @NonNull ViewGroup parent) {
        super(context, parent);
    }

    @NonNull
    @Override
    protected GLSurfaceView onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        ViewGroup root = (ViewGroup) LayoutInflater.from(context)
                .inflate(R.layout.cameraview_gl_view, parent, false);
        final GLSurfaceView glView = root.findViewById(R.id.gl_surface_view);
        final Renderer renderer = instantiateRenderer();
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glView.getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {}
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                dispatchOnSurfaceDestroyed();
                glView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        renderer.onSurfaceDestroyed();
                    }
                });
                mDispatched = false;
            }
        });
        parent.addView(root, 0);
        mRootView = root;
        return glView;
    }

    @NonNull
    @Override
    public View getRootView() {
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        getView().onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // View is gone, so EGL context is gone: callbacks make no sense anymore.
        mRendererFrameCallbacks.clear();
    }

    /**
     * The core renderer that performs the actual drawing operations.
     */
    public class Renderer implements GLSurfaceView.Renderer {

        @RendererThread
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            if (mCurrentFilter == null) {
                mCurrentFilter = new NoFilter();
            }
            mOutputTextureDrawer = new GlTextureDrawer();
            mOutputTextureDrawer.setFilter(mCurrentFilter);
            final int textureId = mOutputTextureDrawer.getTexture().getId();
            mInputSurfaceTexture = new SurfaceTexture(textureId);
            getView().queueEvent(new Runnable() {
                @Override
                public void run() {
                    for (RendererFrameCallback callback : mRendererFrameCallbacks) {
                        callback.onRendererTextureCreated(textureId);
                    }
                }
            });

            // Since we are using GLSurfaceView.RENDERMODE_WHEN_DIRTY, we must notify
            // the SurfaceView of dirtyness, so that it draws again. This is how it's done.
            mInputSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    getView().requestRender(); // requestRender is thread-safe.
                }
            });
        }

        @SuppressWarnings("WeakerAccess")
        @RendererThread
        public void onSurfaceDestroyed() {
            if (mInputSurfaceTexture != null) {
                mInputSurfaceTexture.setOnFrameAvailableListener(null);
                mInputSurfaceTexture.release();
                mInputSurfaceTexture = null;
            }
            if (mOutputTextureDrawer != null) {
                mOutputTextureDrawer.release();
                mOutputTextureDrawer = null;
            }
        }

        @RendererThread
        @Override
        public void onSurfaceChanged(GL10 gl, final int width, final int height) {
            gl.glViewport(0, 0, width, height);
            mCurrentFilter.setSize(width, height);
            if (!mDispatched) {
                dispatchOnSurfaceAvailable(width, height);
                mDispatched = true;
            } else if (width != mOutputSurfaceWidth || height != mOutputSurfaceHeight) {
                dispatchOnSurfaceSizeChanged(width, height);
            }
        }

        @RendererThread
        @Override
        public void onDrawFrame(GL10 gl) {
            if (mInputSurfaceTexture == null) return;
            if (mInputStreamWidth <= 0 || mInputStreamHeight <= 0) {
                // Skip drawing. Camera was not opened.
                return;
            }

            // Latch the latest frame. If there isn't anything new,
            // we'll just re-use whatever was there before.
            final float[] transform = mOutputTextureDrawer.getTextureTransform();
            mInputSurfaceTexture.updateTexImage();
            mInputSurfaceTexture.getTransformMatrix(transform);
            // LOG.v("onDrawFrame:", "timestamp:", mInputSurfaceTexture.getTimestamp());

            // For Camera2, apply the draw rotation.
            // See TextureCameraPreview.setDrawRotation() for info.
            if (mDrawRotation != 0) {
                Matrix.translateM(transform, 0, 0.5F, 0.5F, 0);
                Matrix.rotateM(transform, 0, mDrawRotation, 0, 0, 1);
                Matrix.translateM(transform, 0, -0.5F, -0.5F, 0);
            }

            if (isCropping()) {
                // Scaling is easy, but we must also translate before:
                // If the view is 10x1000 (very tall), it will show only the left strip
                // of the preview (not the center one).
                // If the view is 1000x10 (very large), it will show only the bottom strip
                // of the preview (not the center one).
                float translX = (1F - mCropScaleX) / 2F;
                float translY = (1F - mCropScaleY) / 2F;
                Matrix.translateM(transform, 0, translX, translY, 0);
                Matrix.scaleM(transform, 0, mCropScaleX, mCropScaleY, 1);
            }

            mOutputTextureDrawer.draw(mInputSurfaceTexture.getTimestamp() / 1000L);
            for (RendererFrameCallback callback : mRendererFrameCallbacks) {
                callback.onRendererFrame(mInputSurfaceTexture, mDrawRotation, mCropScaleX, mCropScaleY);
            }
        }
    }

    @NonNull
    @Override
    public Class<SurfaceTexture> getOutputClass() {
        return SurfaceTexture.class;
    }

    @NonNull
    @Override
    public SurfaceTexture getOutput() {
        return mInputSurfaceTexture;
    }

    @Override
    public boolean supportsCropping() {
        return true;
    }

    /**
     * To crop in GL, we could actually use view.setScaleX and setScaleY, but only from Android N
     * onward. See documentation: https://developer.android.com/reference/android/view/SurfaceView
     *
     *   Note: Starting in platform version Build.VERSION_CODES.N, SurfaceView's window position
     *   is updated synchronously with other View rendering. This means that translating and scaling
     *   a SurfaceView on screen will not cause rendering artifacts. Such artifacts may occur on
     *   previous versions of the platform when its window is positioned asynchronously.
     *
     * But to support older platforms, this seem to work - computing scale values and requesting
     * a new frame, then drawing it with a scaled transformation matrix.
     * See {@link Renderer#onDrawFrame(GL10)}.
     */
    @Override
    protected void crop(@Nullable final CropCallback callback) {
        if (mInputStreamWidth > 0 && mInputStreamHeight > 0 && mOutputSurfaceWidth > 0
                && mOutputSurfaceHeight > 0) {
            float scaleX = 1f, scaleY = 1f;
            AspectRatio current = AspectRatio.of(mOutputSurfaceWidth, mOutputSurfaceHeight);
            AspectRatio target = AspectRatio.of(mInputStreamWidth, mInputStreamHeight);
            if (current.toFloat() >= target.toFloat()) {
                // We are too short. Must increase height.
                scaleY = current.toFloat() / target.toFloat();
            } else {
                // We must increase width.
                scaleX = target.toFloat() / current.toFloat();
            }
            mCropping = scaleX > 1.02f || scaleY > 1.02f;
            mCropScaleX = 1F / scaleX;
            mCropScaleY = 1F / scaleY;
            getView().requestRender();
        }
        if (callback != null) callback.onCrop();
    }

    @Override
    public void addRendererFrameCallback(@NonNull final RendererFrameCallback callback) {
        getView().queueEvent(new Runnable() {
            @Override
            public void run() {
                mRendererFrameCallbacks.add(callback);
                if (mOutputTextureDrawer != null) {
                    int textureId = mOutputTextureDrawer.getTexture().getId();
                    callback.onRendererTextureCreated(textureId);
                }
                callback.onRendererFilterChanged(mCurrentFilter);
            }
        });
    }

    @Override
    public void removeRendererFrameCallback(@NonNull final RendererFrameCallback callback) {
        mRendererFrameCallbacks.remove(callback);
    }

    /**
     * Returns the output GL texture id.
     * @return the output GL texture id
     */
    @SuppressWarnings("unused")
    protected int getTextureId() {
        return mOutputTextureDrawer != null ? mOutputTextureDrawer.getTexture().getId() : -1;
    }

    /**
     * Creates the renderer for this GL surface.
     * @return the renderer for this GL surface
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected Renderer instantiateRenderer() {
        return new Renderer();
    }

    //region Filters


    @NonNull
    @Override
    public Filter getCurrentFilter() {
        return mCurrentFilter;
    }

    @Override
    public void setFilter(final @NonNull Filter filter) {
        mCurrentFilter = filter;
        if (hasSurface()) {
            filter.setSize(mOutputSurfaceWidth, mOutputSurfaceHeight);
        }

        getView().queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mOutputTextureDrawer != null) {
                    mOutputTextureDrawer.setFilter(filter);
                }
                for (RendererFrameCallback callback : mRendererFrameCallbacks) {
                    callback.onRendererFilterChanged(filter);
                }
            }
        });
    }
}

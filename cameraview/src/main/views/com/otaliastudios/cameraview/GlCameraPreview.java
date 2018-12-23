package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * - The android camera will stream image to the given {@link SurfaceTexture}.
 *
 * - in the SurfaceTexture constructor we pass the GL texture handle that we have created.
 *
 * - The SurfaceTexture is linked to the Camera1 object. The camera will pass down buffers of data with
 *   a specified size (that is, the Camera1 preview size). For this reason we don't have to specify
 *   surfaceTexture.setDefaultBufferSize() (like we do, for example, in SnapshotPictureRecorder).
 *
 * - When SurfaceTexture.updateTexImage() is called, it will fetch the latest texture image from the
 *   camera stream and assign it to the GL texture that was passed.
 *   Now the GL texture must be drawn using draw* APIs. The SurfaceTexture will also give us
 *   the transformation matrix to be applied.
 *
 * - The easy way to render an OpenGL texture is using the {@link GLSurfaceView} class.
 *   It manages the GL context, hosts a surface and runs a separated rendering thread that will perform
 *   the rendering.
 *
 * - As per docs, we ask the GLSurfaceView to delegate rendering to us, using
 *   {@link GLSurfaceView#setRenderer(GLSurfaceView.Renderer)}. We request a render on the SurfaceView
 *   anytime the SurfaceTexture notifies that it has new data available (see OnFrameAvailableListener below).
 *
 * - So in short:
 *   - The SurfaceTexture has buffers of data of mInputStreamSize
 *   - The SurfaceView hosts a view (and a surface) of size mOutputSurfaceSize.
 *     These are determined by the CameraView.onMeasure method.
 *   - We have a GL rich texture to be drawn (in the given method & thread).
 *
 * This class will provide rendering callbacks to anyone who registers a {@link RendererFrameCallback}.
 * Callbacks are guaranteed to be called on the renderer thread, which means that we can fetch
 * the GL context that was created and is managed by the {@link GLSurfaceView}.
 */
class GlCameraPreview extends CameraPreview<GLSurfaceView, SurfaceTexture> implements GLSurfaceView.Renderer {

    private boolean mDispatched;
    private final float[] mTransformMatrix = new float[16];
    private int mOutputTextureId = 0;
    private SurfaceTexture mInputSurfaceTexture;
    private EglViewport mOutputViewport;
    private Set<RendererFrameCallback> mRendererFrameCallbacks = Collections.synchronizedSet(new HashSet<RendererFrameCallback>());
    /* for tests */ float mScaleX = 1F;
    /* for tests */ float mScaleY = 1F;

    private View mRootView;

    GlCameraPreview(@NonNull Context context, @NonNull ViewGroup parent, @Nullable SurfaceCallback callback) {
        super(context, parent, callback);
    }

    @NonNull
    @Override
    protected GLSurfaceView onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        ViewGroup root = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.cameraview_gl_view, parent, false);
        GLSurfaceView glView = root.findViewById(R.id.gl_surface_view);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(this);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        // Tried these 2 to remove the black background, does not work.
        // glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // glView.setZOrderMediaOverlay(true);
        glView.getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {}
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                dispatchOnOutputSurfaceDestroyed();
                mDispatched = false;
            }
        });
        parent.addView(root, 0);
        mRootView = root;
        return glView;
    }

    @NonNull
    @Override
    View getRootView() {
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
        if (mInputSurfaceTexture != null) {
            mInputSurfaceTexture.setOnFrameAvailableListener(null);
            mInputSurfaceTexture.release();
            mInputSurfaceTexture = null;
        }
        mOutputTextureId = 0;
        if (mOutputViewport != null) {
            mOutputViewport.release();
            mOutputViewport = null;
        }
    }

    @RendererThread
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mOutputViewport = new EglViewport();
        mOutputTextureId = mOutputViewport.createTexture();
        mInputSurfaceTexture = new SurfaceTexture(mOutputTextureId);
        getView().queueEvent(new Runnable() {
            @Override
            public void run() {
                for (RendererFrameCallback callback : mRendererFrameCallbacks) {
                    callback.onRendererTextureCreated(mOutputTextureId);
                }
            }
        });

        // Since we are using GLSurfaceView.RENDERMODE_WHEN_DIRTY, we must notify the SurfaceView
        // of dirtyness, so that it draws again. This is how it's done.
        mInputSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                getView().requestRender(); // requestRender is thread-safe.
            }
        });
    }

    @RendererThread
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onSurfaceChanged(GL10 gl, final int width, final int height) {
        if (!mDispatched) {
            dispatchOnOutputSurfaceAvailable(width, height);
            mDispatched = true;
        } else if (mOutputSurfaceWidth == width && mOutputSurfaceHeight == height) {
            // I was experimenting and this was happening.
            // Not sure if it is stil needed now.
        } else {
            // With other CameraPreview implementation we could just dispatch the 'size changed' event
            // to the controller and everything would go straight. In case of GL, apparently we have to
            // force recreate the EGLContext by calling onPause and onResume in the UI thread.
            dispatchOnOutputSurfaceDestroyed();
            getView().post(new Runnable() {
                @Override
                public void run() {
                    getView().onPause();
                    getView().onResume();
                    dispatchOnOutputSurfaceAvailable(width, height);
                }
            });
        }
    }

    @RendererThread
    @Override
    public void onDrawFrame(GL10 gl) {
        // Latch the latest frame.  If there isn't anything new,
        // we'll just re-use whatever was there before.
        mInputSurfaceTexture.updateTexImage();
        if (mInputStreamWidth <= 0 || mInputStreamHeight <= 0) {
            // Skip drawing. Camera was not opened.
            return;
        }

        mInputSurfaceTexture.getTransformMatrix(mTransformMatrix);
        if (isCropping()) {
            // Scaling is easy. However:
            // If the view is 10x1000 (very tall), it will show only the left strip of the preview (not the center one).
            // If the view is 1000x10 (very large), it will show only the bottom strip of the preview (not the center one).
            // So we must use Matrix.translateM, and it must happen before the crop.
            float translX = (1F - mScaleX) / 2F;
            float translY = (1F - mScaleY) / 2F;
            Matrix.translateM(mTransformMatrix, 0, translX, translY, 0);
            Matrix.scaleM(mTransformMatrix, 0, mScaleX, mScaleY, 1);
        }
        // Future note: passing scale to the viewport?
        // They are scaleX an scaleY, but flipped based on mInputFlipped.
        mOutputViewport.drawFrame(mOutputTextureId, mTransformMatrix);
        for (RendererFrameCallback callback : mRendererFrameCallbacks) {
            callback.onRendererFrame(mInputSurfaceTexture, mScaleX, mScaleY);
        }
    }

    @NonNull
    @Override
    Class<SurfaceTexture> getOutputClass() {
        return SurfaceTexture.class;
    }

    @NonNull
    @Override
    SurfaceTexture getOutput() {
        return mInputSurfaceTexture;
    }

    @Override
    boolean supportsCropping() {
        return true;
    }

    /**
     * To crop in GL, we could actually use view.setScaleX and setScaleY, but only from Android N onward.
     * See documentation: https://developer.android.com/reference/android/view/SurfaceView
     *
     *   Note: Starting in platform version Build.VERSION_CODES.N, SurfaceView's window position is updated
     *   synchronously with other View rendering. This means that translating and scaling a SurfaceView on
     *   screen will not cause rendering artifacts. Such artifacts may occur on previous versions of the
     *   platform when its window is positioned asynchronously.
     *
     * But to support older platforms, this seem to work - computing scale values and requesting a new frame,
     * then drawing it with a scaled transformation matrix. See {@link #onDrawFrame(GL10)}.
     */
    @Override
    protected void crop() {
        mCropTask.start();
        if (mInputStreamWidth > 0 && mInputStreamHeight > 0 && mOutputSurfaceWidth > 0 && mOutputSurfaceHeight > 0) {
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
            mScaleX = 1F / scaleX;
            mScaleY = 1F / scaleY;
            getView().requestRender();
        }
        mCropTask.end(null);
    }

    interface RendererFrameCallback {

        /**
         * Called on the renderer thread, hopefully only once, to notify that
         * the texture was created (or to inform a new callback of the old texture).
         *
         * @param textureId the GL texture linked to the image stream
         */
        @RendererThread
        void onRendererTextureCreated(int textureId);

        /**
         * Called on the renderer thread after each frame was drawn.
         * You are not supposed to hold for too long onto this thread, because
         * well, it is the rendering thread.
         *
         * @param surfaceTexture the texture to get transformation
         * @param scaleX the scaleX (in REF_VIEW) value
         * @param scaleY the scaleY (in REF_VIEW) value
         */
        @RendererThread
        void onRendererFrame(SurfaceTexture surfaceTexture, float scaleX, float scaleY);
    }

    void addRendererFrameCallback(@NonNull final RendererFrameCallback callback) {
        getView().queueEvent(new Runnable() {
            @Override
            public void run() {
                mRendererFrameCallbacks.add(callback);
                if (mOutputTextureId != 0) callback.onRendererTextureCreated(mOutputTextureId);
            }
        });
    }

    void removeRendererFrameCallback(@NonNull final RendererFrameCallback callback) {
        mRendererFrameCallbacks.remove(callback);
    }
}

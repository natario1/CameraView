package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * - The android camera will stream image to the given {@link SurfaceTexture}.
 *
 * - in the SurfaceTexture constructor we pass the GL texture handle that we have created.
 *
 * - The SurfaceTexture is linked to the Camera1 object. It will pass down buffers of data with
 *   a specified size (that is, the Camera1 preview size).
 *
 * - When SurfaceTexture.updateTexImage() is called, it will take the latest image from the camera stream
 *   and update it into the GL texture that was passed.
 *
 * - Now we have a GL texture referencing data. It must be drawn.
 *  [Note: it must be drawn using a transformation matrix taken from SurfaceTexture]
 *
 * - The easy way to render an OpenGL texture is using the {@link GLSurfaceView} class.
 *   It manages the gl context, hosts a surface and runs a separated rendering thread that will perform
 *   the rendering.
 *
 * - As per docs, we ask the GLSurfaceView to delegate rendering to us, using
 *   {@link GLSurfaceView#setRenderer(GLSurfaceView.Renderer)}. We request a render on the SurfaceView
 *   anytime the SurfaceTexture notifies that it has new data available (see OnFrameAvailableListener below).
 *
 * - Everything is linked:
 *   - The SurfaceTexture has buffers of data of mInputStreamSize
 *   - The SurfaceView hosts a view (and surface) of size mOutputSurfaceSize
 *   - We have a GL rich texture to be drawn (in the given method & thread).
 *
 * TODO
 * CROPPING: Managed to do this using Matrix transformation.
 * UPDATING: Managed to work using view.onPause and onResume.
 * TAKING PICTURES: Sometime the snapshot takes ages... Can't reproduce anymore. Cool.
 * TAKING VIDEOS: Still have not tried...
 */
class GLCameraPreview extends CameraPreview<GLSurfaceView, SurfaceTexture> implements GLSurfaceView.Renderer {

    private boolean mDispatched;
    private final float[] mTransformMatrix = new float[16];
    private int mOutputTextureId = -1;
    private SurfaceTexture mInputSurfaceTexture;
    private EglViewport mOutputViewport;
    private RendererFrameCallback mRendererFrameCallback;

    GLCameraPreview(Context context, ViewGroup parent, SurfaceCallback callback) {
        super(context, parent, callback);
    }

    @NonNull
    @Override
    protected GLSurfaceView onCreateView(Context context, ViewGroup parent) {
        View root = LayoutInflater.from(context).inflate(R.layout.cameraview_gl_view, parent, false);
        parent.addView(root, 0);
        GLSurfaceView glView = root.findViewById(R.id.gl_surface_view);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(this);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glView.getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {}
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                dispatchOnOutputSurfaceDestroyed();
            }
        });
        return glView;
    }

    @Override
    void onResume() {
        super.onResume();
        getView().onResume();
    }

    @Override
    void onPause() {
        super.onPause();
        getView().onPause();
    }

    @Override
    void onDestroy() {
        super.onDestroy();
        releaseInputSurfaceTexture();
    }

    private void releaseInputSurfaceTexture() {
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

    private void createInputSurfaceTexture() {
        mOutputViewport = new EglViewport();
        mOutputTextureId = mOutputViewport.createTexture();
        mInputSurfaceTexture = new SurfaceTexture(mOutputTextureId);

        // Since we are using GLSurfaceView.RENDERMODE_WHEN_DIRTY, we must notify the SurfaceView
        // of dirtyness, so that it draws again. This is how it's done.
        mInputSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                // requestRender is thread-safe.
                getView().requestRender();
            }
        });
    }

    // Renderer thread
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        createInputSurfaceTexture();
    }

    // Renderer thread
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onSurfaceChanged(GL10 gl, final int width, final int height) {
        if (!mDispatched) {
            dispatchOnOutputSurfaceAvailable(width, height);
            mDispatched = true;
        } else if (mOutputSurfaceWidth == width && mOutputSurfaceHeight == height) {
                // This change can be triggered by ourselves (see below). Ignore.
        } else {
            // With other CameraPreview implementation we could just dispatch the 'size changed' event
            // to the controller and everything would go straight. In case of GL, apparently we have to:
            // - create a new texture (release the old)
            // - unbind camera and surface
            // - stop camera preview
            // - recreate the GL context using view.onPause() and onResume()
            // ...
            onSizeChangeImplementation4(width, height);
        }
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        // This are only needed with some implementations,
        // and implementation4 seems to work well without them.
        // if (mInputSurfaceTexture == null) return;
        // if (mOutputViewport == null) return;

        // Latch the latest frame.  If there isn't anything new,
        // we'll just re-use whatever was there before.
        mInputSurfaceTexture.updateTexImage();
        if (mInputStreamWidth <= 0 || mInputStreamHeight <= 0) {
            // Skip drawing. Camera was not opened.
            return;
        }

        if (mRendererFrameCallback != null) {
            mRendererFrameCallback.onRendererFrame(mInputSurfaceTexture);
        }

        // Draw the video frame.
        mInputSurfaceTexture.getTransformMatrix(mTransformMatrix);
        if (isCropping()) {
            Matrix.scaleM(mTransformMatrix, 0, mScaleX, mScaleY, 1);
        }
        mOutputViewport.drawFrame(mOutputTextureId, mTransformMatrix);
    }

    @Override
    Class<SurfaceTexture> getOutputClass() {
        return SurfaceTexture.class;
    }

    @Override
    SurfaceTexture getOutput() {
        return mInputSurfaceTexture;
    }

    @Override
    boolean supportsCropping() {
        return true;
    }

    /* for tests */ float mScaleX = 1F;
    /* for tests */ float mScaleY = 1F;

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


    // This does work but looks like a lot of stuff.
    private void onSizeChangeImplementation1(final int width, final int height) {
        releaseInputSurfaceTexture();
        dispatchOnOutputSurfaceDestroyed();
        getView().post(new Runnable() {
            @Override
            public void run() {
                getView().onPause();
                getView().onResume();
                getView().queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        createInputSurfaceTexture();
                        dispatchOnOutputSurfaceAvailable(width, height);
                    }
                });
            }
        });
    }

    // This does not work. We get: startPreview failed.
    private void onSizeChangeImplementation2(final int width, final int height) {
        releaseInputSurfaceTexture();
        getView().post(new Runnable() {
            @Override
            public void run() {
                getView().onPause();
                getView().onResume();
                getView().queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        createInputSurfaceTexture();
                        dispatchOnOutputSurfaceSizeChanged(width, height);
                    }
                });
            }
        });
    }

    // Works! So we don't need to recreate the GL texture.
    private void onSizeChangeImplementation3(final int width, final int height) {
        dispatchOnOutputSurfaceDestroyed();
        getView().post(new Runnable() {
            @Override
            public void run() {
                getView().onPause();
                getView().onResume();
                getView().queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        dispatchOnOutputSurfaceAvailable(width, height);
                    }
                });
            }
        });
    }

    // Works! This is getting easy.
    private void onSizeChangeImplementation4(final int width, final int height) {
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

    // Does not work. onPause and onResume must be called on the UI thread.
    // This make sense.
    private void onSizeChangeImplementation5(final int width, final int height) {
        dispatchOnOutputSurfaceDestroyed();
        getView().onPause();
        getView().onResume();
        dispatchOnOutputSurfaceAvailable(width, height);
    }

    // Does NOT work. The EGL context must be recreated
    // for this to work out.
    private void onSizeChangeImplementation6(final int width, final int height) {
        dispatchOnOutputSurfaceDestroyed();
        getView().post(new Runnable() {
            @Override
            public void run() {
                getView().setPreserveEGLContextOnPause(true);
                getView().onPause();
                getView().onResume();
                getView().setPreserveEGLContextOnPause(false);
                dispatchOnOutputSurfaceAvailable(width, height);
            }
        });
    }

    interface RendererFrameCallback {
        // Renderer thread.
        void onRendererTextureCreated(int textureId);

        // Renderer thread.
        void onRendererFrame(SurfaceTexture surfaceTexture);
    }

    void setRendererFrameCallback(@Nullable RendererFrameCallback callback) {
        mRendererFrameCallback = callback;
        if (mRendererFrameCallback != null && mOutputTextureId != 0) {
            mRendererFrameCallback.onRendererTextureCreated(mOutputTextureId);
        }
    }
}

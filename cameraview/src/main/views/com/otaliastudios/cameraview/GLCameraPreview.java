package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class GLCameraPreview extends CameraPreview<GLSurfaceView, SurfaceTexture> implements GLSurfaceView.Renderer {

    private boolean mDispatched;
    private final float[] mTransformMatrix = new float[16];
    private int mTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    private GLViewport mViewport;

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
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                onSurfaceDestroyed();
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
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mViewport != null) {
            mViewport.release();
            mViewport = null;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mViewport = new GLViewport();
        mTextureId = mViewport.createTexture();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                // requestRender is thread-safe.
                getView().requestRender();
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (!mDispatched) {
            onSurfaceAvailable(width, height);
            mDispatched = true;
        } else {
            onSurfaceSizeChanged(width, height);
        }
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        // Latch the latest frame.  If there isn't anything new,
        // we'll just re-use whatever was there before.
        mSurfaceTexture.updateTexImage();
        if (mDesiredWidth <= 0 || mDesiredHeight <= 0) {
            // Skip drawing. Camera was not opened.
            return;
        }

        // Draw the video frame.
        mSurfaceTexture.getTransformMatrix(mTransformMatrix);
        mViewport.drawFrame(mTextureId, mTransformMatrix);
    }

    @Override
    Class<SurfaceTexture> getOutputClass() {
        return SurfaceTexture.class;
    }

    @Override
    SurfaceTexture getOutput() {
        return mSurfaceTexture;
    }


    @Override
    boolean supportsCropping() {
        return true;
    }

    @Override
    protected void crop() {
        mCropTask.start();
        mCropTask.end(null);
    }
}

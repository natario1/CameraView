package com.otaliastudios.cameraview.internal;


import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.filter.NoFilter;
import com.otaliastudios.opengl.core.Egloo;
import com.otaliastudios.opengl.program.GlProgram;
import com.otaliastudios.opengl.texture.GlTexture;


public class GlTextureDrawer {

    private final static String TAG = GlTextureDrawer.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private final static int TEXTURE_TARGET = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    private final static int TEXTURE_UNIT = GLES20.GL_TEXTURE0;

    private final GlTexture mTexture;
    private float[] mTextureTransform = Egloo.IDENTITY_MATRIX.clone();

    @NonNull
    private Filter mFilter = new NoFilter();
    private Filter mPendingFilter = null;
    private int mProgramHandle = -1;

    @SuppressWarnings("unused")
    public GlTextureDrawer() {
        this(new GlTexture(TEXTURE_UNIT, TEXTURE_TARGET));
    }

    @SuppressWarnings("unused")
    public GlTextureDrawer(int textureId) {
        this(new GlTexture(TEXTURE_UNIT, TEXTURE_TARGET, textureId));
    }

    @SuppressWarnings("WeakerAccess")
    public GlTextureDrawer(@NonNull GlTexture texture) {
        mTexture = texture;
    }

    public void setFilter(@NonNull Filter filter) {
        mPendingFilter = filter;
    }

    @NonNull
    public GlTexture getTexture() {
        return mTexture;
    }

    @NonNull
    public float[] getTextureTransform() {
        return mTextureTransform;
    }

    public void setTextureTransform(@NonNull float[] textureTransform) {
        mTextureTransform = textureTransform;
    }

    public void draw(final long timestampUs) {
        if (mPendingFilter != null) {
            release();
            mFilter = mPendingFilter;
            mPendingFilter = null;

        }

        if (mProgramHandle == -1) {
            mProgramHandle = GlProgram.create(
                    mFilter.getVertexShader(),
                    mFilter.getFragmentShader());
            mFilter.onCreate(mProgramHandle);
            Egloo.checkGlError("program creation");
        }

        GLES20.glUseProgram(mProgramHandle);
        Egloo.checkGlError("glUseProgram(handle)");
        mTexture.bind();
        mFilter.draw(timestampUs, mTextureTransform);
        mTexture.unbind();
        GLES20.glUseProgram(0);
        Egloo.checkGlError("glUseProgram(0)");
    }

    public void release() {
        if (mProgramHandle == -1) return;
        mFilter.onDestroy();
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }
}

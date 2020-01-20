package com.otaliastudios.cameraview.internal.egl;


import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.filter.NoFilter;
import com.otaliastudios.opengl.program.GlProgram;


public class EglViewport {

    private final static CameraLogger LOG = CameraLogger.create(EglViewport.class.getSimpleName());

    private int mProgramHandle = -1;
    private int mTextureTarget;
    private int mTextureUnit;

    private Filter mFilter;
    private Filter mPendingFilter;

    public EglViewport() {
        this(new NoFilter());
    }

    public EglViewport(@NonNull Filter filter) {
        mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        mTextureUnit = GLES20.GL_TEXTURE0;
        mFilter = filter;
        createProgram();
    }

    private void createProgram() {
        mProgramHandle = GlProgram.create(mFilter.getVertexShader(),
                mFilter.getFragmentShader());
        mFilter.onCreate(mProgramHandle);
    }

    public void release() {
        if (mProgramHandle != -1) {
            mFilter.onDestroy();
            GLES20.glDeleteProgram(mProgramHandle);
            mProgramHandle = -1;
        }
    }

    public int createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        Egloo.checkGlError("glGenTextures");

        int texId = textures[0];
        GLES20.glActiveTexture(mTextureUnit);
        GLES20.glBindTexture(mTextureTarget, texId);
        Egloo.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        Egloo.checkGlError("glTexParameter");

        return texId;
    }

    public void setFilter(@NonNull Filter filter) {
        // TODO see if this is needed. If setFilter is always called from the correct GL thread,
        // we don't need to wait for a new draw call (which might not even happen).
        mPendingFilter = filter;
    }

    public void draw(long timestampUs, int textureId, float[] textureMatrix) {
        if (mPendingFilter != null) {
            release();
            mFilter = mPendingFilter;
            mPendingFilter = null;
            createProgram();
        }

        Egloo.checkGlError("draw start");

        // Select the program and the active texture.
        GLES20.glUseProgram(mProgramHandle);
        Egloo.checkGlError("glUseProgram");
        GLES20.glActiveTexture(mTextureUnit);
        GLES20.glBindTexture(mTextureTarget, textureId);

        // Draw.
        mFilter.draw(timestampUs, textureMatrix);

        // Release.
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }
}

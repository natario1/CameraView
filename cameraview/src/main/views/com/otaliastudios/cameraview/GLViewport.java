package com.otaliastudios.cameraview;


import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.FloatBuffer;

class GLViewport extends GLElement {

    private final static CameraLogger LOG = CameraLogger.create(GLViewport.class.getSimpleName());

    // Simple vertex shader.
    private static final String SIMPLE_VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    // Simple fragment shader for use with external 2D textures
    // (e.g. what we get from SurfaceTexture).
    private static final String SIMPLE_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    // Stuff from Drawable2d.FULL_RECTANGLE
    // A full square, extending from -1 to +1 in both dimensions.
    // When the model/view/projection matrix is identity, this will exactly cover the viewport.
    private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f,  1.0f,   // 2 top left
            1.0f,  1.0f,   // 3 top right
    };

    // Stuff from Drawable2d.FULL_RECTANGLE
    // A full square, extending from -1 to +1 in both dimensions.
    private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };

    // Stuff from Drawable2d.FULL_RECTANGLE
    private FloatBuffer mVertexArray = floatBuffer(FULL_RECTANGLE_COORDS);
    private FloatBuffer mTexCoordArray = floatBuffer(FULL_RECTANGLE_TEX_COORDS);
    private int mVertexCount = FULL_RECTANGLE_COORDS.length / 2;
    private final int mCoordsPerVertex = 2;
    private final int mVertexStride = 8;
    private final int mTexCoordStride = 8;

    // Stuff from Texture2dProgram
    private int mProgramHandle;
    private int mTextureTarget;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;

    // private int muKernelLoc; // Used for filtering
    // private int muTexOffsetLoc; // Used for filtering
    // private int muColorAdjustLoc; // Used for filtering

    GLViewport() {
        mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        mProgramHandle = createProgram(SIMPLE_VERTEX_SHADER, SIMPLE_FRAGMENT_SHADER);
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        checkLocation(muTexMatrixLoc, "uTexMatrix");

        // Stuff from Drawable2d.FULL_RECTANGLE

    }


    void release() {
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    int createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        check("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
        check("glBindTexture " + texId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        check("glTexParameter");

        return texId;
    }

    void drawFrame(int textureId, float[] texMatrix) {
        drawFrame(textureId, texMatrix,
                IDENTITY_MATRIX, mVertexArray, 0,
                mVertexCount, mCoordsPerVertex,
                mVertexStride, mTexCoordArray,
                mTexCoordStride);
    }

    private void drawFrame(int textureId, float[] texMatrix,
                          float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                          int vertexCount, int coordsPerVertex, int vertexStride,
                          FloatBuffer texBuffer, int texStride) {
        check("draw start");

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        check("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        check("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        check("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        check("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        check("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        check("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, texStride, texBuffer);
        check("glVertexAttribPointer");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
        check("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }
}

package com.otaliastudios.cameraview;


import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.FloatBuffer;

/**
 * This is a mix of 3 grafika classes, FullFrameRect, Texture2dProgram, Drawable2d.
 */
class EglViewport extends EglElement {

    private final static CameraLogger LOG = CameraLogger.create(EglViewport.class.getSimpleName());

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
    private final static int VERTEX_COUNT = FULL_RECTANGLE_COORDS.length / 2;
    private FloatBuffer mVertexCoordinatesArray = floatBuffer(FULL_RECTANGLE_COORDS);
    private FloatBuffer mTextureCoordinatesArray = floatBuffer(FULL_RECTANGLE_TEX_COORDS);

    // Stuff from Texture2dProgram
    private int mProgramHandle;
    private int mTextureTarget;
    // Program attributes
    private int muMVPMatrixLocation;
    private int muTexMatrixLocation;
    private int maPositionLocation;
    private int maTextureCoordLocation;

    // private int muKernelLoc; // Used for filtering
    // private int muTexOffsetLoc; // Used for filtering
    // private int muColorAdjustLoc; // Used for filtering

    EglViewport() {
        mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        mProgramHandle = createProgram(SIMPLE_VERTEX_SHADER, SIMPLE_FRAGMENT_SHADER);
        maPositionLocation = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        checkLocation(maPositionLocation, "aPosition");
        maTextureCoordLocation = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        checkLocation(maTextureCoordLocation, "aTextureCoord");
        muMVPMatrixLocation = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        checkLocation(muMVPMatrixLocation, "uMVPMatrix");
        muTexMatrixLocation = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        checkLocation(muTexMatrixLocation, "uTexMatrix");

        // Stuff from Drawable2d.FULL_RECTANGLE

    }

    void release(boolean doEglCleanup) {
        if (doEglCleanup) GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    void release() {
        release(true);
    }

    int createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        check("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
        check("glBindTexture " + texId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        check("glTexParameter");

        return texId;
    }

    void drawFrame(int textureId, float[] textureMatrix) {
        drawFrame(textureId, textureMatrix,
                mVertexCoordinatesArray,
                mTextureCoordinatesArray);
    }

    /**
     * The issue with the CIRCLE shader is that if the textureMatrix has a scale value,
     * it fails miserably, not taking the scale into account.
     * So what we can do here is
     *
     * - read textureMatrix scaleX and scaleY values. This is pretty much impossible to do from the matrix itself
     *   without making risky assumptions over the order of operations.
     *   https://www.opengl.org/discussion_boards/showthread.php/159215-Is-it-possible-to-extract-rotation-translation-scale-given-a-matrix
     *   So we prefer passing scaleX and scaleY here to the draw function.
     * - pass these values to the vertex shader
     * - pass them to the fragment shader
     * - in the fragment shader, take this scale value into account
     */
    private void drawFrame(int textureId, float[] textureMatrix,
                           FloatBuffer vertexBuffer,
                           FloatBuffer texBuffer) {
        check("draw start");

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        check("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLocation, 1, false, IDENTITY_MATRIX, 0);
        check("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLocation, 1, false, textureMatrix, 0);
        check("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        // Connect vertexBuffer to "aPosition".
        GLES20.glEnableVertexAttribArray(maPositionLocation);
        check("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(maPositionLocation, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);
        check("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        // Connect texBuffer to "aTextureCoord".
        GLES20.glEnableVertexAttribArray(maTextureCoordLocation);
        check("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(maTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 8, texBuffer);
        check("glVertexAttribPointer");


        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT);
        check("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLocation);
        GLES20.glDisableVertexAttribArray(maTextureCoordLocation);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }
}

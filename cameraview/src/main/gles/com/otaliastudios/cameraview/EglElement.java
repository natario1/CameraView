package com.otaliastudios.cameraview;


import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

class EglElement {

    private final static CameraLogger LOG = CameraLogger.create(EglElement.class.getSimpleName());

    // Identity matrix for general use.
    protected static final float[] IDENTITY_MATRIX = new float[16];
    static {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    // Check for GLES errors.
    protected static void check(String opName) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            LOG.e("Error during", opName, "glError 0x", Integer.toHexString(error));
            throw new RuntimeException(CameraLogger.lastMessage);
        }
    }

    // Check for valid location.
    protected static void checkLocation(int location, String label) {
        if (location < 0) {
            LOG.e("Unable to locate", label, "in program");
            throw new RuntimeException(CameraLogger.lastMessage);
        }
    }

    // Compiles the given shader, returns a handle.
    protected static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        check("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            LOG.e("Could not compile shader", shaderType, ":", GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    // Creates a program with given vertex shader and pixel shader.
    protected static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) return 0;
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) return 0;

        int program = GLES20.glCreateProgram();
        check("glCreateProgram");
        if (program == 0) {
            LOG.e("Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        check("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        check("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            LOG.e("Could not link program:", GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    // Allocates a direct float buffer, and populates it with the float array data.
    protected static FloatBuffer floatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }
}

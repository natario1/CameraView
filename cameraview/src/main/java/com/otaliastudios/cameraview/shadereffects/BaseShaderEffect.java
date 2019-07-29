package com.otaliastudios.cameraview.shadereffects;

import android.opengl.GLSurfaceView;

/**
 * An interface that every effect must implement so that there is a common
 * getFragmentShader method that every effect class is force to override
 */
public abstract class BaseShaderEffect {

    /**
     * Previewing GlSurfaceview.
     * This will be used by a few effects.
     */
    protected GLSurfaceView mGlSurfaceView;

    public void setGlSurfaceView(GLSurfaceView glSurfaceView) {
        this.mGlSurfaceView = glSurfaceView;
    }

    public String getPositionVariableName() {
        return mPositionVariableName;
    }

    /**
     * Local variable name which were used in the shader code.
     * These will be used by openGL program to render these vertex and fragment shader
     */
    private String mPositionVariableName = "aPosition";
    private String mTextureCoordinateVariableName = "aTextureCoord";
    private String mMVPMatrixVariableName = "uMVPMatrix";
    private String mTextureMatrixVariableName = "uTexMatrix";


    public void setPositionVariableName(String positionVariableName) {
        this.mPositionVariableName = positionVariableName;
    }

    public String getTexttureCoordinateVariableName() {
        return mTextureCoordinateVariableName;
    }

    public void setTexttureCoordinateVariableName(String texttureCoordinateVariableName) {
        this.mTextureCoordinateVariableName = texttureCoordinateVariableName;
    }

    public String getMVPMatrixVariableName() {
        return mMVPMatrixVariableName;
    }

    public void setMVPMatrixVariableName(String mvpMatrixVariableName) {
        this.mMVPMatrixVariableName = mvpMatrixVariableName;
    }

    public String getTextureMatrixVariableName() {
        return mTextureMatrixVariableName;
    }

    public void setTextureMatrixVariableName(String textureMatrixVariableName) {
        this.mTextureMatrixVariableName = textureMatrixVariableName;
    }

    /**
     * Get vertex Shader code
     *
     * @return complete shader code in C
     * <p>
     * This is the default implementation, each effect can implement its own.
     */
    public String getVertextShader() {
        return "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uTexMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "    gl_Position = uMVPMatrix * aPosition;\n" +
                "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                "}\n";
    }


    /**
     * Get fragment Shader code
     *
     * @return complete shader code in C
     */
    public abstract String getFragmentShader();

}

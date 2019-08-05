package com.otaliastudios.cameraview.filters;

/**
 * A Base abstract class that every effect must extend so that there is a common getShader method.
 * <p>
 * This class has a default Vertex Shader implementation which in most cases not required to touch.
 * In Effects like sepia, B/W any many, only pixel color changes which can be managed by only fragment shader.
 * If there is some other effect which requires vertex shader also change, you can override it.
 * <p>
 * If your provide your own vertex and fragment shader,
 * please set the {@link #mPositionVariableName}, {@link #mTextureCoordinateVariableName},
 * {@link #mMVPMatrixVariableName}, {@link #mTextureMatrixVariableName}
 * according to your shader code.
 * <p>
 * Please note that these shader applies on live preview as well as pictureSnapshot and videoSnapshot,
 * we only support GLES11Ext.GL_TEXTURE_EXTERNAL_OES
 * check EglViewport()
 * <p>
 * The default implementation of this class is NoEffect.
 */
public abstract class Filter {

    /**
     * Vertex shader code written in Shader Language (C) and stored as String.
     * This wil be used by GL to apply any effect.
     */

    protected String mVertexShader =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";


    /**
     * Fragment shader code written in Shader Language (C) and stored as String.
     * This wil be used by GL to apply any effect.
     */
    protected String mFragmentShader =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "varying vec2 vTextureCoord;\n"
                    + "uniform samplerExternalOES sTexture;\n"
                    + "void main() {\n"
                    + "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
                    + "}\n";

    /**
     * Width and height of previewing GlSurfaceview.
     * This will be used by a few effects.
     */
    protected int mPreviewingViewWidth = 0;
    protected int mPreviewingViewHeight = 0;

    public void setPreviewingViewWidthAndHeight(int width, int height){
        mPreviewingViewWidth = width;
        mPreviewingViewHeight = height;
    }

    /**
     * Local variable name which were used in the shader code.
     * These will be used by openGL program to render these vertex and fragment shader
     */
    private String mPositionVariableName = "aPosition";
    private String mTextureCoordinateVariableName = "aTextureCoord";
    private String mMVPMatrixVariableName = "uMVPMatrix";
    private String mTextureMatrixVariableName = "uTexMatrix";

    public String getPositionVariableName() {
        return mPositionVariableName;
    }

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
     */
    public String getVertexShader() {
        return mVertexShader;
    }


    /**
     * Get fragment Shader code
     *
     * @return complete shader code in C
     */
    public abstract String getFragmentShader();

}

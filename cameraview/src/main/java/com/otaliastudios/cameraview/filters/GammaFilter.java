package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.internal.GlUtils;

/**
 * Applies gamma correction to the frames.
 */
public class GammaFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 vTextureCoord;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float gamma;\n"
            + "void main() {\n"
            + "  vec4 textureColor = texture2D(sTexture, vTextureCoord);\n"
            + "  gl_FragColor = vec4(pow(textureColor.rgb, vec3(gamma)), textureColor.w);\n"
            + "}\n";

    private float gamma = 2.0f;
    private int gammaLocation = -1;

    @SuppressWarnings("WeakerAccess")
    public GammaFilter() { }

    /**
     * Sets the new gamma value in the 0.0 - 1.0 range.
     * The 0.5 value means no correction will be applied.
     *
     * @param gamma gamma value
     */
    @SuppressWarnings("WeakerAccess")
    public void setGamma(float gamma) {
        if (gamma < 0.0f) gamma = 0.0f;
        if (gamma > 1.0f) gamma = 1.0f;
        //since the shader excepts a range of 0.0 - 2.0
        //will multiply the 2.0 to every value
        this.gamma = gamma * 2.0f;
    }

    /**
     * Returns the current gamma.
     *
     * @see #setGamma(float)
     * @return gamma
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public float getGamma() {
        //since the shader excepts a range of 0.0 - 2.0
        //to keep it between 0.0f - 1.0f range, will divide it with 2.0
        return gamma / 2.0f;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        gammaLocation = GLES20.glGetUniformLocation(programHandle, "gamma");
        GlUtils.checkLocation(gammaLocation, "gamma");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gammaLocation = -1;
    }

    @Override
    protected void onPreDraw(float[] transformMatrix) {
        super.onPreDraw(transformMatrix);
        GLES20.glUniform1f(gammaLocation, gamma);
        GlUtils.checkError("glUniform1f");

    }

    @Override
    protected BaseFilter onCopy() {
        GammaFilter filter = new GammaFilter();
        filter.setGamma(getGamma());
        return filter;
    }
}
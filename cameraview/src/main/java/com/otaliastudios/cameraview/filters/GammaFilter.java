package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * Applies gamma correction to the frames.
 */
public class GammaFilter extends BaseFilter implements OneParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float gamma;\n"
            + "void main() {\n"
            + "  vec4 textureColor = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME
            + ");\n"
            + "  gl_FragColor = vec4(pow(textureColor.rgb, vec3(gamma)), textureColor.w);\n"
            + "}\n";

    private float gamma = 2.0f;
    private int gammaLocation = -1;

    public GammaFilter() { }

    /**
     * Sets the new gamma value in the 0.0 - 2.0 range.
     * The 1.0 value means no correction will be applied.
     *
     * @param gamma gamma value
     */
    @SuppressWarnings("WeakerAccess")
    public void setGamma(float gamma) {
        if (gamma < 0.0f) gamma = 0.0f;
        if (gamma > 2.0f) gamma = 2.0f;
        this.gamma = gamma;
    }

    /**
     * Returns the current gamma.
     *
     * @see #setGamma(float)
     * @return gamma
     */
    @SuppressWarnings("WeakerAccess")
    public float getGamma() {
        return gamma;
    }

    @Override
    public void setParameter1(float value) {
        setGamma(value * 2F);
    }

    @Override
    public float getParameter1() {
        return getGamma() / 2F;
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
        Egloo.checkGlProgramLocation(gammaLocation, "gamma");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gammaLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(gammaLocation, gamma);
        Egloo.checkGlError("glUniform1f");
    }
}
package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * Sharpens the input frames.
 */
public class SharpnessFilter extends BaseFilter implements OneParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float scale;\n"
            + "uniform float stepsizeX;\n"
            + "uniform float stepsizeY;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "void main() {\n"
            + "  vec3 nbr_color = vec3(0.0, 0.0, 0.0);\n"
            + "  vec2 coord;\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  coord.x = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".x - 0.5 * stepsizeX;\n"
            + "  coord.y = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".y - stepsizeY;\n"
            + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
            + "  coord.x = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".x - stepsizeX;\n"
            + "  coord.y = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".y + 0.5 * stepsizeY;\n"
            + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
            + "  coord.x = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".x + stepsizeX;\n"
            + "  coord.y = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".y - 0.5 * stepsizeY;\n"
            + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
            + "  coord.x = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".x + stepsizeX;\n"
            + "  coord.y = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".y + 0.5 * stepsizeY;\n"
            + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
            + "  gl_FragColor = vec4(color.rgb - 2.0 * scale * nbr_color, color.a);\n"
            + "}\n";

    private float scale = 0.5f;
    private int width = 1;
    private int height = 1;
    private int scaleLocation = -1;
    private int stepSizeXLocation = -1;
    private int stepSizeYLocation = -1;

    public SharpnessFilter() { }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.width = width;
        this.height = height;
    }

    /**
     * Sets the current sharpness value:
     * 0.0: no change.
     * 1.0: maximum sharpness.
     *
     * @param value new sharpness
     */
    @SuppressWarnings("WeakerAccess")
    public void setSharpness(float value) {
        if (value < 0.0f) value = 0.0f;
        if (value > 1.0f) value = 1.0f;
        this.scale = value;
    }

    /**
     * Returns the current sharpness.
     *
     * @see #setSharpness(float)
     * @return sharpness
     */
    @SuppressWarnings("WeakerAccess")
    public float getSharpness() {
        return scale;
    }

    @Override
    public void setParameter1(float value) {
        setSharpness(value);
    }

    @Override
    public float getParameter1() {
        return getSharpness();
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        scaleLocation = GLES20.glGetUniformLocation(programHandle, "scale");
        Egloo.checkGlProgramLocation(scaleLocation, "scale");
        stepSizeXLocation = GLES20.glGetUniformLocation(programHandle, "stepsizeX");
        Egloo.checkGlProgramLocation(stepSizeXLocation, "stepsizeX");
        stepSizeYLocation = GLES20.glGetUniformLocation(programHandle, "stepsizeY");
        Egloo.checkGlProgramLocation(stepSizeYLocation, "stepsizeY");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scaleLocation = -1;
        stepSizeXLocation = -1;
        stepSizeYLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(scaleLocation, scale);
        Egloo.checkGlError("glUniform1f");
        GLES20.glUniform1f(stepSizeXLocation, 1.0F / width);
        Egloo.checkGlError("glUniform1f");
        GLES20.glUniform1f(stepSizeYLocation, 1.0F / height);
        Egloo.checkGlError("glUniform1f");
    }
}

package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * Adjusts color temperature.
 */
public class TemperatureFilter extends BaseFilter implements OneParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float scale;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  vec3 new_color = color.rgb;\n"
            + "  new_color.r = color.r + color.r * ( 1.0 - color.r) * scale;\n"
            + "  new_color.b = color.b - color.b * ( 1.0 - color.b) * scale;\n"
            + "  if (scale > 0.0) { \n"
            + "    new_color.g = color.g + color.g * ( 1.0 - color.g) * scale * 0.25;\n"
            + "  }\n"
            + "  float max_value = max(new_color.r, max(new_color.g, new_color.b));\n"
            + "  if (max_value > 1.0) { \n"
            + "     new_color /= max_value;\n"
            + "  } \n"
            + "  gl_FragColor = vec4(new_color, color.a);\n"
            + "}\n";

    private float scale = 1F; // -1...1
    private int scaleLocation = -1;

    public TemperatureFilter() { }

    /**
     * Sets the new temperature value:
     * -1.0: cool colors
     * 0.0: no change
     * 1.0: warm colors
     *
     * @param value new value
     */
    @SuppressWarnings("WeakerAccess")
    public void setTemperature(float value) {
        if (value < -1F) value = -1F;
        if (value > 1F) value = 1F;
        this.scale = value;
    }

    /**
     * Returns the current temperature.
     *
     * @see #setTemperature(float)
     * @return temperature
     */
    @SuppressWarnings("WeakerAccess")
    public float getTemperature() {
        return scale;
    }

    @Override
    public void setParameter1(float value) {
        setTemperature((2F * value - 1F));
    }

    @Override
    public float getParameter1() {
        return (getTemperature() + 1F) / 2F;
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scaleLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(scaleLocation, scale);
        Egloo.checkGlError("glUniform1f");
    }
}

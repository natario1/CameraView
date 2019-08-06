package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.internal.GlUtils;

/**
 * Adjusts color temperature.
 */
public class TemperatureFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float scale;\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
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

    private float scale = 0f;
    private int scaleLocation = -1;

    @SuppressWarnings("WeakerAccess")
    public TemperatureFilter() { }

    /**
     * Sets the new temperature value:
     * 0.0: cool colors
     * 0.5: no change
     * 1.0: warm colors
     *
     * @param value new value
     */
    @SuppressWarnings("WeakerAccess")
    public void setTemperature(float value) {
        if (value < 0.0f) value = 0.0f;
        if (value > 1.0f) value = 1.0f;
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

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        scaleLocation = GLES20.glGetUniformLocation(programHandle, "scale");
        GlUtils.checkLocation(scaleLocation, "scale");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scaleLocation = -1;
    }

    @Override
    protected void onPreDraw(float[] transformMatrix) {
        super.onPreDraw(transformMatrix);
        GLES20.glUniform1f(scaleLocation, (2.0f * scale - 1.0f));
        GlUtils.checkError("glUniform1f");
    }

    @Override
    protected BaseFilter onCopy() {
        TemperatureFilter filter = new TemperatureFilter();
        filter.setTemperature(getTemperature());
        return filter;
    }
}

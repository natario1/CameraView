package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * Applies a hue effect on the input frames.
 */
public class HueFilter extends BaseFilter implements OneParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float hue;\n"
            + "void main() {\n"
            + "  vec4 kRGBToYPrime = vec4 (0.299, 0.587, 0.114, 0.0);\n"
            + "  vec4 kRGBToI = vec4 (0.595716, -0.274453, -0.321263, 0.0);\n"
            + "  vec4 kRGBToQ = vec4 (0.211456, -0.522591, 0.31135, 0.0);\n"
            + "  vec4 kYIQToR = vec4 (1.0, 0.9563, 0.6210, 0.0);\n"
            + "  vec4 kYIQToG = vec4 (1.0, -0.2721, -0.6474, 0.0);\n"
            + "  vec4 kYIQToB = vec4 (1.0, -1.1070, 1.7046, 0.0);\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  float YPrime = dot(color, kRGBToYPrime);\n"
            + "  float I = dot(color, kRGBToI);\n"
            + "  float Q = dot(color, kRGBToQ);\n"
            + "  float chroma = sqrt (I * I + Q * Q);\n"
            + "  Q = chroma * sin (hue);\n"
            + "  I = chroma * cos (hue);\n"
            + "  vec4 yIQ = vec4 (YPrime, I, Q, 0.0);\n"
            + "  color.r = dot (yIQ, kYIQToR);\n"
            + "  color.g = dot (yIQ, kYIQToG);\n"
            + "  color.b = dot (yIQ, kYIQToB);\n"
            + "  gl_FragColor = color;\n"
            + "}\n";

    private float hue = 0.0f;
    private int hueLocation = -1;

    public HueFilter() { }

    /**
     * Sets the hue value in degrees. See the values chart:
     * https://cloud.githubusercontent.com/assets/2201511/21810115/b99ac22a-d74a-11e6-9f6c-ef74d15c88c7.jpg
     *
     * @param hue hue degrees
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setHue(float hue) {
        this.hue = hue % 360;
    }

    /**
     * Returns the current hue value.
     *
     * @see #setHue(float)
     * @return hue
     */
    @SuppressWarnings("WeakerAccess")
    public float getHue() {
        return hue;
    }

    @Override
    public void setParameter1(float value) {
        setHue(value * 360F);
    }

    @Override
    public float getParameter1() {
        return getHue() / 360F;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        hueLocation = GLES20.glGetUniformLocation(programHandle, "hue");
        Egloo.checkGlProgramLocation(hueLocation, "hue");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hueLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        // map it on 360 degree circle
        float shaderHue = ((hue - 45) / 45f + 0.5f) * -1;
        GLES20.glUniform1f(hueLocation, shaderHue);
        Egloo.checkGlError("glUniform1f");
    }
}
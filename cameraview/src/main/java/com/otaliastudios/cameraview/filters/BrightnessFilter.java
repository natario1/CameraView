package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * Adjusts the brightness of the frames.
 */
public class BrightnessFilter extends BaseFilter implements OneParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float brightness;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  gl_FragColor = brightness * color;\n"
            + "}\n";

    private float brightness = 2.0f; // 1.0F...2.0F
    private int brightnessLocation = -1;


    public BrightnessFilter() { }

    /**
     * Sets the brightness adjustment.
     * 1.0: normal brightness.
     * 2.0: high brightness.
     *
     * @param brightness brightness.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public void setBrightness(float brightness) {
        if (brightness < 1.0f) brightness = 1.0f;
        if (brightness > 2.0f) brightness = 2.0f;
        this.brightness = brightness;
    }

    /**
     * Returns the current brightness.
     *
     * @see #setBrightness(float)
     * @return brightness
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public float getBrightness() {
        return brightness;
    }

    @Override
    public void setParameter1(float value) {
        // parameter is 0...1, brightness is 1...2.
        setBrightness(value + 1);
    }

    @Override
    public float getParameter1() {
        // parameter is 0...1, brightness is 1...2.
        return getBrightness() - 1F;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        brightnessLocation = GLES20.glGetUniformLocation(programHandle, "brightness");
        Egloo.checkGlProgramLocation(brightnessLocation, "brightness");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        brightnessLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(brightnessLocation, brightness);
        Egloo.checkGlError("glUniform1f");
    }
}

package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * Adjusts the contrast.
 */
public class ContrastFilter extends BaseFilter implements OneParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float contrast;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  color -= 0.5;\n"
            + "  color *= contrast;\n"
            + "  color += 0.5;\n"
            + "  gl_FragColor = color;\n"
            + "}\n";

    private float contrast = 2F;
    private int contrastLocation = -1;

    public ContrastFilter() { }

    /**
     * Sets the current contrast adjustment.
     * 1.0: no adjustment
     * 2.0: increased contrast
     *
     * @param contrast contrast
     */
    @SuppressWarnings("WeakerAccess")
    public void setContrast(float contrast) {
        if (contrast < 1.0f) contrast = 1.0f;
        if (contrast > 2.0f) contrast = 2.0f;
        this.contrast = contrast;
    }

    /**
     * Returns the current contrast.
     *
     * @see #setContrast(float)
     * @return contrast
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public float getContrast() {
        return contrast;
    }

    @Override
    public void setParameter1(float value) {
        // parameter is 0...1, contrast is 1...2.
        setContrast(value + 1);
    }

    @Override
    public float getParameter1() {
        // parameter is 0...1, contrast is 1...2.
        return getContrast() - 1F;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        contrastLocation = GLES20.glGetUniformLocation(programHandle, "contrast");
        Egloo.checkGlProgramLocation(contrastLocation, "contrast");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        contrastLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(contrastLocation, contrast);
        Egloo.checkGlError("glUniform1f");
    }
}

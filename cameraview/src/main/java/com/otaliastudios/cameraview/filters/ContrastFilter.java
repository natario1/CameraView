package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.internal.GlUtils;

/**
 * Adjusts the contrast.
 */
public class ContrastFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float contrast;\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
            + "  color -= 0.5;\n"
            + "  color *= contrast;\n"
            + "  color += 0.5;\n"
            + "  gl_FragColor = color;\n"
            + "}\n";

    private float contrast = 2.0f;
    private int contrastLocation = -1;

    @SuppressWarnings("WeakerAccess")
    public ContrastFilter() { }

    /**
     * Sets the current contrast adjustment.
     * 0.0: no adjustment
     * 1.0: maximum adjustment
     *
     * @param contrast contrast
     */
    @SuppressWarnings("WeakerAccess")
    public void setContrast(float contrast) {
        if (contrast < 0.0f) contrast = 0.0f;
        if (contrast > 1.0f) contrast = 1.0f;
        //since the shader excepts a range of 1.0 - 2.0
        //will add the 1.0 to every value
        this.contrast = contrast + 1.0f;
    }

    /**
     * Returns the current contrast.
     *
     * @see #setContrast(float)
     * @return contrast
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public float getContrast() {
        //since the shader excepts a range of 1.0 - 2.0
        //to keep it between 0.0f - 1.0f range, will subtract the 1.0 to every value
        return contrast - 1.0f;
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
        GlUtils.checkLocation(contrastLocation, "contrast");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        contrastLocation = -1;
    }

    @Override
    protected void onPreDraw(float[] transformMatrix) {
        super.onPreDraw(transformMatrix);
        GLES20.glUniform1f(contrastLocation, contrast);
        GlUtils.checkError("glUniform1f");
    }

    @Override
    protected BaseFilter onCopy() {
        ContrastFilter filter = new ContrastFilter();
        filter.setContrast(getContrast());
        return filter;
    }
}

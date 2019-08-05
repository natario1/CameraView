package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Adjusts the contrast.
 */
public class ContrastFilter extends BaseFilter {
    private float contrast = 2.0f;

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


    @Override
    protected BaseFilter onCopy() {
        ContrastFilter filter = new ContrastFilter();
        filter.setContrast(getContrast());
        return filter;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "float contrast;\n"
                + "varying vec2 vTextureCoord;\n"
                + "void main() {\n"
                + "  contrast =" + contrast + ";\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  color -= 0.5;\n"
                + "  color *= contrast;\n"
                + "  color += 0.5;\n"
                + "  gl_FragColor = color;\n" + "}\n";

    }

}

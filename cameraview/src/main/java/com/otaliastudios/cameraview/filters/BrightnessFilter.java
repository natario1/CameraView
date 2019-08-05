package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Adjusts the brightness of the frames.
 */
public class BrightnessFilter extends BaseFilter {

    private float brightness = 2.0f;

    @SuppressWarnings("WeakerAccess")
    public BrightnessFilter() { }

    /**
     * Sets the brightness adjustment.
     * 0.0: normal brightness.
     * 1.0: high brightness.
     *
     * @param brightness brightness.
     */
    @SuppressWarnings("WeakerAccess")
    public void setBrightness(float brightness) {
        if (brightness < 0.0f) brightness = 0.0f;
        if (brightness > 1.0f) brightness = 1.0f;

        //since the shader excepts a range of 1.0 - 2.0
        // will add the 1.0 to every value
        this.brightness = 1.0f + brightness;
    }

    /**
     * Returns the current brightness.
     *
     * @see #setBrightness(float)
     * @return brightness
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public float getBrightness() {
        //since the shader excepts a range of 1.0 - 2.0
        //to keep it between 0.0f - 1.0f range, will subtract the 1.0 to every value
        return brightness - 1.0f;
    }

    @Override
    protected BaseFilter onCopy() {
        BrightnessFilter filter = new BrightnessFilter();
        filter.setBrightness(getBrightness());
        return filter;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "float brightness ;\n"
                + "varying vec2 vTextureCoord;\n"
                + "void main() {\n"
                + "  brightness =" + brightness + ";\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  gl_FragColor = brightness * color;\n"
                + "}\n";
    }

}

package com.otaliastudios.cameraview.filters;

import com.otaliastudios.cameraview.filters.Filter;

/**
 * Adjusts the brightness of the preview.
 */
public class BrightnessEffect extends Filter {
    private float brightnessValue = 2.0f;

    /**
     * Initialize Effect
     */
    public BrightnessEffect() {
    }

    /**
     * setBrightnessValue
     *
     * @param brightnessvalue Range should be between 0.0- 1.0 with 0.0 being normal.
     */
    public void setBrightnessValue(float brightnessvalue) {
        if (brightnessvalue < 0.0f)
            brightnessvalue = 0.0f;
        else if (brightnessvalue > 1.0f)
            brightnessvalue = 1.0f;

        //since the shader excepts a range of 1.0 - 2.0
        // will add the 1.0 to every value
        this.brightnessValue = 1.0f + brightnessvalue;
    }

    public float getBrightnessValue() {
        //since the shader excepts a range of 1.0 - 2.0
        //to keep it between 0.0f - 1.0f range, will subtract the 1.0 to every value
        return brightnessValue - 1.0f;
    }

    @Override
    public String getFragmentShader() {

        String shader = "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "float brightness ;\n" + "varying vec2 vTextureCoord;\n"
                + "void main() {\n" + "  brightness =" + brightnessValue
                + ";\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  gl_FragColor = brightness * color;\n" + "}\n";

        return shader;

    }

}

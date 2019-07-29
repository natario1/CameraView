package com.otaliastudios.cameraview.shadereffects.effects;

import com.otaliastudios.cameraview.shadereffects.BaseShaderEffect;

/**
 * Adjusts the brightness of the preview.
 */
public class BrightnessEffect extends BaseShaderEffect {
    private float brightnessValue = 2.0f;

    /**
     * Initialize Effect
     */
    public BrightnessEffect() {
    }

    /**
     * setBrightnessValue
     *
     * @param brightnessvalue Range should be between 1.0- 2.0 with 1.0 being normal.
     */
    public void setBrightnessValue(float brightnessvalue) {
        if (brightnessvalue < 1.0f)
            brightnessvalue = 1.0f;
        else if (brightnessvalue > 2.0f)
            brightnessvalue = 2.0f;

        this.brightnessValue = brightnessvalue;
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

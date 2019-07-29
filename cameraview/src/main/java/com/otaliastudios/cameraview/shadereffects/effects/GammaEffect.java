package com.otaliastudios.cameraview.shadereffects.effects;

import com.otaliastudios.cameraview.shadereffects.BaseShaderEffect;

/**
 * Apply Gamma Effect on preview being played
 */
public class GammaEffect extends BaseShaderEffect {
    private float gammaValue = 2.0f;

    /**
     * Initialize Effect
     */
    public GammaEffect() {
    }

    /**
     * setGammaValue
     *
     * @param gammaValue Range should be between 0.0 - 2.0 with 1.0 being normal.
     */
    public void setGammaValue(float gammaValue){
        if (gammaValue < 0.0f)
            gammaValue = 0.0f;
        else if (gammaValue > 2.0f)
            gammaValue = 2.0f;
        this.gammaValue = gammaValue;
    }

    @Override
    public String getFragmentShader() {

        String shader = "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"

                + "varying vec2 vTextureCoord;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "float gamma=" + gammaValue + ";\n"

                + "void main() {\n"

                + "vec4 textureColor = texture2D(sTexture, vTextureCoord);\n"
                + "gl_FragColor = vec4(pow(textureColor.rgb, vec3(gamma)), textureColor.w);\n"

                + "}\n";

        return shader;
    }
}
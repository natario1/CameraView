package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

/**
 * Apply Gamma Effect on preview being played
 */
public class GammaEffect extends Filter {
    private float gammaValue = 2.0f;

    /**
     * Initialize Effect
     */
    public GammaEffect() {
    }

    /**
     * setGammaValue
     *
     * @param gammaValue Range should be between 0.0 - 1.0 with 0.5 being normal.
     */
    public void setGammaValue(float gammaValue) {
        if (gammaValue < 0.0f)
            gammaValue = 0.0f;
        else if (gammaValue > 1.0f)
            gammaValue = 1.0f;

        //since the shader excepts a range of 0.0 - 2.0
        //will multiply the 2.0 to every value
        this.gammaValue = gammaValue * 2.0f;
    }

    public float getGammaValue() {
        //since the shader excepts a range of 0.0 - 2.0
        //to keep it between 0.0f - 1.0f range, will divide it with 2.0
        return gammaValue / 2.0f;
    }

    @NonNull
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
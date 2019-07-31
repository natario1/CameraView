package com.otaliastudios.cameraview.shadereffects.effects;

import com.otaliastudios.cameraview.shadereffects.BaseShaderEffect;


/**
 * This class is to implement any custom effect.
 */
public class CustomEffect extends BaseShaderEffect {

    /**
     * Parameterized constructor with vertex and fragment shader as parameter
     *
     * @param vertexShader
     * @param fragmentShader
     */
    public CustomEffect(String vertexShader, String fragmentShader) {
        this.mVertexShader = vertexShader;
        this.mFragmentShader = fragmentShader;
    }

    @Override
    public String getFragmentShader() {
        return mFragmentShader;
    }
}

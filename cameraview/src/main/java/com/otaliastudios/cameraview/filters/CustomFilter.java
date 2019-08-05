package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;


/**
 * This class is to implement any custom effect.
 */
public class CustomFilter extends BaseFilter {

    /**
     * Parameterized constructor with vertex and fragment shader as parameter
     *
     * @param vertexShader
     * @param fragmentShader
     */
    public CustomFilter(String vertexShader, String fragmentShader) {
        this.mVertexShader = vertexShader;
        this.mFragmentShader = fragmentShader;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return mFragmentShader;
    }
}

package com.otaliastudios.cameraview.filters;

import com.otaliastudios.cameraview.filters.Filter;


/**
 * This class is to implement any custom effect.
 */
public class CustomEffect extends Filter {

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

package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;


public interface Filter {

    void setOutputSize(int width, int height);

    @NonNull
    String getVertexShader();

    @NonNull
    String getFragmentShader();

}

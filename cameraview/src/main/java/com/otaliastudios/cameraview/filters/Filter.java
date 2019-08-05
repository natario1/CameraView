package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;


public interface Filter {

    void onCreate(int programHandle);

    void onDestroy();

    void draw(float[] transformMatrix);

    void setOutputSize(int width, int height);

    @NonNull
    String getVertexShader();

    @NonNull
    String getFragmentShader();

}

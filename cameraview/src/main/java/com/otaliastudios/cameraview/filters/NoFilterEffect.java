package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

public class NoFilterEffect extends Filter {

    @NonNull
    @Override
    public String getFragmentShader() {
        return mFragmentShader;
    }
}

package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

public class NoFilter extends BaseFilter {

    @NonNull
    @Override
    public String getFragmentShader() {
        return mFragmentShader;
    }
}

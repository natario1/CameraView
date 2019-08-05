package com.otaliastudios.cameraview.filters;

import com.otaliastudios.cameraview.filters.Filter;

public class NoFilterEffect extends Filter {

    @Override
    public String getFragmentShader() {
        return mFragmentShader;
    }
}

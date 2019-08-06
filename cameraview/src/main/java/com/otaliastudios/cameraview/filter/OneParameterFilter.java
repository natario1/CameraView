package com.otaliastudios.cameraview.filter;

public interface OneParameterFilter extends Filter {

    void setParameter1(float value);

    float getParameter1();
}

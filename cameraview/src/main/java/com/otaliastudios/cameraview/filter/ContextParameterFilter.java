package com.otaliastudios.cameraview.filter;

import android.content.Context;

public interface ContextParameterFilter {
    /**
     * Sets the parameter.
     * The value should always be between 0 and 1.
     *
     * @param value parameter
     */
    void setContext(Context value);

}

package com.otaliastudios.cameraview.filter;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * A {@link Filter} that draws frames without any modification.
 */
public final class NoFilter extends BaseFilter {

    @NonNull
    @Override
    public String getFragmentShader() {
        return createDefaultFragmentShader();
    }
}

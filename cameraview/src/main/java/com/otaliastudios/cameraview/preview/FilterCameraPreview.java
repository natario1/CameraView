package com.otaliastudios.cameraview.preview;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.Filter;


/**
 * A preview that support GL filters defined through the {@link Filter} interface.
 *
 * The preview has the responsibility of calling {@link Filter#setSize(int, int)}
 * whenever the preview size changes and as soon as the filter is applied.
 */
public abstract class FilterCameraPreview<T extends View, Output> extends CameraPreview<T, Output> {

    @SuppressWarnings("WeakerAccess")
    public FilterCameraPreview(@NonNull Context context, @NonNull ViewGroup parent) {
        super(context, parent);
    }

    /**
     * Sets a new filter.
     * @param filter new filter
     */
    public abstract void setFilter(@NonNull Filter filter);

    /**
     * Returns the currently used filter.
     * @return currently used filter
     */
    @SuppressWarnings("unused")
    @NonNull
    public abstract Filter getCurrentFilter();
}

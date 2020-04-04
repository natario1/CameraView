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
public interface FilterCameraPreview {

    /**
     * Sets a new filter.
     * @param filter new filter
     */
    void setFilter(@NonNull Filter filter);

    /**
     * Returns the currently used filter.
     * @return currently used filter
     */
    @SuppressWarnings("unused")
    @NonNull
    Filter getCurrentFilter();
}

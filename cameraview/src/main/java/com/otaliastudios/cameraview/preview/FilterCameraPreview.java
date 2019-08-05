package com.otaliastudios.cameraview.preview;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filters.Filter;


/**
 * A preview that support GL filters defined through the {@link Filter} interface.
 */
public abstract class FilterCameraPreview<T extends View, Output> extends CameraPreview<T, Output> {

    public FilterCameraPreview(@NonNull Context context, @NonNull ViewGroup parent) {
        super(context, parent);
    }

    public abstract void setFilter(@NonNull Filter filter);

    @NonNull
    public abstract Filter getCurrentFilter();
}

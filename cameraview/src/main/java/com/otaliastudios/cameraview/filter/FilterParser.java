package com.otaliastudios.cameraview.filter;

import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.cameraview.R;

/**
 * Parses filters from XML attributes.
 */
public class FilterParser {

    private Filter filter;

    public FilterParser(@NonNull String filterName) {
        try {
            //noinspection ConstantConditions
            Class<?> filterClass = Class.forName(filterName);
            filter = (Filter) filterClass.newInstance();
        } catch (Exception ignore) {
            filter = new NoFilter();
        }
    }
    public FilterParser(@NonNull TypedArray array) {
        this(array.getString(R.styleable.CameraView_cameraFilter));
    }

    @NonNull
    public Filter getFilter() {
        return filter;
    }
}

package com.otaliastudios.cameraview.filter;

import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.cameraview.R;

/**
 * Parses filters from XML attributes.
 */
public class FilterParser {

    private Filter filter = null;

    public FilterParser(@NonNull TypedArray array) {
        String filterName = array.getString(R.styleable.CameraView_cameraFilter);
        try {
            //noinspection ConstantConditions
            Class<?> filterClass = Class.forName(filterName);
            filter = (Filter) filterClass.newInstance();
        } catch (Exception ignore) {
            filter = new NoFilter();
        }
    }

    @NonNull
    public Filter getFilter() {
        return filter;
    }
}

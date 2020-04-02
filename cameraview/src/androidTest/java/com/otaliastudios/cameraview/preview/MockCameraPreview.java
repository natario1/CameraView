package com.otaliastudios.cameraview.preview;


import android.content.Context;
import androidx.annotation.NonNull;

import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.preview.CameraPreview;

public class MockCameraPreview extends CameraPreview<View, Void> implements FilterCameraPreview {

    public MockCameraPreview(Context context, ViewGroup parent) {
        super(context, parent);
    }

    private View rootView;
    private Filter filter;

    @Override
    public boolean supportsCropping() {
        return true;
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        rootView = new View(context);
        return rootView;
    }

    @NonNull
    @Override
    public Class<Void> getOutputClass() {
        return null;
    }

    @NonNull
    @Override
    public Void getOutput() {
        return null;
    }


    @NonNull
    @Override
    public View getRootView() {
        return rootView;
    }

    @Override
    public void setFilter(@NonNull Filter filter) {
        this.filter = filter;
    }

    @NonNull
    @Override
    public Filter getCurrentFilter() {
        return filter;
    }
}

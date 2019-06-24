package com.otaliastudios.cameraview.preview;


import android.content.Context;
import androidx.annotation.NonNull;

import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.preview.CameraPreview;

public class MockCameraPreview extends CameraPreview<View, Void> {

    public MockCameraPreview(Context context, ViewGroup parent) {
        super(context, parent, null);
    }

    @Override
    public boolean supportsCropping() {
        return true;
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        return new View(context);
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
        return null;
    }
}

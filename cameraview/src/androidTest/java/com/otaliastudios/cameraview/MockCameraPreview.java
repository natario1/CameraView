package com.otaliastudios.cameraview;


import android.content.Context;
import androidx.annotation.NonNull;

import android.view.View;
import android.view.ViewGroup;

public class MockCameraPreview extends CameraPreview<View, Void> {

    MockCameraPreview(Context context, ViewGroup parent) {
        super(context, parent, null);
    }

    @Override
    boolean supportsCropping() {
        return true;
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        return new View(context);
    }

    @NonNull
    @Override
    Class<Void> getOutputClass() {
        return null;
    }

    @NonNull
    @Override
    Void getOutput() {
        return null;
    }

    @NonNull
    @Override
    View getRootView() {
        return null;
    }
}

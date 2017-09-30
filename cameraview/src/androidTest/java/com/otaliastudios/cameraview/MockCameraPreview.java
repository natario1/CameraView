package com.otaliastudios.cameraview;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

public class MockCameraPreview extends CameraPreview<View, Void> {

    MockCameraPreview(Context context, ViewGroup parent) {
        super(context, parent, null);
    }

    private boolean mCropping = false;

    public void setIsCropping(boolean crop) {
        mCropping = crop;
    }

    @Override
    boolean isCropping() {
        return mCropping;
    }

    @NonNull
    @Override
    protected View onCreateView(Context context, ViewGroup parent) {
        return new View(context);
    }

    @Override
    Surface getSurface() {
        return null;
    }

    @Override
    Class<Void> getOutputClass() {
        return null;
    }

    @Override
    Void getOutput() {
        return null;
    }

}

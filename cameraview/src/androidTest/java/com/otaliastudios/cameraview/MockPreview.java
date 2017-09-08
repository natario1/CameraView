package com.otaliastudios.cameraview;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

public class MockPreview extends Preview<View, Void> {

    MockPreview(Context context, ViewGroup parent) {
        super(context, parent, null);
    }

    public void setIsCropping(boolean crop) {
        getView().setScaleX(crop ? 2 : 1);
        getView().setScaleY(crop ? 2 : 1);
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

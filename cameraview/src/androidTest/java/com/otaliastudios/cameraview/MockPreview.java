package com.otaliastudios.cameraview;


import android.content.Context;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

public class MockPreview extends Preview {

    public MockPreview(Context context, ViewGroup parent) {
        super(context, parent);
    }

    @Override
    Surface getSurface() {
        return null;
    }

    @Override
    View getView() {
        return null;
    }

    @Override
    Class getOutputClass() {
        return null;
    }

    @Override
    boolean isReady() {
        return true;
    }
}

package com.otaliastudios.cameraview.preview;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import android.view.ViewGroup;

import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TextureCameraPreviewTest extends CameraPreviewTest<TextureCameraPreview> {

    @Override
    protected TextureCameraPreview createPreview(Context context, ViewGroup parent) {
        return new TextureCameraPreview(context, parent);
    }

    @Override
    protected void ensureAvailable() {
        if (isHardwareAccelerated()) {
            super.ensureAvailable();
        } else {
            preview.dispatchOnSurfaceAvailable(
                    surfaceSize.getWidth(),
                    surfaceSize.getHeight());
        }
    }

    @Override
    protected void ensureDestroyed() {
        super.ensureDestroyed();
        if (!isHardwareAccelerated()) {
            // Ensure it is called.
            preview.dispatchOnSurfaceDestroyed();
        }
    }

    private boolean isHardwareAccelerated() {
        return preview.getView().isHardwareAccelerated();
    }

    @Override
    protected float getCropScaleX() {
        return preview.getView().getScaleX();
    }

    @Override
    protected float getCropScaleY() {
        return preview.getView().getScaleY();
    }
}

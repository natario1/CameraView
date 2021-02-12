package com.otaliastudios.cameraview.preview;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import android.view.ViewGroup;

import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SurfaceCameraPreviewTest extends CameraPreviewTest<SurfaceCameraPreview> {

    @Override
    protected SurfaceCameraPreview createPreview(Context context, ViewGroup parent) {
        return new SurfaceCameraPreview(context, parent);
    }

    @Override
    protected float getCropScaleX() {
        return 1F;
    }

    @Override
    protected float getCropScaleY() {
        return 1F;
    }
}

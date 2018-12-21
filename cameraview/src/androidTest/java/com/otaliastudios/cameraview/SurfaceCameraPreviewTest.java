package com.otaliastudios.cameraview;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import android.view.ViewGroup;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SurfaceCameraPreviewTest extends CameraPreviewTest {

    @Override
    protected CameraPreview createPreview(Context context, ViewGroup parent, CameraPreview.SurfaceCallback callback) {
        return new SurfaceCameraPreview(context, parent, callback);
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

package com.otaliastudios.cameraview.preview;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.preview.CameraPreview;
import com.otaliastudios.cameraview.preview.CameraPreviewTest;
import com.otaliastudios.cameraview.preview.SurfaceCameraPreview;

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

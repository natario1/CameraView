package com.otaliastudios.cameraview;


import android.content.Context;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ViewGroup;

import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GlCameraPreviewTest extends CameraPreviewTest {

    @Override
    protected CameraPreview createPreview(Context context, ViewGroup parent, CameraPreview.SurfaceCallback callback) {
        return new GlCameraPreview(context, parent, callback);
    }

    @Override
    protected float getCropScaleY() {
        return 1F / ((GlCameraPreview) preview).mScaleY;
    }

    @Override
    protected float getCropScaleX() {
        return 1F / ((GlCameraPreview) preview).mScaleX;
    }
}

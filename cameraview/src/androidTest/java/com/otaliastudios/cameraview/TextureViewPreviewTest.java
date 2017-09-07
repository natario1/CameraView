package com.otaliastudios.cameraview;


import android.content.Context;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ViewGroup;

import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TextureViewPreviewTest extends PreviewTest {

    @Override
    protected Preview createPreview(Context context, ViewGroup parent, Preview.SurfaceCallback callback) {
        return new TextureViewPreview(context, parent, callback);
    }

    @Override
    protected void ensureAvailable() {
        if (isHardwareAccelerated()) {
            super.ensureAvailable();
        } else {
            preview.onSurfaceAvailable(
                    surfaceSize.getWidth(),
                    surfaceSize.getHeight());
        }
    }

    @Override
    protected void ensureDestroyed() {
        super.ensureDestroyed();
        if (!isHardwareAccelerated()) {
            // Ensure it is called.
            preview.onSurfaceDestroyed();
        }
    }

    private boolean isHardwareAccelerated() {
        return preview.getView().isHardwareAccelerated();
    }
}

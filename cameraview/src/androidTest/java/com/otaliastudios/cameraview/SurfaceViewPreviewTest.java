package com.otaliastudios.cameraview;


import android.content.Context;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ViewGroup;

import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SurfaceViewPreviewTest extends PreviewTest {

    @Override
    protected Preview createPreview(Context context, ViewGroup parent) {
        return new SurfaceViewPreview(context, parent);
    }
}

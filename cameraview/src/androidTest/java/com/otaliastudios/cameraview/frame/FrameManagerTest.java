package com.otaliastudios.cameraview.frame;


import android.graphics.ImageFormat;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.size.Size;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FrameManagerTest extends BaseTest {

    @Test
    public void testFrameRecycling() {
        // A 1-pool manager will always recycle the same frame.
        FrameManager<String> manager = new FrameManager<String>(1, String.class) {
            @Override
            protected void onFrameDataRecycled(@NonNull String data) { }
        };
        manager.setUp(ImageFormat.NV21, new Size(50, 50));

        Frame first = manager.getFrame("foo", 0, 0);
        first.release();
        Frame second = manager.getFrame("bar", 0, 0);
        second.release();
        assertEquals(first, second);
    }
}

package com.otaliastudios.cameraview.frame;


import android.graphics.ImageFormat;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.engine.offset.Angles;
import com.otaliastudios.cameraview.size.Size;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FrameManagerTest extends BaseTest {

    private final Angles angles = new Angles();

    @Test
    public void testFrameRecycling() {
        // A 1-pool manager will always recycle the same frame.
        FrameManager<String> manager = new FrameManager<String>(1, String.class) {
            @Override
            protected void onFrameDataReleased(@NonNull String data, boolean recycled) { }

            @NonNull
            @Override
            protected String onCloneFrameData(@NonNull String data) {
                return data;
            }
        };
        manager.setUp(ImageFormat.NV21, new Size(50, 50), angles);

        Frame first = manager.getFrame("foo", 0);
        assertNotNull(first);
        first.release();
        Frame second = manager.getFrame("bar", 0);
        assertNotNull(second);
        second.release();
        assertEquals(first, second);
    }

    @Test
    public void testGetFrame() {
        FrameManager<String> manager = new FrameManager<String>(1, String.class) {
            @Override
            protected void onFrameDataReleased(@NonNull String data, boolean recycled) { }

            @NonNull
            @Override
            protected String onCloneFrameData(@NonNull String data) {
                return data;
            }
        };
        manager.setUp(ImageFormat.NV21, new Size(50, 50), angles);

        Frame first = manager.getFrame("foo", 0);
        assertNotNull(first);
        Frame second = manager.getFrame("bar", 0);
        assertNull(second);
    }
}

package com.otaliastudios.cameraview.frame;


import android.graphics.ImageFormat;

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

    private FrameManager.BufferCallback callback;

    @Before
    public void setUp() {
        callback = mock(FrameManager.BufferCallback.class);
    }

    @After
    public void tearDown() {
        callback = null;
    }

    @Test
    public void testAllocate() {
        FrameManager manager = new FrameManager(1, callback);
        manager.setUp(ImageFormat.NV21, new Size(50, 50));
        verify(callback, times(1)).onBufferAvailable(any(byte[].class));
        reset(callback);

        manager = new FrameManager(5, callback);
        manager.setUp(ImageFormat.NV21, new Size(50, 50));
        verify(callback, times(5)).onBufferAvailable(any(byte[].class));
    }

    @Test
    public void testFrameRecycling() {
        // A 1-pool manager will always recycle the same frame.
        FrameManager manager = new FrameManager(1, callback);
        manager.setUp(ImageFormat.NV21, new Size(50, 50));

        Frame first = manager.getFrame(null, 0, 0);
        first.release();

        Frame second = manager.getFrame(null, 0, 0);
        second.release();

        assertEquals(first, second);
    }

    @Test
    public void testOnFrameReleased_alreadyFull() {
        FrameManager manager = new FrameManager(1, callback);
        int length = manager.setUp(ImageFormat.NV21, new Size(50, 50));

        Frame frame1 = manager.getFrame(new byte[length], 0, 0);
        // Since frame1 is already taken and poolSize = 1, a new Frame is created.
        Frame frame2 = manager.getFrame(new byte[length], 0, 0);
        // Release the first frame so it goes back into the pool.
        manager.onFrameReleased(frame1, frame1.getData());
        reset(callback);
        // Release the second. The pool is already full, so onBufferAvailable should not be called
        // since this Frame instance will NOT be reused.
        manager.onFrameReleased(frame2, frame2.getData());
        verify(callback, never()).onBufferAvailable(frame2.getData());
    }

    @Test
    public void testOnFrameReleased_sameLength() {
        FrameManager manager = new FrameManager(1, callback);
        int length = manager.setUp(ImageFormat.NV21, new Size(50, 50));

        // A camera preview frame comes. Request a frame.
        byte[] picture = new byte[length];
        Frame frame = manager.getFrame(picture, 0, 0);

        // Release the frame and ensure that onBufferAvailable is called.
        reset(callback);
        manager.onFrameReleased(frame, frame.getData());
        verify(callback, times(1)).onBufferAvailable(picture);
    }

    @Test
    public void testOnFrameReleased_differentLength() {
        FrameManager manager = new FrameManager(1, callback);
        int length = manager.setUp(ImageFormat.NV21, new Size(50, 50));

        // A camera preview frame comes. Request a frame.
        byte[] picture = new byte[length];
        Frame frame = manager.getFrame(picture, 0, 0);

        // Don't release the frame. Change the allocation size.
        manager.setUp(ImageFormat.NV16, new Size(15, 15));

        // Now release the old frame and ensure that onBufferAvailable is NOT called,
        // because the released data has wrong length.
        manager.onFrameReleased(frame, frame.getData());
        reset(callback);
        verify(callback, never()).onBufferAvailable(picture);
    }
}

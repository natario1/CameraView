package com.otaliastudios.cameraview;


import android.graphics.ImageFormat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FrameManagerTest {

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
        manager.allocate(4, new Size(50, 50));
        verify(callback, times(1)).onBufferAvailable(any(byte[].class));
        reset(callback);

        manager = new FrameManager(5, callback);
        manager.allocate(4, new Size(50, 50));
        verify(callback, times(5)).onBufferAvailable(any(byte[].class));
    }

    @Test
    public void testFrameRecycling() {
        // A 1-pool manager will always recycle the same frame.
        FrameManager manager = new FrameManager(1, callback);
        manager.allocate(4, new Size(50, 50));

        Frame first = manager.getFrame(null, 0, 0, null, 0);
        first.release();

        Frame second = manager.getFrame(null, 0, 0, null, 0);
        second.release();

        assertEquals(first, second);
    }

    @Test
    public void testOnFrameReleased_nullBuffer() {
        FrameManager manager = new FrameManager(1, callback);
        manager.allocate(4, new Size(50, 50));
        reset(callback);

        Frame frame = manager.getFrame(null, 0, 0, null, 0);
        manager.onFrameReleased(frame);
        verify(callback, never()).onBufferAvailable(frame.getData());
    }

    @Test
    public void testOnFrameReleased_sameLength() {
        FrameManager manager = new FrameManager(1, callback);
        int length = manager.allocate(4, new Size(50, 50));

        // A camera preview frame comes. Request a frame.
        byte[] picture = new byte[length];
        Frame frame = manager.getFrame(picture, 0, 0, null, 0);

        // Release the frame and ensure that onBufferAvailable is called.
        reset(callback);
        manager.onFrameReleased(frame);
        verify(callback, times(1)).onBufferAvailable(picture);
    }

    @Test
    public void testOnFrameReleased_differentLength() {
        FrameManager manager = new FrameManager(1, callback);
        int length = manager.allocate(4, new Size(50, 50));

        // A camera preview frame comes. Request a frame.
        byte[] picture = new byte[length];
        Frame frame = manager.getFrame(picture, 0, 0, null, 0);

        // Don't release the frame. Change the allocation size.
        manager.allocate(2, new Size(15, 15));

        // Now release the old frame and ensure that onBufferAvailable is NOT called,
        // because the released data has wrong length.
        manager.onFrameReleased(frame);
        reset(callback);
        verify(callback, never()).onBufferAvailable(picture);
    }

    @Test
    public void testRelease() {
        FrameManager manager = new FrameManager(1, callback);
        int length = manager.allocate(4, new Size(50, 50));
        Frame first = manager.getFrame(new byte[length], 0, 0, null, 0);
        first.release(); // Store this frame in the queue.

        // Release the whole manager and ensure it clears the frame.
        manager.release();
        assertNull(first.getData());
        assertNull(first.mManager);
    }
}

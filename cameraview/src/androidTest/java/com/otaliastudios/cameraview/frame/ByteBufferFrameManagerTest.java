package com.otaliastudios.cameraview.frame;


import android.graphics.ImageFormat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.engine.offset.Angles;
import com.otaliastudios.cameraview.size.Size;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ByteBufferFrameManagerTest extends BaseTest {

    private final Angles angles = new Angles();
    private ByteBufferFrameManager.BufferCallback callback;

    @Before
    public void setUp() {
        callback = mock(ByteBufferFrameManager.BufferCallback.class);
    }

    @After
    public void tearDown() {
        callback = null;
    }

    @Test
    public void testAllocate() {
        ByteBufferFrameManager manager = new ByteBufferFrameManager(1, callback);
        manager.setUp(ImageFormat.NV21, new Size(50, 50), angles);
        verify(callback, times(1)).onBufferAvailable(any(byte[].class));
        reset(callback);

        manager = new ByteBufferFrameManager(5, callback);
        manager.setUp(ImageFormat.NV21, new Size(50, 50), angles);
        verify(callback, times(5)).onBufferAvailable(any(byte[].class));
    }

    @Test
    public void testOnFrameReleased_alreadyFull() {
        ByteBufferFrameManager manager = new ByteBufferFrameManager(1, callback);
        manager.setUp(ImageFormat.NV21, new Size(50, 50), angles);
        int length = manager.getFrameBytes();

        Frame frame1 = manager.getFrame(new byte[length], 0);
        assertNotNull(frame1);
        // Since frame1 is already taken and poolSize = 1, getFrame() would return null.
        // To create a new frame, freeze the first one.
        Frame frame2 = frame1.freeze();
        // Now release the first frame so it goes back into the pool.
        manager.onFrameReleased(frame1, (byte[]) frame1.getData());
        reset(callback);
        // Release the second. The pool is already full, so onBufferAvailable should not be called
        // since this Frame instance will NOT be reused.
        manager.onFrameReleased(frame2, (byte[]) frame2.getData());
        verify(callback, never()).onBufferAvailable((byte[]) frame2.getData());
    }

    @Test
    public void testOnFrameReleased_sameLength() {
        ByteBufferFrameManager manager = new ByteBufferFrameManager(1, callback);
        manager.setUp(ImageFormat.NV21, new Size(50, 50), angles);
        int length = manager.getFrameBytes();

        // A camera preview frame comes. Request a frame.
        byte[] picture = new byte[length];
        Frame frame = manager.getFrame(picture, 0);
        assertNotNull(frame);

        // Release the frame and ensure that onBufferAvailable is called.
        reset(callback);
        manager.onFrameReleased(frame, (byte[]) frame.getData());
        verify(callback, times(1)).onBufferAvailable(picture);
    }

    @Test
    public void testOnFrameReleased_differentLength() {
        ByteBufferFrameManager manager = new ByteBufferFrameManager(1, callback);
        manager.setUp(ImageFormat.NV21, new Size(50, 50), angles);
        int length = manager.getFrameBytes();

        // A camera preview frame comes. Request a frame.
        byte[] picture = new byte[length];
        Frame frame = manager.getFrame(picture, 0);
        assertNotNull(frame);

        // Don't release the frame. Change the allocation size.
        manager.setUp(ImageFormat.NV16, new Size(15, 15), angles);

        // Now release the old frame and ensure that onBufferAvailable is NOT called,
        // because the released data has wrong length.
        manager.onFrameReleased(frame, (byte[]) frame.getData());
        reset(callback);
        verify(callback, never()).onBufferAvailable(picture);
    }
}

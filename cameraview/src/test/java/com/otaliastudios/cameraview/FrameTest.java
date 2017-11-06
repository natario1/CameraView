package com.otaliastudios.cameraview;


import android.graphics.ImageFormat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FrameTest {

    private FrameManager manager;

    @Before
    public void setUp() {
        manager = mock(FrameManager.class);
    }

    @After
    public void tearDown() {
        manager = null;
    }

    @Test
    public void testDefaults() {
        Frame frame = new Frame(manager);
        assertEquals(frame.getTime(), -1);
        assertEquals(frame.getFormat(), -1);
        assertEquals(frame.getRotation(), 0);
        assertNull(frame.getData());
        assertNull(frame.getSize());
    }

    @Test
    public void testEquals() {
        Frame f1 = new Frame(manager);
        long time = 1000;
        f1.set(null, time, 90, null, ImageFormat.NV21);
        Frame f2 = new Frame(manager);
        f2.set(new byte[2], time, 0, new Size(10, 10), ImageFormat.NV21);
        assertEquals(f1, f2);

        f2.set(new byte[2], time + 1, 0, new Size(10, 10), ImageFormat.NV21);
        assertNotEquals(f1, f2);
    }

    @Test
    public void testRelease() {
        Frame frame = new Frame(manager);
        frame.set(new byte[2], 1000, 90, new Size(10, 10), ImageFormat.NV21);
        frame.release();

        assertEquals(frame.getTime(), -1);
        assertEquals(frame.getFormat(), -1);
        assertEquals(frame.getRotation(), 0);
        assertNull(frame.getData());
        assertNull(frame.getSize());
        verify(manager, times(1)).onFrameReleased(frame);
    }

    @Test
    public void testReleaseManager() {
        Frame frame = new Frame(manager);
        assertNotNull(frame.mManager);
        frame.releaseManager();
        assertNull(frame.mManager);
    }

    @Test
    public void testFreeze() {
        Frame frame = new Frame(manager);
        byte[] data = new byte[]{0, 1, 5, 0, 7, 3, 4, 5};
        long time = 1000;
        int rotation = 90;
        Size size = new Size(10, 10);
        int format = ImageFormat.NV21;
        frame.set(data, time, rotation, size, format);

        Frame frozen = frame.freeze();
        assertArrayEquals(data, frozen.getData());
        assertEquals(time, frozen.getTime());
        assertEquals(rotation, frozen.getRotation());
        assertEquals(size, frozen.getSize());

        // Mutate the first, ensure that frozen is not affected
        frame.set(new byte[]{3, 2, 1}, 50, 180, new Size(1, 1), ImageFormat.JPEG);
        assertArrayEquals(data, frozen.getData());
        assertEquals(time, frozen.getTime());
        assertEquals(rotation, frozen.getRotation());
        assertEquals(size, frozen.getSize());
        assertEquals(format, frozen.getFormat());
    }

}

package com.otaliastudios.cameraview;


import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FrameTest {

    @Test
    public void testDefaults() {
        Frame frame = new Frame();
        assertEquals(frame.getTime(), -1);
        assertEquals(frame.getRotation(), 0);
        assertNull(frame.getData());
    }

    @Test
    public void testEquals() {
        Frame f1 = new Frame();
        f1.set(null, 1000, 90);
        Frame f2 = new Frame();
        f2.set(new byte[2], 1000, 0);
        assertEquals(f1, f2);

        f2.set(new byte[2], 1001, 0);
        assertNotEquals(f1, f2);
    }

    @Test
    public void testClear() {
        Frame frame = new Frame();
        frame.set(new byte[2], 1000, 90);
        frame.clear();

        assertEquals(frame.getTime(), -1);
        assertEquals(frame.getRotation(), 0);
        assertNull(frame.getData());
    }

    @Test
    public void testFreeze() {
        Frame frame = new Frame();
        byte[] data = new byte[]{0, 1, 5, 0, 7, 3, 4, 5};
        long time = 1000;
        int rotation = 90;
        frame.set(data, time, rotation);

        Frame frozen = frame.freeze();
        assertArrayEquals(data, frozen.getData());
        assertEquals(time, frozen.getTime());
        assertEquals(rotation, frozen.getRotation());

        // Mutate the first, ensure that frozen is not affected
        frame.set(new byte[]{3, 2, 1}, 50, 180);
        assertArrayEquals(data, frozen.getData());
        assertEquals(time, frozen.getTime());
        assertEquals(rotation, frozen.getRotation());
    }

}

package com.otaliastudios.cameraview.size;


import org.junit.Test;

import static org.junit.Assert.*;

public class SizeTest {


    @Test
    public void testDimensions() {
        Size size = new Size(10, 20);
        assertEquals(size.getWidth(), 10);
        assertEquals(size.getHeight(), 20);
        assertEquals("10x20", size.toString());
    }

    @Test
    public void testFlip() {
        Size size = new Size(10, 20);
        Size flipped = size.flip();
        assertEquals(size.getWidth(), flipped.getHeight());
        assertEquals(size.getHeight(), flipped.getWidth());
    }

    @Test
    public void testEquals() {
        Size s1 = new Size(10, 20);
        assertEquals(s1, s1);
        assertNotEquals(s1, null);
        assertNotEquals(s1, "");

        Size s2 = new Size(10, 0);
        Size s3 = new Size(10, 20);
        assertEquals(s1, s3);
        assertNotEquals(s1, s2);
    }

    @Test
    public void testHashCode() {
        Size s1 = new Size(10, 20);
        Size s2 = new Size(10, 0);
        assertNotEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testCompare() {
        Size s1 = new Size(10, 20);
        Size s2 = new Size(10, 0);
        Size s3 = new Size(10, 20);
        //noinspection SimplifiableJUnitAssertion
        assertTrue(s1.compareTo(s3) == 0);
        assertTrue(s1.compareTo(s2) > 0);
        assertTrue(s2.compareTo(s1) < 0);
    }
}

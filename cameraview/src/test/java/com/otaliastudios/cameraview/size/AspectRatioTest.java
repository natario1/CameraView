package com.otaliastudios.cameraview.size;

import org.junit.Test;

import static org.junit.Assert.*;

public class AspectRatioTest {

    @Test
    public void testConstructor() {
        AspectRatio ratio = AspectRatio.of(50, 10);
        assertEquals(ratio.getX(), 5f, 0);
        assertEquals(ratio.getY(), 1f, 0);
        assertEquals(ratio.toString(), "5:1");
    }

    @Test
    public void testEquals() {
        AspectRatio ratio = AspectRatio.of(50, 10);
        assertNotNull(ratio);
        assertEquals(ratio, ratio);

        AspectRatio ratio1 = AspectRatio.of(5, 1);
        assertEquals(ratio, ratio1);

        AspectRatio.sCache.clear();
        AspectRatio ratio2 = AspectRatio.of(500, 100);
        assertEquals(ratio, ratio2);

        Size size = new Size(500, 100);
        assertTrue(ratio.matches(size));
    }

    @Test
    public void testCompare() {
        AspectRatio ratio1 = AspectRatio.of(10, 10);
        AspectRatio ratio2 = AspectRatio.of(10, 2);
        AspectRatio ratio3 = AspectRatio.of(2, 10);
        assertTrue(ratio1.compareTo(ratio2) < 0);
        assertTrue(ratio1.compareTo(ratio3) > 0);
        //noinspection EqualsWithItself,SimplifiableJUnitAssertion
        assertTrue(ratio1.compareTo(ratio1) == 0);
        assertNotEquals(ratio1.hashCode(), ratio2.hashCode());
    }

    @Test
    public void testInverse() {
        AspectRatio ratio = AspectRatio.of(50, 10);
        AspectRatio inverse = ratio.flip();
        assertEquals(inverse.getX(), 1f, 0);
        assertEquals(inverse.getY(), 5f, 0);
    }

    @Test(expected = NumberFormatException.class)
    public void testParse_notNumbers() {
        AspectRatio.parse("a:b");
    }

    @Test(expected = NumberFormatException.class)
    public void testParse_noColon() {
        AspectRatio.parse("24");
    }

    @Test
    public void testParse() {
        AspectRatio ratio = AspectRatio.parse("16:9");
        assertEquals(ratio.getX(), 16);
        assertEquals(ratio.getY(), 9);
    }
}

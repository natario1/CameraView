package com.otaliastudios.cameraview.gesture;


import androidx.annotation.NonNull;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.tools.SdkExclude;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * On API 26 these tests fail during Espresso's inRoot() - the window never gains focus.
 * This might be due to a system popup or something similar.
 */
@SdkExclude(minSdkVersion = 26, maxSdkVersion = 26)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ScrollGestureFinderTest extends GestureFinderTest<ScrollGestureFinder> {

    private final static long WAIT = 2000; // 500 was too short

    @Override
    protected ScrollGestureFinder createFinder(@NonNull GestureFinder.Controller controller) {
        return new ScrollGestureFinder(controller);
    }

    @Test
    public void testDefaults() {
        assertNull(finder.mType);
        assertEquals(finder.getPoints().length, 2);
        assertEquals(finder.getPoints()[0].x, 0, 0);
        assertEquals(finder.getPoints()[0].y, 0, 0);
        assertEquals(finder.getPoints()[1].x, 0, 0);
        assertEquals(finder.getPoints()[1].y, 0, 0);
    }

    @Test
    public void testScrollDisabled() {
        finder.setActive(false);
        touchOp.listen();
        touchOp.controller().start();
        onLayout().perform(swipeUp());
        Gesture found = touchOp.await(WAIT);
        assertNull(found);
    }

    private void testScroll(ViewAction scroll, Gesture expected, boolean increasing) {
        touchOp.listen();
        touchOp.controller().start();
        onLayout().perform(scroll);
        Gesture found = touchOp.await(WAIT);
        assertEquals(found, expected);

        // How will this move our parameter?
        float curr = 0.5f, min = 0f, max = 1f;
        float newValue = finder.computeValue(curr, min, max);
        if (increasing) {
            assertTrue(newValue >= curr);
            assertTrue(newValue <= max);
        } else {
            assertTrue(newValue <= curr);
            assertTrue(newValue >= min);
        }
    }

    @Test
    public void testScrollLeft() {
        testScroll(swipeLeft(), Gesture.SCROLL_HORIZONTAL, false);
    }

    @Test
    public void testScrollRight() {
        testScroll(swipeRight(), Gesture.SCROLL_HORIZONTAL, true);
    }

    @Test
    public void testScrollUp() {
        testScroll(swipeUp(), Gesture.SCROLL_VERTICAL, true);
    }

    @Test
    public void testScrollDown() {
        testScroll(swipeDown(), Gesture.SCROLL_VERTICAL, false);
    }


}

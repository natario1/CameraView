package com.otaliastudios.cameraview;


import android.content.Context;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ScrollGestureLayoutTest extends GestureLayoutTest<ScrollGestureLayout> {

    @Override
    protected ScrollGestureLayout create(Context context) {
        return new ScrollGestureLayout(context);
    }

    @Test
    public void testDefaults() {
        assertNull(layout.getGestureType());
        assertEquals(layout.getPoints().length, 2);
        assertEquals(layout.getPoints()[0].x, 0, 0);
        assertEquals(layout.getPoints()[0].y, 0, 0);
        assertEquals(layout.getPoints()[1].x, 0, 0);
        assertEquals(layout.getPoints()[1].y, 0, 0);
    }

    @Test
    public void testScrollDisabled() {
        layout.enable(false);
        touch.listen();
        touch.start();
        onLayout().perform(swipeUp());
        Gesture found = touch.await(500);
        assertNull(found);
    }

    private void testScroll(ViewAction scroll, Gesture expected, boolean increasing) {
        touch.listen();
        touch.start();
        onLayout().perform(scroll);
        Gesture found = touch.await(500);
        assertEquals(found, expected);

        // How will this move our parameter?
        float curr = 0.5f, min = 0f, max = 1f;
        float newValue = layout.scaleValue(curr, min, max);
        if (increasing) {
            assertTrue(newValue > curr);
            assertTrue(newValue <= max);
        } else {
            assertTrue(newValue < curr);
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

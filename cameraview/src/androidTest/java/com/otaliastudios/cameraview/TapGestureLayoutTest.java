package com.otaliastudios.cameraview;


import android.content.Context;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TapGestureLayoutTest extends GestureLayoutTest<TapGestureLayout> {

    @Override
    protected TapGestureLayout create(Context context) {
        return new TapGestureLayout(context);
    }

    @Test
    public void testDefaults() {
        assertNull(layout.getGestureType());
        assertEquals(layout.getPoints().length, 1);
        assertEquals(layout.getPoints()[0].x, 0, 0);
        assertEquals(layout.getPoints()[0].y, 0, 0);
    }

    @Test
    public void testTap() {
        touch.listen();
        touch.start();
        Size size = rule.getActivity().getContentSize();
        int x = (int) (size.getWidth() / 2f);
        int y = (int) (size.getHeight() / 2f);
        onView(withId(layout.getId())).perform(click(x, y));
        Gesture found = touch.await(500, TimeUnit.MILLISECONDS);
        assertEquals(found, Gesture.TAP);
        assertEquals(layout.getPoints()[0].x, x, 1);
        assertEquals(layout.getPoints()[0].y, y, 1);
    }

    @Test
    public void testTapWhileDisabled() {
        layout.enable(false);
        touch.listen();
        touch.start();
        Size size = rule.getActivity().getContentSize();
        int x = (int) (size.getWidth() / 2f);
        int y = (int) (size.getHeight() / 2f);
        onView(withId(layout.getId())).perform(click(x, y));
        Gesture found = touch.await(500, TimeUnit.MILLISECONDS);
        assertNull(found);
    }

    @Test
    public void testLongTap() {
        touch.listen();
        touch.start();
        Size size = rule.getActivity().getContentSize();
        int x = (int) (size.getWidth() / 2f);
        int y = (int) (size.getHeight() / 2f);
        onView(withId(layout.getId())).perform(longClick());
        Gesture found = touch.await(500, TimeUnit.MILLISECONDS);
        assertEquals(found, Gesture.LONG_TAP);
    }
}

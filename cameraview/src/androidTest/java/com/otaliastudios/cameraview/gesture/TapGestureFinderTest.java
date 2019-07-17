package com.otaliastudios.cameraview.gesture;


import androidx.annotation.NonNull;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.otaliastudios.cameraview.size.Size;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TapGestureFinderTest extends GestureFinderTest<TapGestureFinder> {

    @Override
    protected TapGestureFinder createFinder(@NonNull GestureFinder.Controller controller) {
        return new TapGestureFinder(controller);
    }

    @Test
    public void testDefaults() {
        assertNull(finder.mType);
        assertEquals(finder.getPoints().length, 1);
        assertEquals(finder.getPoints()[0].x, 0, 0);
        assertEquals(finder.getPoints()[0].y, 0, 0);
    }

    @Test
    public void testTap() {
        touchOp.listen();
        touchOp.start();
        GeneralClickAction a = new GeneralClickAction(
                Tap.SINGLE, GeneralLocation.CENTER, Press.FINGER,
                InputDevice.SOURCE_UNKNOWN, MotionEvent.BUTTON_PRIMARY);
        onLayout().perform(a);
        Gesture found = touchOp.await(500);

        assertEquals(found, Gesture.TAP);
        Size size = rule.getActivity().getContentSize();
        assertEquals(finder.getPoints()[0].x, (size.getWidth() / 2f), 1f);
        assertEquals(finder.getPoints()[0].y, (size.getHeight() / 2f), 1f);
    }

    @Test
    public void testTapWhileDisabled() {
        finder.setActive(false);
        touchOp.listen();
        touchOp.start();
        onLayout().perform(click());
        Gesture found = touchOp.await(500);
        assertNull(found);
    }

    @Test
    public void testLongTap() {
        touchOp.listen();
        touchOp.start();
        GeneralClickAction a = new GeneralClickAction(
                Tap.LONG, GeneralLocation.CENTER, Press.FINGER,
                InputDevice.SOURCE_UNKNOWN, MotionEvent.BUTTON_PRIMARY);
        onLayout().perform(a);
        Gesture found = touchOp.await(500);
        assertEquals(found, Gesture.LONG_TAP);
        Size size = rule.getActivity().getContentSize();
        assertEquals(finder.getPoints()[0].x, (size.getWidth() / 2f), 1f);
        assertEquals(finder.getPoints()[0].y, (size.getHeight() / 2f), 1f);
    }
}

package com.otaliastudios.cameraview;


import android.content.Context;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PinchGestureLayoutTest extends GestureLayoutTest<PinchGestureLayout> {

    @Override
    protected PinchGestureLayout create(Context context) {
        return new PinchGestureLayout(context);
    }

    @Test
    public void testDefaults() {
        assertEquals(layout.getGestureType(), Gesture.PINCH);
        assertEquals(layout.getPoints().length, 2);
        assertEquals(layout.getPoints()[0].x, 0, 0);
        assertEquals(layout.getPoints()[0].y, 0, 0);
        assertEquals(layout.getPoints()[1].x, 0, 0);
        assertEquals(layout.getPoints()[1].y, 0, 0);
    }

    // TODO: test pinch open
    // TODO: test pinch close
    // TODO: test pinch disabled

    // Possible approach: mimic pinch gesture and let espresso test.
    // Too lazy to do this now, but it's possible.
    // https://stackoverflow.com/questions/11523423/how-to-generate-zoom-pinch-gesture-for-testing-for-android

    public abstract class PinchViewAction implements ViewAction {
    }

    private void testPinch(ViewAction action, boolean increasing) {
        touch.listen();
        touch.start();
        onLayout().perform(action);
        Gesture found = touch.await(10000);
        assertNotNull(found);

        // How will this move  our parameter?
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
}

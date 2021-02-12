package com.otaliastudios.cameraview.gesture;


import androidx.annotation.NonNull;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.tools.SdkExclude;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * On API 26 these tests fail during Espresso's inRoot() - the window never gains focus.
 * This might be due to a system popup or something similar.
 */
@SdkExclude(minSdkVersion = 26, maxSdkVersion = 26)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PinchGestureFinderTest extends GestureFinderTest<PinchGestureFinder> {

    @Override
    protected PinchGestureFinder createFinder(@NonNull GestureFinder.Controller controller) {
        return new PinchGestureFinder(controller);
    }

    @Test
    public void testDefaults() {
        assertEquals(finder.getGesture(), Gesture.PINCH);
        assertEquals(finder.getPoints().length, 2);
        assertEquals(finder.getPoints()[0].x, 0, 0);
        assertEquals(finder.getPoints()[0].y, 0, 0);
        assertEquals(finder.getPoints()[1].x, 0, 0);
        assertEquals(finder.getPoints()[1].y, 0, 0);
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
        touchOp.listen();
        touchOp.controller().start();
        onLayout().perform(action);
        Gesture found = touchOp.await(10000);
        assertNotNull(found);

        // How will this move  our parameter?
        float curr = 0.5f, min = 0f, max = 1f;
        float newValue = finder.computeValue(curr, min, max);
        if (increasing) {
            assertTrue(newValue > curr);
            assertTrue(newValue <= max);
        } else {
            assertTrue(newValue < curr);
            assertTrue(newValue >= min);
        }
    }
}

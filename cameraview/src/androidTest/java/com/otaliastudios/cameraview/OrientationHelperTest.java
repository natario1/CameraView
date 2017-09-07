package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.OrientationEventListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class OrientationHelperTest extends BaseTest {

    private OrientationHelper helper;
    private OrientationHelper.Callbacks callbacks;

    @Before
    public void setUp() {
        ui(new Runnable() {
            @Override
            public void run() {
                callbacks = mock(OrientationHelper.Callbacks.class);
                helper = new OrientationHelper(context(), callbacks);
            }
        });
    }

    @After
    public void tearDown() {
        callbacks = null;
        helper = null;
    }

    @Test
    public void testEnable() {
        assertNotNull(helper.mListener);
        assertNull(helper.mDisplay);

        helper.enable(context());
        assertNotNull(helper.mListener);
        assertNotNull(helper.mDisplay);

        // Ensure nothing bad if called twice.
        helper.enable(context());
        assertNotNull(helper.mListener);
        assertNotNull(helper.mDisplay);

        helper.disable();
        assertNotNull(helper.mListener);
        assertNull(helper.mDisplay);

        verify(callbacks, atLeastOnce()).onDisplayOffsetChanged(anyInt());
    }

    @Test
    public void testRotation() {
        helper.enable(context());

        reset(callbacks); // Reset counts.
        helper.mListener.onOrientationChanged(OrientationEventListener.ORIENTATION_UNKNOWN);
        assertEquals(helper.mLastOrientation, 0);
        helper.mListener.onOrientationChanged(10);
        assertEquals(helper.mLastOrientation, 0);
        helper.mListener.onOrientationChanged(-10);
        assertEquals(helper.mLastOrientation, 0);
        helper.mListener.onOrientationChanged(44);
        assertEquals(helper.mLastOrientation, 0);
        helper.mListener.onOrientationChanged(360);
        assertEquals(helper.mLastOrientation, 0);

        // Callback called just once.
        verify(callbacks, times(1)).onDeviceOrientationChanged(0);

        helper.mListener.onOrientationChanged(90);
        helper.mListener.onOrientationChanged(91);
        assertEquals(helper.mLastOrientation, 90);
        verify(callbacks, times(1)).onDeviceOrientationChanged(90);

        helper.mListener.onOrientationChanged(180);
        assertEquals(helper.mLastOrientation, 180);
        verify(callbacks, times(1)).onDeviceOrientationChanged(180);

        helper.mListener.onOrientationChanged(270);
        assertEquals(helper.mLastOrientation, 270);
        verify(callbacks, times(1)).onDeviceOrientationChanged(270);
    }
}

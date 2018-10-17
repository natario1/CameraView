package com.otaliastudios.cameraview;


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
    private OrientationHelper.Callback callback;

    @Before
    public void setUp() {
        ui(new Runnable() {
            @Override
            public void run() {
                callback = mock(OrientationHelper.Callback.class);
                helper = new OrientationHelper(context(), callback);
            }
        });
    }

    @After
    public void tearDown() {
        callback = null;
        helper = null;
    }

    @Test
    public void testEnable() {
        assertNotNull(helper.mListener);
        assertEquals(helper.getDisplayOffset(), -1);
        assertEquals(helper.getDeviceOrientation(), -1);

        helper.enable(context());
        assertNotNull(helper.mListener);
        assertNotEquals(helper.getDisplayOffset(), -1); // Don't know about device orientation.

        // Ensure nothing bad if called twice.
        helper.enable(context());
        assertNotNull(helper.mListener);
        assertNotEquals(helper.getDisplayOffset(), -1);

        helper.disable();
        assertNotNull(helper.mListener);
        assertEquals(helper.getDisplayOffset(), -1);
        assertEquals(helper.getDeviceOrientation(), -1);
    }

    @Test
    public void testRotation() {

        // Sometimes (on some APIs) the helper will trigger an update to 0
        // right after enabling. But that's fine for us, times(1) will be OK either way.
        helper.enable(context());
        helper.mListener.onOrientationChanged(OrientationEventListener.ORIENTATION_UNKNOWN);
        assertEquals(helper.getDeviceOrientation(), 0);
        helper.mListener.onOrientationChanged(10);
        assertEquals(helper.getDeviceOrientation(), 0);
        helper.mListener.onOrientationChanged(-10);
        assertEquals(helper.getDeviceOrientation(), 0);
        helper.mListener.onOrientationChanged(44);
        assertEquals(helper.getDeviceOrientation(), 0);
        helper.mListener.onOrientationChanged(360);
        assertEquals(helper.getDeviceOrientation(), 0);

        // Callback called just once.
        verify(callback, times(1)).onDeviceOrientationChanged(0);

        helper.mListener.onOrientationChanged(90);
        helper.mListener.onOrientationChanged(91);
        assertEquals(helper.getDeviceOrientation(), 90);
        verify(callback, times(1)).onDeviceOrientationChanged(90);

        helper.mListener.onOrientationChanged(180);
        assertEquals(helper.getDeviceOrientation(), 180);
        verify(callback, times(1)).onDeviceOrientationChanged(180);

        helper.mListener.onOrientationChanged(270);
        assertEquals(helper.getDeviceOrientation(), 270);
        verify(callback, times(1)).onDeviceOrientationChanged(270);

        // It is still 270 after ORIENTATION_UNKNOWN
        helper.mListener.onOrientationChanged(OrientationEventListener.ORIENTATION_UNKNOWN);
        assertEquals(helper.getDeviceOrientation(), 270);
        verify(callback, times(1)).onDeviceOrientationChanged(270);
    }
}

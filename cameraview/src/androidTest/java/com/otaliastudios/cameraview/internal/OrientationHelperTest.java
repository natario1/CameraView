package com.otaliastudios.cameraview.internal;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import android.view.OrientationEventListener;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.internal.OrientationHelper;

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
        uiSync(new Runnable() {
            @Override
            public void run() {
                callback = mock(OrientationHelper.Callback.class);
                helper = new OrientationHelper(getContext(), callback);
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
        // On some API levels, enable() needs to be run on the UI thread.
        uiSync(new Runnable() {
            @Override
            public void run() {
                assertEquals(helper.getLastDisplayOffset(), -1);
                assertEquals(helper.getLastDeviceOrientation(), -1);

                helper.enable();
                assertNotEquals(helper.getLastDisplayOffset(), -1); // Don't know about device orientation.

                // Ensure nothing bad if called twice.
                helper.enable();
                assertNotEquals(helper.getLastDisplayOffset(), -1);

                helper.disable();
                assertEquals(helper.getLastDisplayOffset(), -1);
                assertEquals(helper.getLastDeviceOrientation(), -1);
            }
        });
    }

    @Test
    public void testRotation() {
        // On some API levels, enable() needs to be run on the UI thread.
        uiSync(new Runnable() {
            @Override
            public void run() {
                // Sometimes (on some APIs) the helper will trigger an update to 0
                // right after enabling. But that's fine for us, times(1) will be OK either way.
                helper.enable();
                helper.mDeviceOrientationListener.onOrientationChanged(OrientationEventListener.ORIENTATION_UNKNOWN);
                assertEquals(helper.getLastDeviceOrientation(), 0);
                helper.mDeviceOrientationListener.onOrientationChanged(10);
                assertEquals(helper.getLastDeviceOrientation(), 0);
                helper.mDeviceOrientationListener.onOrientationChanged(-10);
                assertEquals(helper.getLastDeviceOrientation(), 0);
                helper.mDeviceOrientationListener.onOrientationChanged(44);
                assertEquals(helper.getLastDeviceOrientation(), 0);
                helper.mDeviceOrientationListener.onOrientationChanged(360);
                assertEquals(helper.getLastDeviceOrientation(), 0);

                // Callback called just once.
                verify(callback, times(1)).onDeviceOrientationChanged(0);

                helper.mDeviceOrientationListener.onOrientationChanged(90);
                helper.mDeviceOrientationListener.onOrientationChanged(91);
                assertEquals(helper.getLastDeviceOrientation(), 90);
                verify(callback, times(1)).onDeviceOrientationChanged(90);

                helper.mDeviceOrientationListener.onOrientationChanged(180);
                assertEquals(helper.getLastDeviceOrientation(), 180);
                verify(callback, times(1)).onDeviceOrientationChanged(180);

                helper.mDeviceOrientationListener.onOrientationChanged(270);
                assertEquals(helper.getLastDeviceOrientation(), 270);
                verify(callback, times(1)).onDeviceOrientationChanged(270);

                // It is still 270 after ORIENTATION_UNKNOWN
                helper.mDeviceOrientationListener.onOrientationChanged(OrientationEventListener.ORIENTATION_UNKNOWN);
                assertEquals(helper.getLastDeviceOrientation(), 270);
                verify(callback, times(1)).onDeviceOrientationChanged(270);
            }
        });
    }
}

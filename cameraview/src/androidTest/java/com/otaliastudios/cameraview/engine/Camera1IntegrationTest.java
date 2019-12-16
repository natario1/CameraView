package com.otaliastudios.cameraview.engine;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.tools.Op;
import com.otaliastudios.cameraview.tools.Retry;
import com.otaliastudios.cameraview.tools.SdkExclude;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.RequiresDevice;

import java.util.Collection;

import static org.junit.Assert.assertNotNull;

/**
 * These tests work great on real devices, and are the only way to test actual CameraEngine
 * implementation - we really need to open the camera device.
 * Unfortunately they fail unreliably on emulated devices, due to some bug with the
 * emulated camera controller.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
// @RequiresDevice
public class Camera1IntegrationTest extends CameraIntegrationTest<Camera1Engine> {

    @NonNull
    @Override
    protected Engine getEngine() {
        return Engine.CAMERA1;
    }

    @Override
    protected long getMeteringTimeoutMillis() {
        return Camera1Engine.AUTOFOCUS_END_DELAY_MILLIS;
    }

    @Override
    public void testFrameProcessing_maxSize() {
        // Camera1Engine does not support different sizes.
        // super.testFrameProcessing_maxSize();
    }
}

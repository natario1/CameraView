package com.otaliastudios.cameraview.engine;

import com.otaliastudios.cameraview.DoNotRunOnTravis;
import com.otaliastudios.cameraview.controls.Engine;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

/**
 * These tests work great on real devices, and are the only way to test actual CameraEngine
 * implementation - we really need to open the camera device.
 * Unfortunately they fail unreliably on emulated devices, due to some bug with the
 * emulated camera controller.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
@DoNotRunOnTravis(because = "These do work but fail on CI emulators, due to bugs in the Camera1 emulated devices.")
public class CameraIntegration1Test extends CameraIntegrationTest {

    @NonNull
    @Override
    protected Engine getEngine() {
        return Engine.CAMERA1;
    }

    @Override
    protected long getMeteringTimeoutMillis() {
        return Camera1Engine.AUTOFOCUS_END_DELAY_MILLIS;
    }
}

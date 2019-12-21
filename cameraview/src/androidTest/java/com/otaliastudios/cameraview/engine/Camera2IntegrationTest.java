package com.otaliastudios.cameraview.engine;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;

import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.RequiresDevice;

import java.util.concurrent.CountDownLatch;

/**
 * These tests work great on real devices, and are the only way to test actual CameraEngine
 * implementation - we really need to open the camera device.
 * Unfortunately they fail unreliably on emulated devices, due to some bug with the
 * emulated camera controller.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
// @RequiresDevice
public class Camera2IntegrationTest extends CameraIntegrationTest<Camera2Engine> {

    @NonNull
    @Override
    protected Engine getEngine() {
        return Engine.CAMERA2;
    }

    /* @Override
    protected void onOpenSync() {
        super.onOpenSync();
        // Extra wait for the first frame to be dispatched.
        // This is because various classes require getLastResult to be non-null
        // and that's typically the case in a real app.
        final CountDownLatch latch = new CountDownLatch(1);
        new BaseAction() {
            @Override
            public void onCaptureCompleted(@NonNull ActionHolder holder,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(holder, request, result);
                latch.countDown();
                setState(STATE_COMPLETED);
            }
        }.start(controller);
        try { latch.await(); } catch (InterruptedException ignore) {}
    } */

    @Override
    protected long getMeteringTimeoutMillis() {
        return Camera2Engine.METER_TIMEOUT;
    }

    /**
     * setMaxDuration can crash on legacy devices (most emulator are), and I don't see
     * any way to fix this in code. They shouldn't use Camera2 at all.
     * @return true if possible.
     */
    @Override
    protected boolean canSetVideoMaxDuration() {
        if (!super.canSetVideoMaxDuration()) return false;
        boolean shouldOpen = !camera.isOpened();
        if (shouldOpen) openSync(true);
        boolean result = controller.readCharacteristic(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1)
                != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        if (shouldOpen) closeSync(true);
        return result;
    }

    @Override
    public void testFrameProcessing_freezeRelease() {
        // Camera2 Frames are not freezable.
        // super.testFrameProcessing_freezeRelease();
    }
}

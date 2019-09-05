package com.otaliastudios.cameraview.engine;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;

import com.otaliastudios.cameraview.DoNotRunOnTravis;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import java.util.concurrent.CountDownLatch;

/**
 * These tests work great on real devices, and are the only way to test actual CameraEngine
 * implementation - we really need to open the camera device.
 * Unfortunately they fail unreliably on emulated devices, due to some bug with the
 * emulated camera controller.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
@DoNotRunOnTravis(because = "These do work but fail on CI emulators.")
public class CameraIntegration2Test extends CameraIntegrationTest {

    @NonNull
    @Override
    protected Engine getEngine() {
        return Engine.CAMERA2;
    }

    @Override
    protected void onOpenSync() {
        super.onOpenSync();
        // Extra wait for the first frame to be dispatched.
        // This is because various classes require getLastResult to be non-null
        // and that's typically the case in a real app.
        Camera2Engine engine = (Camera2Engine) controller;
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
        }.start(engine);
        try { latch.await(); } catch (InterruptedException ignore) {}
    }

    @Override
    protected long getMeteringTimeoutMillis() {
        return Camera2Engine.METER_TIMEOUT;
    }
}

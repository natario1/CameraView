package com.otaliastudios.cameraview.engine;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.TestActivity;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.PictureFormat;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.engine.orchestrator.CameraState;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.SizeSelectors;
import com.otaliastudios.cameraview.tools.Emulator;
import com.otaliastudios.cameraview.tools.Op;
import com.otaliastudios.cameraview.internal.WorkerHandler;
import com.otaliastudios.cameraview.overlay.Overlay;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.tools.Retry;
import com.otaliastudios.cameraview.tools.RetryRule;
import com.otaliastudios.cameraview.tools.SdkExclude;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class CameraIntegrationTest<E extends CameraBaseEngine> extends BaseTest {

    private final static CameraLogger LOG = CameraLogger.create(CameraIntegrationTest.class.getSimpleName());
    private final static long DELAY = 8000;

    @Rule
    public ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class);

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    );

    protected CameraView camera;
    protected E controller;
    private CameraListener listener;
    private Op<Throwable> error;

    @NonNull
    protected abstract Engine getEngine();

    @Before
    public void setUp() {
        LOG.w("[TEST STARTED]", "Setting up camera.");
        WorkerHandler.destroyAll();

        uiSync(new Runnable() {
            @Override
            public void run() {
                camera = new CameraView(activityRule.getActivity()) {
                    @NonNull
                    @Override
                    protected CameraEngine instantiateCameraEngine(
                            @NonNull Engine engine,
                            @NonNull CameraEngine.Callback callback) {
                        //noinspection unchecked
                        controller = (E) super.instantiateCameraEngine(getEngine(), callback);
                        return controller;
                    }
                };
                camera.setExperimental(true);
                camera.setEngine(getEngine());
                activityRule.getActivity().inflate(camera);
            }
        });

        listener = mock(CameraListener.class);
        camera.addCameraListener(listener);

        error = new Op<>();
        WorkerHandler crashThread = WorkerHandler.get("CrashThread");
        crashThread.getThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable exception) {
                error.controller().end(exception);
            }
        });
        controller.mCrashHandler = crashThread.getHandler();

        camera.addCameraListener(new CameraListener() {
            @Override
            public void onCameraError(@NonNull CameraException exception) {
                super.onCameraError(exception);
                if (exception.isUnrecoverable()) {
                    LOG.e("[UNRECOVERABLE CAMERAEXCEPTION]",
                            "Got unrecoverable exception, not clear what to do in a test.");
                }
            }
        });

    }

    @After
    public void tearDown() {
        LOG.w("[TEST ENDED]", "Tearing down camera.");
        camera.destroy();
        WorkerHandler.destroyAll();
        LOG.w("[TEST ENDED]", "Torn down camera.");
    }

    protected final CameraOptions openSync(boolean expectSuccess) {
        final Op<CameraOptions> open = new Op<>();
        doEndOp(open, 0).when(listener).onCameraOpened(any(CameraOptions.class));
        camera.open();
        CameraOptions result = open.await(DELAY);
        if (expectSuccess) {
            LOG.i("[OPEN SYNC]", "Expecting success.");
            assertNotNull("Can open", result);
            onOpenSync();
        } else {
            LOG.i("[OPEN SYNC]", "Expecting failure.");
            assertNull("Should not open", result);
        }
        return result;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    protected void onOpenSync() {
        // Extra wait for the bind and preview state, so we run tests in a fully operational
        // state. If we didn't do so, we could have null values, for example, in getPictureSize
        // or in getSnapshotSize.
        while (controller.getState() != CameraState.PREVIEW) {}
    }

    protected final void closeSync(boolean expectSuccess) {
        final Op<Boolean> close = new Op<>();
        doEndOp(close, true).when(listener).onCameraClosed();
        camera.close();
        Boolean result = close.await(DELAY);
        if (expectSuccess) {
            LOG.i("[CLOSE SYNC]", "Expecting success.");
            assertNotNull("Can close", result);
        } else {
            LOG.i("[CLOSE SYNC]", "Expecting failure.");
            assertNull("Should not close", result);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    @Nullable
    private VideoResult waitForVideoResult(boolean expectSuccess) {
        Op<Boolean> wait1 = new Op<>();
        Op<VideoResult> wait2 = new Op<>();
        doEndOp(wait1, true).when(listener).onVideoRecordingEnd();
        doEndOp(wait1, false).when(listener).onCameraError(argThat(new ArgumentMatcher<CameraException>() {
            @Override
            public boolean matches(CameraException argument) {
                return argument.getReason() == CameraException.REASON_VIDEO_FAILED;
            }
        }));
        doEndOp(wait2, 0).when(listener).onVideoTaken(any(VideoResult.class));
        doEndOp(wait2, null).when(listener).onCameraError(argThat(new ArgumentMatcher<CameraException>() {
            @Override
            public boolean matches(CameraException argument) {
                return argument.getReason() == CameraException.REASON_VIDEO_FAILED;
            }
        }));
        int maxLoops = 10;
        int loops = 0;

        // First wait for onVideoRecordingEnd().
        // It seems that when running all the tests together, the device can go in some
        // power saving mode which makes the CPU extremely slow. This is especially problematic
        // with video snapshots where we do lots of processing. The videoEnd callback can return
        // long after the actual stop() call, so if we're still processing, let's wait more.
        LOG.i("[WAIT VIDEO]", "Waiting for onVideoRecordingEnd()...");
        Boolean wait1Result = wait1.await(DELAY);
        if (expectSuccess) {
            while (wait1Result == null && loops <= maxLoops) {
                LOG.w("[WAIT VIDEO]", "Waiting extra", DELAY, "milliseconds...");
                wait1.listen();
                wait1Result = wait1.await(DELAY);
                loops++;
            }
        }

        // Now wait for onVideoResult(). One cycle should be enough.
        LOG.i("[WAIT VIDEO]", "Waiting for onVideoTaken()...");
        VideoResult wait2Result = wait2.await(DELAY);
        if (expectSuccess) {
            while (wait2Result == null && loops <= maxLoops) {
                LOG.w("[WAIT VIDEO]", "Waiting extra", DELAY, "milliseconds...");
                wait2.listen();
                wait2Result = wait2.await(DELAY);
                loops++;
            }
        }

        // Assert.
        if (expectSuccess) {
            assertNotNull("Should call onVideoRecordingEnd", wait1Result);
            assertTrue("Should call onVideoRecordingEnd", wait1Result);
            assertNotNull("Should call onVideoTaken", wait2Result);
        } else {
            assertTrue("Should not call onVideoRecordingEnd",
                    wait1Result == null || !wait1Result);
            assertNull("Should not call onVideoTaken", wait2Result);
        }
        return wait2Result;
    }

    @Nullable
    private PictureResult waitForPictureResult(boolean expectSuccess) {
        final Op<PictureResult> pic = new Op<>();
        doEndOp(pic, 0).when(listener).onPictureTaken(any(PictureResult.class));
        doEndOp(pic, null).when(listener).onCameraError(argThat(new ArgumentMatcher<CameraException>() {
            @Override
            public boolean matches(CameraException argument) {
                return argument.getReason() == CameraException.REASON_PICTURE_FAILED;
            }
        }));
        PictureResult result = pic.await(DELAY);
        if (expectSuccess) {
            LOG.i("[WAIT PICTURE]", "Expecting success.");
            assertNotNull("Can take picture", result);
        } else {
            LOG.i("[WAIT PICTURE]", "Expecting failure.");
            assertNull("Should not take picture", result);
        }
        return result;
    }

    /**
     * Emulators do not respond well to setVideoMaxDuration() with full videos.
     * The mediaRecorder.stop() call can hang forever.
     * @return true if possible
     */
    protected boolean canSetVideoMaxDuration() {
        return !Emulator.isEmulator();
    }

    protected void takeVideoSync(boolean expectSuccess) {
        takeVideoSync(expectSuccess,0);
    }

    protected void takeVideoSync(boolean expectSuccess, int duration) {
        final Op<Boolean> op = new Op<>();
        doEndOp(op, true).when(listener).onVideoRecordingStart();
        doEndOp(op, false).when(listener).onCameraError(argThat(new ArgumentMatcher<CameraException>() {
            @Override
            public boolean matches(CameraException argument) {
                return argument.getReason() == CameraException.REASON_VIDEO_FAILED;
            }
        }));
        File file = new File(getContext().getFilesDir(), "video.mp4");
        if (duration > 0) {
            if (canSetVideoMaxDuration()) {
                camera.takeVideo(file, duration);
            } else {
                final int delay = Math.round(duration * 1.2F); // Compensate for thread jumps, ...
                uiSync(new Runnable() {
                    @Override
                    public void run() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.stopVideo();
                            }
                        }, delay);
                    }
                });
                camera.takeVideo(file);
            }
        } else {
            camera.takeVideo(file);
        }
        Boolean result = op.await(DELAY);
        if (expectSuccess) {
            LOG.i("[WAIT VIDEO START]", "Expecting success.");
            assertNotNull("should start video recording or get CameraError", result);
            assertTrue("should start video recording successfully", result);
        } else {
            LOG.i("[WAIT VIDEO START]", "Expecting failure.");
            assertTrue("should not start video recording", result == null || !result);
        }
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    private void takeVideoSnapshotSync(boolean expectSuccess) {
        takeVideoSnapshotSync(expectSuccess,0);
    }

    private void takeVideoSnapshotSync(boolean expectSuccess, int duration) {
        final Op<Boolean> op = new Op<>();
        doEndOp(op, true).when(listener).onVideoRecordingStart();
        doEndOp(op, false).when(listener).onCameraError(argThat(new ArgumentMatcher<CameraException>() {
            @Override
            public boolean matches(CameraException argument) {
                return argument.getReason() == CameraException.REASON_VIDEO_FAILED;
            }
        }));
        File file = new File(getContext().getFilesDir(), "video.mp4");
        if (duration > 0) {
            camera.takeVideoSnapshot(file, duration);
        } else {
            camera.takeVideoSnapshot(file);
        }
        Boolean result = op.await(DELAY);
        if (expectSuccess) {
            LOG.i("[WAIT VIDEO SNAP START]", "Expecting success.");
            assertNotNull("should start video recording or get CameraError", result);
            assertTrue("should start video recording successfully", result);
        } else {
            LOG.i("[WAIT VIDEO SNAP START]", "Expecting failure.");
            assertTrue("should not start video recording", result == null || !result);
        }
    }

    private void waitForError() throws Throwable {
        Throwable throwable = error.await(DELAY);
        if (throwable != null) {
            throw throwable;
        }
    }

    //region test open/close

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testOpenClose() {
        assertEquals(CameraState.OFF, controller.getState());
        openSync(true);
        assertTrue(controller.getState().isAtLeast(CameraState.ENGINE));
        closeSync(true);
        assertEquals(CameraState.OFF, controller.getState());
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testOpenTwice() {
        openSync(true);
        openSync(false);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCloseTwice() {
        closeSync(false);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    // This works great on the device but crashes often on the emulator.
    // There must be something wrong with the emulated camera...
    // Like stopPreview() and release() are not really sync calls?
    public void testConcurrentCalls() throws Exception {
        final CountDownLatch latch = new CountDownLatch(4);
        doCountDown(latch).when(listener).onCameraOpened(any(CameraOptions.class));
        doCountDown(latch).when(listener).onCameraClosed();

        camera.open();
        camera.close();
        camera.open();
        camera.close();

        boolean did = latch.await(10, TimeUnit.SECONDS);
        assertTrue("Handles concurrent calls to start & stop, " + latch.getCount(), did);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testStartInitializesOptions() {
        assertNull(camera.getCameraOptions());
        openSync(true);
        assertNotNull(camera.getCameraOptions());
    }

    //endregion

    //region test Facing/SessionType
    // Test things that should reset the camera.

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetFacing() throws Exception {
        CameraOptions o = openSync(true);
        int size = o.getSupportedFacing().size();
        if (size > 1) {
            // set facing should call stop and start again.
            final CountDownLatch latch = new CountDownLatch(2);
            doCountDown(latch).when(listener).onCameraOpened(any(CameraOptions.class));
            doCountDown(latch).when(listener).onCameraClosed();

            camera.toggleFacing();

            boolean did = latch.await(2, TimeUnit.SECONDS);
            assertTrue("Handles setFacing while active", did);
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetMode() throws Exception {
        camera.setMode(Mode.PICTURE);
        openSync(true);

        // set session type should call stop and start again.
        final CountDownLatch latch = new CountDownLatch(2);
        doCountDown(latch).when(listener).onCameraOpened(any(CameraOptions.class));
        doCountDown(latch).when(listener).onCameraClosed();

        camera.setMode(Mode.VIDEO);

        boolean did = latch.await(2, TimeUnit.SECONDS);
        assertTrue("Handles setMode while active", did);
        assertEquals(camera.getMode(), Mode.VIDEO);
    }

    //endregion

    //region test Set Parameters
    // When camera is open, parameters will be set only if supported.

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetZoom() {
        CameraOptions options = openSync(true);
        float oldValue = camera.getZoom();
        float newValue = 0.65f;
        camera.setZoom(newValue);
        Op<Void> op = new Op<>(controller.mZoomTask);
        op.await(500);

        if (options.isZoomSupported()) {
            assertEquals(newValue, camera.getZoom(), 0f);
        } else {
            assertEquals(oldValue, camera.getZoom(), 0f);
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetExposureCorrection() {
        CameraOptions options = openSync(true);
        float oldValue = camera.getExposureCorrection();
        float newValue = options.getExposureCorrectionMaxValue();
        camera.setExposureCorrection(newValue);
        Op<Void> op = new Op<>(controller.mExposureCorrectionTask);
        op.await(300);

        if (options.isExposureCorrectionSupported()) {
            assertEquals(newValue, camera.getExposureCorrection(), 0f);
        } else {
            assertEquals(oldValue, camera.getExposureCorrection(), 0f);
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetFlash() {
        CameraOptions options = openSync(true);
        Flash[] values = Flash.values();
        Flash oldValue = camera.getFlash();
        for (Flash value : values) {

            camera.setFlash(value);
            Op<Void> op = new Op<>(controller.mFlashTask);
            op.await(300);
            if (options.supports(value)) {
                assertEquals(camera.getFlash(), value);
                oldValue = value;
            } else {
                assertEquals(camera.getFlash(), oldValue);
            }
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetWhiteBalance() {
        CameraOptions options = openSync(true);
        WhiteBalance[] values = WhiteBalance.values();
        WhiteBalance oldValue = camera.getWhiteBalance();
        for (WhiteBalance value : values) {
            camera.setWhiteBalance(value);
            Op<Void> op = new Op<>(controller.mWhiteBalanceTask);
            op.await(300);
            if (options.supports(value)) {
                assertEquals(camera.getWhiteBalance(), value);
                oldValue = value;
            } else {
                assertEquals(camera.getWhiteBalance(), oldValue);
            }
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetHdr() {
        CameraOptions options = openSync(true);
        Hdr[] values = Hdr.values();
        Hdr oldValue = camera.getHdr();
        for (Hdr value : values) {
            camera.setHdr(value);
            Op<Void> op = new Op<>(controller.mHdrTask);
            op.await(300);
            if (options.supports(value)) {
                assertEquals(camera.getHdr(), value);
                oldValue = value;
            } else {
                assertEquals(camera.getHdr(), oldValue);
            }
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetAudio() {
        openSync(true);
        Audio[] values = Audio.values();
        for (Audio value : values) {
            camera.setAudio(value);
            assertEquals(camera.getAudio(), value);
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetLocation() {
        openSync(true);
        camera.setLocation(10d, 2d);
        Op<Void> op = new Op<>(controller.mLocationTask);
        op.await(300);
        assertNotNull(camera.getLocation());
        assertEquals(camera.getLocation().getLatitude(), 10d, 0d);
        assertEquals(camera.getLocation().getLongitude(), 2d, 0d);
        // This also ensures there are no crashes when attaching it to camera parameters.
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetPreviewFrameRate() {
        CameraOptions options = openSync(true);
        camera.setPreviewFrameRate(30);
        Op<Void> op = new Op<>(controller.mPreviewFrameRateTask);
        op.await(300);
        assertEquals(camera.getPreviewFrameRate(),
                Math.min(options.getPreviewFrameRateMaxValue(),
                        Math.max(options.getPreviewFrameRateMinValue(), 30)),
                0);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testSetPlaySounds() {
        boolean oldValue = camera.getPlaySounds();
        boolean newValue = !oldValue;
        camera.setPlaySounds(newValue);
        Op<Void> op = new Op<>(controller.mPlaySoundsTask);
        op.await(300);

        if (controller instanceof Camera1Engine) {
            Camera1Engine camera1Engine = (Camera1Engine) controller;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(camera1Engine.mCameraId, info);
                if (info.canDisableShutterSound) {
                    assertEquals(newValue, camera.getPlaySounds());
                }
            } else {
                assertEquals(oldValue, camera.getPlaySounds());
            }
        } else {
            assertEquals(newValue, camera.getPlaySounds());
        }
    }

    //endregion

    //region test takeVideo

    @Test(expected = RuntimeException.class)
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testStartVideo_whileInPictureMode() throws Throwable {
        camera.setMode(Mode.PICTURE);
        openSync(true);
        takeVideoSync(false);
        waitForError();
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 25, emulatorOnly = true)
    public void testStartEndVideo() {
        camera.setMode(Mode.VIDEO);
        openSync(true);
        takeVideoSync(true, 4000);
        waitForVideoResult(true);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testStartEndVideoSnapshot() {
        // TODO should check api level for snapshot?
        openSync(true);
        takeVideoSnapshotSync(true, 4000);
        waitForVideoResult(true);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 25, emulatorOnly = true)
    public void testStartEndVideo_withManualStop() {
        camera.setMode(Mode.VIDEO);
        openSync(true);
        takeVideoSync(true);
        uiSync(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        camera.stopVideo();
                    }
                }, 5000);
            }
        });
        waitForVideoResult(true);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testStartEndVideoSnapshot_withManualStop() {
        openSync(true);
        takeVideoSnapshotSync(true);
        uiSync(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        camera.stopVideo();
                    }
                }, 5000);
            }
        });
        waitForVideoResult(true);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testEndVideo_withoutStarting() {
        camera.setMode(Mode.VIDEO);
        openSync(true);
        camera.stopVideo();
        waitForVideoResult(false);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 25, emulatorOnly = true)
    public void testEndVideo_withMaxSize() {
        camera.setMode(Mode.VIDEO);
        camera.setVideoSize(SizeSelectors.maxArea(480 * 360));
        openSync(true);
        // Assuming video frame rate is 20...
        //noinspection ConstantConditions
        camera.setVideoBitRate((int) estimateVideoBitRate(camera.getVideoSize(), 20));
        camera.setVideoMaxSize(estimateVideoBytes(camera.getVideoBitRate(), 5000));
        takeVideoSync(true);
        waitForVideoResult(true);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testEndVideoSnapshot_withMaxSize() {
        openSync(true);
        camera.setSnapshotMaxWidth(480);
        camera.setSnapshotMaxHeight(480);
        // We don't want a very low FPS or the video frames are too sparse and recording
        // can fail (e.g. audio reaching completion while video still struggling to start)
        camera.setPreviewFrameRate(30F);
        //noinspection ConstantConditions
        camera.setVideoBitRate((int) estimateVideoBitRate(camera.getSnapshotSize(),
                (int) camera.getPreviewFrameRate()));
        camera.setVideoMaxSize(estimateVideoBytes(camera.getVideoBitRate(), 5000));
        takeVideoSnapshotSync(true);
        waitForVideoResult(true);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 25, emulatorOnly = true)
    public void testEndVideo_withMaxDuration() {
        if (canSetVideoMaxDuration()) {
            camera.setMode(Mode.VIDEO);
            camera.setVideoMaxDuration(4000);
            openSync(true);
            takeVideoSync(true);
            waitForVideoResult(true);
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testEndVideoSnapshot_withMaxDuration() {
        camera.setVideoMaxDuration(4000);
        openSync(true);
        takeVideoSnapshotSync(true);
        waitForVideoResult(true);
    }

    //endregion

    //region startAutoFocus

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testStartAutoFocus() {
        CameraOptions o = openSync(true);

        final Op<PointF> focus = new Op<>();
        doEndOp(focus, 0).when(listener).onAutoFocusStart(any(PointF.class));

        camera.startAutoFocus(1, 1);
        PointF point = focus.await(300);
        if (o.isAutoFocusSupported()) {
            assertNotNull(point);
            assertEquals(point, new PointF(1, 1));
        } else {
            assertNull(point);
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testStopAutoFocus() {
        CameraOptions o = openSync(true);

        final Op<PointF> focus = new Op<>();
        doEndOp(focus, 1).when(listener).onAutoFocusEnd(anyBoolean(), any(PointF.class));

        camera.startAutoFocus(1, 1);
        // Stop routine can fail, so engines use a timeout. So wait at least the timeout time.
        PointF point = focus.await(1000 + getMeteringTimeoutMillis());
        if (o.isAutoFocusSupported()) {
            assertNotNull(point);
            assertEquals(point, new PointF(1, 1));
        } else {
            assertNull(point);
        }
    }

    protected abstract long getMeteringTimeoutMillis();

    //endregion

    //region capture

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCapturePicture_beforeStarted() {
        camera.takePicture();
        waitForPictureResult(false);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCapturePicture_concurrentCalls() throws Exception {
        // Second take should fail.
        openSync(true);

        CountDownLatch latch = new CountDownLatch(2);
        doCountDown(latch).when(listener).onPictureTaken(any(PictureResult.class));

        camera.takePicture();
        camera.takePicture();
        boolean did = latch.await(4, TimeUnit.SECONDS);
        assertFalse(did);
        assertEquals(1, latch.getCount());
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCapturePicture_size() {
        // Decoding can fail for large bitmaps. set a small size.
        // camera.setPictureSize(SizeSelectors.smallest());
        openSync(true);
        Size size = camera.getPictureSize();
        assertNotNull(size);
        camera.takePicture();
        PictureResult result = waitForPictureResult(true);
        assertNotNull(result);
        assertNotNull(result.getData());
        assertNull(result.getLocation());
        assertFalse(result.isSnapshot());
        assertEquals(result.getSize(), size);
        Bitmap bitmap = CameraUtils.decodeBitmap(result.getData(),
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        if (bitmap != null) {
            assertNotNull(bitmap);
            String message = LOG.i("[PICTURE SIZE]", "Desired:", size, "Bitmap:",
                    new Size(bitmap.getWidth(), bitmap.getHeight()));
            if (!Emulator.isEmulator()) {
                assertEquals(message, bitmap.getWidth(), size.getWidth());
                assertEquals(message, bitmap.getHeight(), size.getHeight());
            } else {
                // Emulator implementation sometimes does not rotate the image correctly.
                assertTrue(message, bitmap.getWidth() == size.getWidth()
                        || bitmap.getWidth() == size.getHeight());
                assertTrue(message, bitmap.getHeight() == size.getWidth()
                        || bitmap.getHeight() == size.getHeight());
                assertEquals(message, size.getWidth() * size.getHeight(),
                        bitmap.getWidth() * bitmap.getHeight());
            }
        }
    }

    @Test(expected = RuntimeException.class)
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCapturePicture_whileInVideoMode() throws Throwable {
        camera.setMode(Mode.VIDEO);
        openSync(true);
        camera.takePicture();
        waitForError();
        camera.takePicture();

    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCapturePicture_withMetering() {
        openSync(true);
        camera.setPictureMetering(true);
        camera.takePicture();
        waitForPictureResult(true);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCapturePicture_withoutMetering() {
        openSync(true);
        camera.setPictureMetering(false);
        camera.takePicture();
        waitForPictureResult(true);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCaptureSnapshot_beforeStarted() {
        camera.takePictureSnapshot();
        waitForPictureResult(false);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCaptureSnapshot_concurrentCalls() throws Exception {
        // Second take should fail.
        openSync(true);

        CountDownLatch latch = new CountDownLatch(2);
        doCountDown(latch).when(listener).onPictureTaken(any(PictureResult.class));

        camera.takePictureSnapshot();
        camera.takePictureSnapshot();
        boolean did = latch.await(6, TimeUnit.SECONDS);
        assertFalse(did);
        assertEquals(1, latch.getCount());
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCaptureSnapshot_size() {
        openSync(true);
        Size size = camera.getSnapshotSize();
        assertNotNull(size);
        camera.takePictureSnapshot();

        PictureResult result = waitForPictureResult(true);
        assertNotNull(result);
        assertNotNull(result.getData());
        assertNull(result.getLocation());
        assertTrue(result.isSnapshot());
        assertEquals(result.getSize(), size);
        Bitmap bitmap = CameraUtils.decodeBitmap(result.getData(),
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        if (bitmap != null) {
            String message = LOG.i("[PICTURE SIZE]", "Desired:", size, "Bitmap:",
                    new Size(bitmap.getWidth(), bitmap.getHeight()));
            assertNotNull(bitmap);
            assertEquals(message, bitmap.getWidth(), size.getWidth());
            assertEquals(message, bitmap.getHeight(), size.getHeight());
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCaptureSnapshot_withMetering() {
        openSync(true);
        camera.setPictureSnapshotMetering(true);
        camera.takePictureSnapshot();
        waitForPictureResult(true);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testCaptureSnapshot_withoutMetering() {
        openSync(true);
        camera.setPictureSnapshotMetering(false);
        camera.takePictureSnapshot();
        waitForPictureResult(true);
    }

    //endregion

    //region Picture Formats

    @SuppressWarnings("ConstantConditions")
    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testPictureFormat_DNG() {
        openSync(true);
        if (camera.getCameraOptions().supports(PictureFormat.DNG)) {
            Op<Boolean> op = new Op<>();
            doEndOp(op, true).when(listener).onCameraOpened(any(CameraOptions.class));
            camera.setPictureFormat(PictureFormat.DNG);
            assertNotNull(op.await(2000));
            camera.takePicture();
            PictureResult result = waitForPictureResult(true);
            // assert that result.getData() is a DNG file:
            // We can use the first 4 bytes assuming they are the same as a TIFF file
            // https://en.wikipedia.org/wiki/List_of_file_signatures 73, 73, 42, 0
            byte[] b = result.getData();
            boolean isII = b[0] == 'I' && b[1] == 'I' && b[2] == '*' && b[3] == 0;
            boolean isMM = b[0] == 'M' && b[1] == 'M' && b[2] == 0 && b[3] == '*';
            assertTrue(isII || isMM);
        }
    }

    //endregion

    //region Frame Processing

    private void assert15Frames(@NonNull FrameProcessor mock) throws Exception {
        // Expect 15 frames. Time is very high because currently Camera2 keeps a very low FPS.
        CountDownLatch latch = new CountDownLatch(15);
        doCountDown(latch).when(mock).process(any(Frame.class));
        boolean did = latch.await(30, TimeUnit.SECONDS);
        assertTrue("Latch count should be 0: " + latch.getCount(), did);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testFrameProcessing_simple() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        openSync(true);

        assert15Frames(processor);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testFrameProcessing_maxSize() {
        final int max = 600;
        camera.setFrameProcessingMaxWidth(max);
        camera.setFrameProcessingMaxHeight(max);
        final Op<Size> sizeOp = new Op<>();
        camera.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                sizeOp.controller().end(frame.getSize());
            }
        });
        openSync(true);
        Size size = sizeOp.await(2000);
        assertNotNull(size);
        assertTrue(size.getWidth() <= max);
        assertTrue(size.getHeight() <= max);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testFrameProcessing_afterSnapshot() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        openSync(true);

        // In Camera1, snapshots will clear the preview callback
        // Ensure we restore correctly
        camera.takePictureSnapshot();
        waitForPictureResult(true);

        assert15Frames(processor);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testFrameProcessing_afterPicture() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        openSync(true);

        camera.takePicture();
        waitForPictureResult(true);

        assert15Frames(processor);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testFrameProcessing_afterRestart() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        openSync(true);
        closeSync(true);
        openSync(true);

        assert15Frames(processor);
    }


    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 25, emulatorOnly = true)
    public void testFrameProcessing_afterVideo() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        camera.setMode(Mode.VIDEO);
        openSync(true);
        takeVideoSync(true,4000);
        waitForVideoResult(true);

        assert15Frames(processor);
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testFrameProcessing_freezeRelease() throws Exception {
        // Ensure that freeze/release cycles do not cause OOMs.
        // There was a bug doing this and it might resurface for any improper
        // disposal of the frames.
        FrameProcessor source = new FreezeReleaseFrameProcessor();
        FrameProcessor processor = spy(source);
        camera.addFrameProcessor(processor);
        openSync(true);

        assert15Frames(processor);
    }

    public class FreezeReleaseFrameProcessor implements FrameProcessor {
        @Override
        public void process(@NonNull Frame frame) {
            frame.freeze().release();
        }
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testFrameProcessing_format() {
        // We wouldn't need to open/close for each format, but we do because legacy devices can
        // crash due to their bad internal implementation when we perform restartBind().
        // And setFrameProcessorFormat can trigger such restart.
        CameraOptions o = openSync(true);
        Collection<Integer> formats = o.getSupportedFrameProcessingFormats();
        closeSync(true);
        for (int format : formats) {
            LOG.i("[TEST FRAME FORMAT]", "Testing", format, "...");
            Op<Boolean> op = testFrameProcessorFormat(format);
            assertNotNull(op.await(DELAY));
            closeSync(true);
        }
    }

    @NonNull
    private Op<Boolean> testFrameProcessorFormat(final int format) {
        final Op<Boolean> op = new Op<>();
        camera.setFrameProcessingFormat(format);
        camera.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                if (frame.getFormat() == format) {
                    op.controller().start();
                    op.controller().end(true);
                }
            }
        });
        openSync(true);
        return op;
    }

    //endregion

    //region Overlays

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testOverlay_forPictureSnapshot() {
        Overlay overlay = mock(Overlay.class);
        when(overlay.drawsOn(any(Overlay.Target.class))).thenReturn(true);
        controller.setOverlay(overlay);
        openSync(true);
        camera.takePictureSnapshot();
        waitForPictureResult(true);
        verify(overlay, atLeastOnce()).drawsOn(Overlay.Target.PICTURE_SNAPSHOT);
        verify(overlay, times(1)).drawOn(eq(Overlay.Target.PICTURE_SNAPSHOT), any(Canvas.class));
    }

    @Test
    @Retry(emulatorOnly = true)
    @SdkExclude(maxSdkVersion = 22, emulatorOnly = true)
    public void testOverlay_forVideoSnapshot() {
        Overlay overlay = mock(Overlay.class);
        when(overlay.drawsOn(any(Overlay.Target.class))).thenReturn(true);
        controller.setOverlay(overlay);
        openSync(true);
        takeVideoSnapshotSync(true, 4000);
        waitForVideoResult(true);
        verify(overlay, atLeastOnce()).drawsOn(Overlay.Target.VIDEO_SNAPSHOT);
        verify(overlay, atLeastOnce()).drawOn(eq(Overlay.Target.VIDEO_SNAPSHOT), any(Canvas.class));
    }

    //endregion

    @SuppressWarnings("SameParameterValue")
    private static long estimateVideoBitRate(@NonNull Size size, int frameRate) {
        // Nasty estimate for a LQ video
        return Math.round(0.05D * size.getWidth() * size.getHeight() * frameRate);
    }

    @SuppressWarnings("SameParameterValue")
    private static long estimateVideoBytes(long videoBitRate, long millis) {
        // 1.3F accounts for audio.
        return Math.round((videoBitRate * 1.3F) * (millis / 1000D) / 8D);
    }
}

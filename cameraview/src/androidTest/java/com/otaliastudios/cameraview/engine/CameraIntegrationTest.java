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
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.internal.utils.Op;
import com.otaliastudios.cameraview.internal.utils.WorkerHandler;
import com.otaliastudios.cameraview.overlay.Overlay;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.File;
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

public abstract class CameraIntegrationTest extends BaseTest {

    private final static CameraLogger LOG = CameraLogger.create(CameraIntegrationTest.class.getSimpleName());
    private final static long DELAY = 8000;

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    private CameraView camera;
    protected CameraEngine controller;
    private CameraListener listener;
    private Op<Throwable> uiExceptionOp;

    @BeforeClass
    public static void grant() {
        grantAllPermissions();
    }

    @NonNull
    protected abstract Engine getEngine();

    @Before
    public void setUp() {
        LOG.e("Test started. Setting up camera.");
        WorkerHandler.destroyAll();

        uiSync(new Runnable() {
            @Override
            public void run() {
                camera = new CameraView(rule.getActivity()) {

                    @NonNull
                    @Override
                    protected CameraEngine instantiateCameraEngine(@NonNull Engine engine, @NonNull CameraEngine.Callback callback) {
                        controller = super.instantiateCameraEngine(getEngine(), callback);
                        return controller;
                    }
                };
                listener = mock(CameraListener.class);
                camera.setExperimental(true);
                camera.setEngine(getEngine());
                camera.addCameraListener(listener);
                rule.getActivity().inflate(camera);

                // Ensure that controller exceptions are thrown on this thread (not on the UI thread).
                // TODO this makes debugging for wrong tests very hard, as we don't get the exception
                // unless waitForUiException() is called.
                uiExceptionOp = new Op<>(true);
                WorkerHandler crashThread = WorkerHandler.get("CrashThread");
                crashThread.getThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        uiExceptionOp.end(e);
                    }
                });
                controller.mCrashHandler = crashThread.getHandler();
            }
        });
    }

    @After
    public void tearDown() {
        LOG.e("Test ended. Tearing down camera.");
        camera.destroy();
        WorkerHandler.destroyAll();
    }

    private void waitForUiException() throws Throwable {
        Throwable throwable = uiExceptionOp.await(DELAY);
        if (throwable != null) {
            throw throwable;
        }
    }

    private CameraOptions openSync(boolean expectSuccess) {
        camera.open();
        final Op<CameraOptions> open = new Op<>(true);
        doEndOp(open, 0).when(listener).onCameraOpened(any(CameraOptions.class));
        CameraOptions result = open.await(DELAY);
        if (expectSuccess) {
            assertNotNull("Can open", result);
            onOpenSync();
        } else {
            assertNull("Should not open", result);
        }
        return result;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    protected void onOpenSync() {
        // Extra wait for the bind and preview state, so we run tests in a fully operational
        // state. If we didn't do so, we could have null values, for example, in getPictureSize
        // or in getSnapshotSize.
        while (controller.getBindState() != CameraEngine.STATE_STARTED) {}
        while (controller.getPreviewState() != CameraEngine.STATE_STARTED) {}
    }

    private void closeSync(boolean expectSuccess) {
        camera.close();
        final Op<Boolean> close = new Op<>(true);
        doEndOp(close, true).when(listener).onCameraClosed();
        Boolean result = close.await(DELAY);
        if (expectSuccess) {
            assertNotNull("Can close", result);
        } else {
            assertNull("Should not close", result);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    @Nullable
    private VideoResult waitForVideoResult(boolean expectSuccess) {
        // CountDownLatch for onVideoRecordingEnd.
        CountDownLatch onVideoRecordingEnd = new CountDownLatch(1);
        doCountDown(onVideoRecordingEnd).when(listener).onVideoRecordingEnd();

        // Op for onVideoTaken.
        final Op<VideoResult> video = new Op<>(true);
        doEndOp(video, 0).when(listener).onVideoTaken(any(VideoResult.class));
        doEndOp(video, null).when(listener).onCameraError(argThat(new ArgumentMatcher<CameraException>() {
            @Override
            public boolean matches(CameraException argument) {
                return argument.getReason() == CameraException.REASON_VIDEO_FAILED;
            }
        }));

        // Wait for onVideoTaken and check.
        VideoResult result = video.await(DELAY);

        // It seems that when running all the tests together, the device can go in some
        // power saving mode which makes the CPU extremely slow. This is especially problematic
        // with video snapshots where we do lots of processing. The videoEnd callback can return
        // long after the actual stop() call, so if we're still processing, let's wait more.
        if (expectSuccess && camera.isTakingVideo()) {
            while (camera.isTakingVideo()) {
                video.listen();
                result = video.await(DELAY);
            }
            // Sleep another 1000, because camera.isTakingVideo() might return false even
            // if the result still has to be dispatched. Rare but could happen.
            try { Thread.sleep(1000); } catch (InterruptedException ignore) {}
        }

        // Now we should be OK.
        if (expectSuccess) {
            assertEquals("Should call onVideoRecordingEnd", 0, onVideoRecordingEnd.getCount());
            assertNotNull("Should end video", result);
        } else {
            assertNull("Should not end video", result);
        }
        return result;
    }

    @Nullable
    private PictureResult waitForPictureResult(boolean expectSuccess) {
        final Op<PictureResult> pic = new Op<>(true);
        doEndOp(pic, 0).when(listener).onPictureTaken(any(PictureResult.class));
        doEndOp(pic, null).when(listener).onCameraError(argThat(new ArgumentMatcher<CameraException>() {
            @Override
            public boolean matches(CameraException argument) {
                return argument.getReason() == CameraException.REASON_PICTURE_FAILED;
            }
        }));
        PictureResult result = pic.await(DELAY);
        if (expectSuccess) {
            assertNotNull("Can take picture", result);
        } else {
            assertNull("Should not take picture", result);
        }
        return result;
    }

    private void takeVideoSync(boolean expectSuccess) {
        takeVideoSync(expectSuccess,0);
    }

    private void takeVideoSync(boolean expectSuccess, int duration) {
        final Op<Boolean> op = new Op<>(true);
        doEndOp(op, true).when(listener).onVideoRecordingStart();
        doEndOp(op, false).when(listener).onCameraError(argThat(new ArgumentMatcher<CameraException>() {
            @Override
            public boolean matches(CameraException argument) {
                return argument.getReason() == CameraException.REASON_VIDEO_FAILED;
            }
        }));
        File file = new File(getContext().getFilesDir(), "video.mp4");
        if (duration > 0) {
            camera.takeVideo(file, duration);
        } else {
            camera.takeVideo(file);
        }
        Boolean result = op.await(DELAY);
        if (expectSuccess) {
            assertNotNull("should start video recording or get CameraError", result);
            assertTrue("should start video recording successfully", result);
        } else {
            assertTrue("should not start video recording", result == null || !result);
        }
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    private void takeVideoSnapshotSync(boolean expectSuccess) {
        takeVideoSnapshotSync(expectSuccess,0);
    }

    private void takeVideoSnapshotSync(boolean expectSuccess, int duration) {
        final Op<Boolean> op = new Op<>(true);
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
            assertNotNull("should start video recording or get CameraError", result);
            assertTrue("should start video recording successfully", result);
        } else {
            assertTrue("should not start video recording", result == null || !result);
        }
    }

    //region test open/close

    @Test
    public void testOpenClose() {
        // Starting and stopping are hard to get since they happen on another thread.
        assertEquals(controller.getEngineState(), CameraEngine.STATE_STOPPED);

        openSync(true);
        assertEquals(controller.getEngineState(), CameraEngine.STATE_STARTED);

        closeSync(true);
        assertEquals(controller.getEngineState(), CameraEngine.STATE_STOPPED);
    }

    @Test
    public void testOpenTwice() {
        openSync(true);
        openSync(false);
    }

    @Test
    public void testCloseTwice() {
        closeSync(false);
    }

    @Test
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
    public void testStartInitializesOptions() {
        assertNull(camera.getCameraOptions());
        openSync(true);
        assertNotNull(camera.getCameraOptions());
    }

    //endregion

    //region test Facing/SessionType
    // Test things that should reset the camera.

    @Test
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
    public void testSetZoom() {
        CameraOptions options = openSync(true);

        controller.mZoomOp.listen();
        float oldValue = camera.getZoom();
        float newValue = 0.65f;
        camera.setZoom(newValue);
        controller.mZoomOp.await(500);

        if (options.isZoomSupported()) {
            assertEquals(newValue, camera.getZoom(), 0f);
        } else {
            assertEquals(oldValue, camera.getZoom(), 0f);
        }
    }

    @Test
    public void testSetExposureCorrection() {
        CameraOptions options = openSync(true);

        controller.mExposureCorrectionOp.listen();
        float oldValue = camera.getExposureCorrection();
        float newValue = options.getExposureCorrectionMaxValue();
        camera.setExposureCorrection(newValue);
        controller.mExposureCorrectionOp.await(300);

        if (options.isExposureCorrectionSupported()) {
            assertEquals(newValue, camera.getExposureCorrection(), 0f);
        } else {
            assertEquals(oldValue, camera.getExposureCorrection(), 0f);
        }
    }

    @Test
    public void testSetFlash() {
        CameraOptions options = openSync(true);
        Flash[] values = Flash.values();
        Flash oldValue = camera.getFlash();
        for (Flash value : values) {
            controller.mFlashOp.listen();
            camera.setFlash(value);
            controller.mFlashOp.await(300);
            if (options.supports(value)) {
                assertEquals(camera.getFlash(), value);
                oldValue = value;
            } else {
                assertEquals(camera.getFlash(), oldValue);
            }
        }
    }

    @Test
    public void testSetWhiteBalance() {
        CameraOptions options = openSync(true);
        WhiteBalance[] values = WhiteBalance.values();
        WhiteBalance oldValue = camera.getWhiteBalance();
        for (WhiteBalance value : values) {
            controller.mWhiteBalanceOp.listen();
            camera.setWhiteBalance(value);
            controller.mWhiteBalanceOp.await(300);
            if (options.supports(value)) {
                assertEquals(camera.getWhiteBalance(), value);
                oldValue = value;
            } else {
                assertEquals(camera.getWhiteBalance(), oldValue);
            }
        }
    }

    @Test
    public void testSetHdr() {
        CameraOptions options = openSync(true);
        Hdr[] values = Hdr.values();
        Hdr oldValue = camera.getHdr();
        for (Hdr value : values) {
            controller.mHdrOp.listen();
            camera.setHdr(value);
            controller.mHdrOp.await(300);
            if (options.supports(value)) {
                assertEquals(camera.getHdr(), value);
                oldValue = value;
            } else {
                assertEquals(camera.getHdr(), oldValue);
            }
        }
    }

    @Test
    public void testSetAudio() {
        openSync(true);
        Audio[] values = Audio.values();
        for (Audio value : values) {
            camera.setAudio(value);
            assertEquals(camera.getAudio(), value);
        }
    }

    @Test
    public void testSetLocation() {
        openSync(true);
        controller.mLocationOp.listen();
        camera.setLocation(10d, 2d);
        controller.mLocationOp.await(300);
        assertNotNull(camera.getLocation());
        assertEquals(camera.getLocation().getLatitude(), 10d, 0d);
        assertEquals(camera.getLocation().getLongitude(), 2d, 0d);
        // This also ensures there are no crashes when attaching it to camera parameters.
    }

    @Test
    public void testSetPreviewFrameRate() {
        openSync(true);
        controller.mPreviewFrameRateOp.listen();
        camera.setPreviewFrameRate(30);
        controller.mPreviewFrameRateOp.await(300);
        assertEquals(camera.getPreviewFrameRate(), 30, 0);
    }

    @Test
    public void testSetPlaySounds() {
        controller.mPlaySoundsOp.listen();
        boolean oldValue = camera.getPlaySounds();
        boolean newValue = !oldValue;
        camera.setPlaySounds(newValue);
        controller.mPlaySoundsOp.await(300);

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
    public void testStartVideo_whileInPictureMode() throws Throwable {
        // Fails on Travis. Some emulators can't deal with MediaRecorder
        // Error while starting MediaRecorder. java.lang.RuntimeException: start failed.
        // as documented. This works locally though.
        camera.setMode(Mode.PICTURE);
        openSync(true);
        takeVideoSync(false);
        waitForUiException();
    }

    @Test
    public void testStartEndVideo() {
        camera.setMode(Mode.VIDEO);
        openSync(true);
        takeVideoSync(true, 4000);
        waitForVideoResult(true);
    }

    @Test
    public void testStartEndVideoSnapshot() {
        // TODO should check api level for snapshot?
        openSync(true);
        takeVideoSnapshotSync(true, 4000);
        waitForVideoResult(true);
    }

    @Test
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
    public void testEndVideo_withoutStarting() {
        camera.setMode(Mode.VIDEO);
        openSync(true);
        camera.stopVideo();
        waitForVideoResult(false);
    }

    @Test
    public void testEndVideo_withMaxSize() {
        camera.setMode(Mode.VIDEO);
        camera.setVideoMaxSize(3000*1000); // Less is risky
        openSync(true);
        takeVideoSync(true);
        waitForVideoResult(true);
    }

    @Test
    public void testEndVideoSnapshot_withMaxSize() {
        camera.setVideoMaxSize(3000*1000);
        openSync(true);
        takeVideoSnapshotSync(true);
        waitForVideoResult(true);
    }

    @Test
    public void testEndVideo_withMaxDuration() {
        camera.setMode(Mode.VIDEO);
        camera.setVideoMaxDuration(4000);
        openSync(true);
        takeVideoSync(true);
        waitForVideoResult(true);
    }

    @Test
    public void testEndVideoSnapshot_withMaxDuration() {
        camera.setVideoMaxDuration(4000);
        openSync(true);
        takeVideoSnapshotSync(true);
        waitForVideoResult(true);
    }

    //endregion

    //region startAutoFocus

    @Test
    public void testStartAutoFocus() {
        CameraOptions o = openSync(true);

        final Op<PointF> focus = new Op<>(true);
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
    public void testStopAutoFocus() {
        CameraOptions o = openSync(true);

        final Op<PointF> focus = new Op<>(true);
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
    public void testCapturePicture_beforeStarted() {
        camera.takePicture();
        waitForPictureResult(false);
    }

    @Test
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
    public void testCapturePicture_size() {
        openSync(true);
        Size size = camera.getPictureSize();
        assertNotNull(size);
        camera.takePicture();
        PictureResult result = waitForPictureResult(true);
        assertNotNull(result);
        Bitmap bitmap = CameraUtils.decodeBitmap(result.getData(), Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertNotNull(bitmap);
        assertEquals(result.getSize(), size);
        assertEquals(bitmap.getWidth(), size.getWidth());
        assertEquals(bitmap.getHeight(), size.getHeight());
        assertNotNull(result.getData());
        assertNull(result.getLocation());
        assertFalse(result.isSnapshot());
    }

    @Test(expected = RuntimeException.class)
    public void testCapturePicture_whileInVideoMode() throws Throwable {
        camera.setMode(Mode.VIDEO);
        openSync(true);
        camera.takePicture();
        waitForUiException();
        camera.takePicture();

    }

    @Test
    public void testCapturePicture_withMetering() {
        openSync(true);
        camera.setPictureMetering(true);
        camera.takePicture();
        waitForPictureResult(true);
    }

    @Test
    public void testCapturePicture_withoutMetering() {
        openSync(true);
        camera.setPictureMetering(false);
        camera.takePicture();
        waitForPictureResult(true);
    }

    @Test
    public void testCaptureSnapshot_beforeStarted() {
        camera.takePictureSnapshot();
        waitForPictureResult(false);
    }

    @Test
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
    public void testCaptureSnapshot_size() {
        openSync(true);
        Size size = camera.getSnapshotSize();
        assertNotNull(size);
        camera.takePictureSnapshot();

        PictureResult result = waitForPictureResult(true);
        assertNotNull(result);
        Bitmap bitmap = CameraUtils.decodeBitmap(result.getData(), Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertNotNull(bitmap);
        assertEquals(result.getSize(), size);
        assertEquals(bitmap.getWidth(), size.getWidth());
        assertEquals(bitmap.getHeight(), size.getHeight());
        assertNotNull(result.getData());
        assertNull(result.getLocation());
        assertTrue(result.isSnapshot());
    }

    @Test
    public void testCaptureSnapshot_withMetering() {
        openSync(true);
        camera.setPictureSnapshotMetering(true);
        camera.takePictureSnapshot();
        waitForPictureResult(true);
    }

    @Test
    public void testCaptureSnapshot_withoutMetering() {
        openSync(true);
        camera.setPictureSnapshotMetering(false);
        camera.takePictureSnapshot();
        waitForPictureResult(true);
    }

    //endregion

    //region Frame Processing

    private void assert30Frames(FrameProcessor mock) throws Exception {
        // Expect 30 frames
        CountDownLatch latch = new CountDownLatch(30);
        doCountDown(latch).when(mock).process(any(Frame.class));
        boolean did = latch.await(15, TimeUnit.SECONDS);
        assertTrue("Latch count should be 0: " + latch.getCount(), did);
    }

    @Test
    public void testFrameProcessing_simple() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        openSync(true);

        assert30Frames(processor);
    }

    @Test
    public void testFrameProcessing_afterSnapshot() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        openSync(true);

        // In Camera1, snapshots will clear the preview callback
        // Ensure we restore correctly
        camera.takePictureSnapshot();
        waitForPictureResult(true);

        assert30Frames(processor);
    }

    @Test
    public void testFrameProcessing_afterRestart() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        openSync(true);
        closeSync(true);
        openSync(true);

        assert30Frames(processor);
    }


    @Test
    public void testFrameProcessing_afterVideo() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        camera.setMode(Mode.VIDEO);
        openSync(true);
        takeVideoSync(true,4000);
        waitForVideoResult(true);

        assert30Frames(processor);
    }

    @Test
    public void testFrameProcessing_freezeRelease() throws Exception {
        // Ensure that freeze/release cycles do not cause OOMs.
        // There was a bug doing this and it might resurface for any improper
        // disposal of the frames.
        FrameProcessor source = new FreezeReleaseFrameProcessor();
        FrameProcessor processor = spy(source);
        camera.addFrameProcessor(processor);
        openSync(true);

        assert30Frames(processor);
    }

    public class FreezeReleaseFrameProcessor implements FrameProcessor {
        @Override
        public void process(@NonNull Frame frame) {
            frame.freeze().release();
        }
    }

    //endregion

    //region Overlays

    @Test
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
}

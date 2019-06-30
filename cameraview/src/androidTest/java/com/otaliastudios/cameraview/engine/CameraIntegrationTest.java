package com.otaliastudios.cameraview.engine;


import android.graphics.Bitmap;
import android.graphics.PointF;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.CameraListener;
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
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;


public abstract class CameraIntegrationTest extends BaseTest {

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    private CameraView camera;
    private CameraEngine controller;
    private CameraListener listener;
    private Op<Throwable> uiExceptionOp;

    @BeforeClass
    public static void grant() {
        grantPermissions();
    }

    @NonNull
    protected abstract Engine getEngine();

    @Before
    public void setUp() {
        WorkerHandler.destroy();

        ui(new Runnable() {
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
                camera.addCameraListener(listener);
                rule.getActivity().inflate(camera);

                // Ensure that controller exceptions are thrown on this thread (not on the UI thread).
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
        camera.stopVideo();
        camera.destroy();
        WorkerHandler.destroy();
    }

    private void waitForUiException() throws Throwable {
        Throwable throwable = uiExceptionOp.await(2500);
        if (throwable != null) throw throwable;
    }

    private CameraOptions waitForOpen(boolean expectSuccess) {
        camera.open();
        final Op<CameraOptions> open = new Op<>(true);
        doEndTask(open, 0).when(listener).onCameraOpened(any(CameraOptions.class));
        CameraOptions result = open.await(4000);
        if (expectSuccess) {
            assertNotNull("Can open", result);
        } else {
            assertNull("Should not open", result);
        }
        return result;
    }

    private void waitForClose(boolean expectSuccess) {
        camera.close();
        final Op<Boolean> close = new Op<>(true);
        doEndTask(close, true).when(listener).onCameraClosed();
        Boolean result = close.await(4000);
        if (expectSuccess) {
            assertNotNull("Can close", result);
        } else {
            assertNull("Should not close", result);
        }
    }

    private void waitForVideoEnd(boolean expectSuccess) {
        final Op<Boolean> video = new Op<>(true);
        doEndTask(video, true).when(listener).onVideoTaken(any(VideoResult.class));
        Boolean result = video.await(8000);
        if (expectSuccess) {
            assertNotNull("Should end video", result);
        } else {
            assertNull("Should not end video", result);
        }
    }

    private PictureResult waitForPicture(boolean expectSuccess) {
        final Op<PictureResult> pic = new Op<>(true);
        doEndTask(pic, 0).when(listener).onPictureTaken(any(PictureResult.class));
        PictureResult result = pic.await(5000);
        if (expectSuccess) {
            assertNotNull("Can take picture", result);
        } else {
            assertNull("Should not take picture", result);
        }
        return result;
    }

    private void waitForVideoStart() {
        controller.mStartVideoOp.listen();
        File file = new File(context().getFilesDir(), "video.mp4");
        camera.takeVideo(file);
        controller.mStartVideoOp.await(400);
    }

    //region test open/close

    @Test
    public void testOpenClose() {
        // Starting and stopping are hard to get since they happen on another thread.
        assertEquals(controller.getEngineState(), CameraEngine.STATE_STOPPED);

        waitForOpen(true);
        assertEquals(controller.getEngineState(), CameraEngine.STATE_STARTED);

        waitForClose(true);
        assertEquals(controller.getEngineState(), CameraEngine.STATE_STOPPED);
    }

    @Test
    public void testOpenTwice() {
        waitForOpen(true);
        waitForOpen(false);
    }

    @Test
    public void testCloseTwice() {
        waitForClose(false);
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
        waitForOpen(true);
        assertNotNull(camera.getCameraOptions());
    }

    //endregion

    //region test Facing/SessionType
    // Test things that should reset the camera.

    @Test
    public void testSetFacing() throws Exception {
        CameraOptions o = waitForOpen(true);
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
        waitForOpen(true);

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
        CameraOptions options = waitForOpen(true);

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
        CameraOptions options = waitForOpen(true);

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
        CameraOptions options = waitForOpen(true);
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
        CameraOptions options = waitForOpen(true);
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
        CameraOptions options = waitForOpen(true);
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
        // TODO: when permissions are managed, check that Audio.ON triggers the audio permission
        waitForOpen(true);
        Audio[] values = Audio.values();
        for (Audio value : values) {
            camera.setAudio(value);
            assertEquals(camera.getAudio(), value);
        }
    }

    @Test
    public void testSetLocation() {
        waitForOpen(true);
        controller.mLocationOp.listen();
        camera.setLocation(10d, 2d);
        controller.mLocationOp.await(300);
        assertNotNull(camera.getLocation());
        assertEquals(camera.getLocation().getLatitude(), 10d, 0d);
        assertEquals(camera.getLocation().getLongitude(), 2d, 0d);
        // This also ensures there are no crashes when attaching it to camera parameters.
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
            // TODO do when Camera2 is completed
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
        waitForOpen(true);
        waitForVideoStart();
        waitForUiException();
    }

    @Test
    public void testStartEndVideo() {
        // Fails on Travis. Some emulators can't deal with MediaRecorder,
        // Error while starting MediaRecorder. java.lang.RuntimeException: start failed.
        // as documented. This works locally though.
        camera.setMode(Mode.VIDEO);
        waitForOpen(true);
        camera.takeVideo(new File(context().getFilesDir(), "video.mp4"), 4000);
        waitForVideoEnd(true);
    }

    @Test
    public void testEndVideo_withoutStarting() {
        camera.setMode(Mode.VIDEO);
        waitForOpen(true);
        camera.stopVideo();
        waitForVideoEnd(false);
    }

    @Test
    public void testEndVideo_withMaxSize() {
        camera.setMode(Mode.VIDEO);
        camera.setVideoMaxSize(3000*1000); // Less is risky
        waitForOpen(true);
        waitForVideoStart();
        waitForVideoEnd(true);
    }

    @Test
    public void testEndVideo_withMaxDuration() {
        camera.setMode(Mode.VIDEO);
        camera.setVideoMaxDuration(4000);
        waitForOpen(true);
        waitForVideoStart();
        waitForVideoEnd(true);
    }

    //endregion

    //region startAutoFocus

    @Test
    public void testStartAutoFocus() {
        CameraOptions o = waitForOpen(true);

        final Op<PointF> focus = new Op<>(true);
        doEndTask(focus, 0).when(listener).onAutoFocusStart(any(PointF.class));

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
        CameraOptions o = waitForOpen(true);

        final Op<PointF> focus = new Op<>(true);
        doEndTask(focus, 1).when(listener).onAutoFocusEnd(anyBoolean(), any(PointF.class));

        camera.startAutoFocus(1, 1);
        // Stop is not guaranteed to be called, we use a delay. So wait at least the delay time.
        PointF point = focus.await(1000 + Camera1Engine.AUTOFOCUS_END_DELAY_MILLIS);
        if (o.isAutoFocusSupported()) {
            assertNotNull(point);
            assertEquals(point, new PointF(1, 1));
        } else {
            assertNull(point);
        }
    }

    //endregion

    //region capture

    @Test
    public void testCapturePicture_beforeStarted() {
        camera.takePicture();
        waitForPicture(false);
    }

    @Test
    public void testCapturePicture_concurrentCalls() throws Exception {
        // Second take should fail.
        waitForOpen(true);

        CountDownLatch latch = new CountDownLatch(2);
        doCountDown(latch).when(listener).onPictureTaken(any(PictureResult.class));

        camera.takePicture();
        camera.takePicture();
        boolean did = latch.await(4, TimeUnit.SECONDS);
        assertFalse(did);
        assertEquals(latch.getCount(), 1);
    }

    @Test
    public void testCapturePicture_size() throws Exception {
        waitForOpen(true);
        // PictureSize can still be null after opened.
        while (camera.getPictureSize() == null) {}
        Size size = camera.getPictureSize();
        camera.takePicture();
        PictureResult result = waitForPicture(true);
        Bitmap bitmap = CameraUtils.decodeBitmap(result.getData(), Integer.MAX_VALUE, Integer.MAX_VALUE);
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
        waitForOpen(true);
        camera.takePicture();
        waitForUiException();
    }

    @Test
    public void testCaptureSnapshot_beforeStarted() {
        camera.takePictureSnapshot();
        waitForPicture(false);
    }

    @Test
    public void testCaptureSnapshot_concurrentCalls() throws Exception {
        // Second take should fail.
        waitForOpen(true);

        CountDownLatch latch = new CountDownLatch(2);
        doCountDown(latch).when(listener).onPictureTaken(any(PictureResult.class));

        camera.takePictureSnapshot();
        camera.takePictureSnapshot();
        boolean did = latch.await(6, TimeUnit.SECONDS);
        assertFalse(did);
        assertEquals(1, latch.getCount());
    }

    @Test
    public void testCaptureSnapshot_size() throws Exception {
        waitForOpen(true);
        // SnapshotSize can still be null after opened.
        while (camera.getSnapshotSize() == null) {}
        Size size = camera.getSnapshotSize();
        camera.takePictureSnapshot();

        PictureResult result = waitForPicture(true);
        Bitmap bitmap = CameraUtils.decodeBitmap(result.getData(), Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(result.getSize(), size);
        assertEquals(bitmap.getWidth(), size.getWidth());
        assertEquals(bitmap.getHeight(), size.getHeight());
        assertNotNull(result.getData());
        assertNull(result.getLocation());
        assertTrue(result.isSnapshot());
    }

    //endregion

    //region Frame Processing

    private void assert30Frames(FrameProcessor mock) throws Exception {
        // Expect 30 frames
        CountDownLatch latch = new CountDownLatch(30);
        doCountDown(latch).when(mock).process(any(Frame.class));
        boolean did = latch.await(4, TimeUnit.SECONDS);
        assertTrue(did);
    }

    @Test
    public void testFrameProcessing_simple() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        waitForOpen(true);

        assert30Frames(processor);
    }

    @Test
    public void testFrameProcessing_afterSnapshot() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        waitForOpen(true);

        // In Camera1, snapshots will clear the preview callback
        // Ensure we restore correctly
        camera.takePictureSnapshot();
        waitForPicture(true);

        assert30Frames(processor);
    }

    @Test
    public void testFrameProcessing_afterRestart() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        waitForOpen(true);
        waitForClose(true);
        waitForOpen(true);

        assert30Frames(processor);
    }


    @Test
    public void testFrameProcessing_afterVideo() throws Exception {
        FrameProcessor processor = mock(FrameProcessor.class);
        camera.addFrameProcessor(processor);
        camera.setMode(Mode.VIDEO);
        waitForOpen(true);
        camera.takeVideo(new File(context().getFilesDir(), "video.mp4"), 4000);
        waitForVideoEnd(true);

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
        waitForOpen(true);

        assert30Frames(processor);
    }

    public class FreezeReleaseFrameProcessor implements FrameProcessor {
        @Override
        public void process(@NonNull Frame frame) {
            frame.freeze().release();
        }
    }

    //endregion
}

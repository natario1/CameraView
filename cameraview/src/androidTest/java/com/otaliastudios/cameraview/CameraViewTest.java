package com.otaliastudios.cameraview;


import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import android.view.MotionEvent;
import android.view.ViewGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import static android.view.View.MeasureSpec.*;
import static android.view.ViewGroup.LayoutParams.*;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class CameraViewTest extends BaseTest {

    private CameraView cameraView;
    private MockCameraController mockController;
    private CameraPreview mockPreview;
    private boolean hasPermissions;

    @Before
    public void setUp() {
        ui(new Runnable() {
            @Override
            public void run() {
                Context context = context();
                cameraView = new CameraView(context) {
                    @Override
                    protected CameraController instantiateCameraController(CameraCallbacks callbacks) {
                        mockController = spy(new MockCameraController(callbacks));
                        return mockController;
                    }

                    @Override
                    protected CameraPreview instantiatePreview(Context context, ViewGroup container) {
                        mockPreview = spy(new MockCameraPreview(context, container));
                        return mockPreview;
                    }

                    @Override
                    protected boolean checkPermissions(@NonNull Audio audio) {
                        return hasPermissions;
                    }
                };
                // Instantiate preview now.
                cameraView.instantiatePreview();
            }
        });
    }

    @After
    public void tearDown() {
        cameraView = null;
        mockController = null;
        mockPreview = null;
        hasPermissions = false;
    }

    //region testLifecycle

    @Test
    public void testOpen() {
        cameraView.open();
        verify(mockPreview, times(1)).onResume();
        // Can't verify controller, depends on permissions.
        // See to-do at the end.
    }

    @Test
    public void testClose() {
        cameraView.close();
        verify(mockPreview, times(1)).onPause();
        verify(mockController, times(1)).stop();
    }

    @Test
    public void testDestroy() {
        cameraView.destroy();
        verify(mockPreview, times(1)).onDestroy();
        verify(mockController, times(1)).destroy();
    }

    //region testDefaults

    @Test
    public void testNullBeforeStart() {
        assertFalse(cameraView.isOpened());
        assertNull(cameraView.getCameraOptions());
        assertNull(cameraView.getSnapshotSize());
        assertNull(cameraView.getPictureSize());
        assertNull(cameraView.getVideoSize());
    }

    @Test
    public void testDefaults() {
        // CameraController
        assertEquals(cameraView.getFlash(), Flash.DEFAULT);
        assertEquals(cameraView.getFacing(), Facing.DEFAULT(context()));
        assertEquals(cameraView.getGrid(), Grid.DEFAULT);
        assertEquals(cameraView.getWhiteBalance(), WhiteBalance.DEFAULT);
        assertEquals(cameraView.getMode(), Mode.DEFAULT);
        assertEquals(cameraView.getHdr(), Hdr.DEFAULT);
        assertEquals(cameraView.getAudio(), Audio.DEFAULT);
        assertEquals(cameraView.getVideoCodec(), VideoCodec.DEFAULT);
        assertEquals(cameraView.getLocation(), null);
        assertEquals(cameraView.getExposureCorrection(), 0f, 0f);
        assertEquals(cameraView.getZoom(), 0f, 0f);
        assertEquals(cameraView.getVideoMaxDuration(), 0, 0);
        assertEquals(cameraView.getVideoMaxSize(), 0, 0);

        // Self managed
        assertEquals(cameraView.getPlaySounds(), CameraView.DEFAULT_PLAY_SOUNDS);
        assertEquals(cameraView.getGestureAction(Gesture.TAP), GestureAction.DEFAULT_TAP);
        assertEquals(cameraView.getGestureAction(Gesture.LONG_TAP), GestureAction.DEFAULT_LONG_TAP);
        assertEquals(cameraView.getGestureAction(Gesture.PINCH), GestureAction.DEFAULT_PINCH);
        assertEquals(cameraView.getGestureAction(Gesture.SCROLL_HORIZONTAL), GestureAction.DEFAULT_SCROLL_HORIZONTAL);
        assertEquals(cameraView.getGestureAction(Gesture.SCROLL_VERTICAL), GestureAction.DEFAULT_SCROLL_VERTICAL);
    }

    //endregion

    //region testGesture

    @Test
    public void testGesture_mapAndClear() {
        // Assignable
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
        assertEquals(cameraView.getGestureAction(Gesture.PINCH), GestureAction.ZOOM);

        // Not assignable: This is like clearing
        cameraView.mapGesture(Gesture.PINCH, GestureAction.CAPTURE);
        assertEquals(cameraView.getGestureAction(Gesture.PINCH), GestureAction.NONE);

        // Test clearing
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
        cameraView.clearGesture(Gesture.PINCH);
        assertEquals(cameraView.getGestureAction(Gesture.PINCH), GestureAction.NONE);
    }

    @Test
    public void testGesture_enablingDisablingLayouts() {
        cameraView.clearGesture(Gesture.TAP);
        cameraView.clearGesture(Gesture.LONG_TAP);
        cameraView.clearGesture(Gesture.PINCH);
        cameraView.clearGesture(Gesture.SCROLL_HORIZONTAL);
        cameraView.clearGesture(Gesture.SCROLL_VERTICAL);

        // PinchGestureLayout
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
        assertTrue(cameraView.mPinchGestureLayout.enabled());
        cameraView.clearGesture(Gesture.PINCH);
        assertFalse(cameraView.mPinchGestureLayout.enabled());

        // TapGestureLayout
        cameraView.mapGesture(Gesture.TAP, GestureAction.CAPTURE);
        assertTrue(cameraView.mTapGestureLayout.enabled());
        cameraView.clearGesture(Gesture.TAP);
        assertFalse(cameraView.mPinchGestureLayout.enabled());

        // ScrollGestureLayout
        cameraView.mapGesture(Gesture.SCROLL_HORIZONTAL, GestureAction.ZOOM);
        assertTrue(cameraView.mScrollGestureLayout.enabled());
        cameraView.clearGesture(Gesture.SCROLL_HORIZONTAL);
        assertFalse(cameraView.mScrollGestureLayout.enabled());
    }

    //endregion

    //region testGestureAction

    @Test
    public void testGestureAction_capture() {
        mockController.mockStarted(true);
        MotionEvent event = MotionEvent.obtain(0L, 0L, 0, 0f, 0f, 0);
        ui(new Runnable() {
            @Override
            public void run() {
                cameraView.mTapGestureLayout = new TapGestureLayout(cameraView.getContext()) {
                    public boolean onTouchEvent(MotionEvent event) { return true; }

                };
                cameraView.mTapGestureLayout.setGestureType(Gesture.TAP);
            }
        });
        cameraView.mapGesture(Gesture.TAP, GestureAction.CAPTURE);
        cameraView.dispatchTouchEvent(event);
        assertTrue(mockController.mPictureCaptured);
    }

    @Test
    public void testGestureAction_focus() {
        mockController.mockStarted(true);
        MotionEvent event = MotionEvent.obtain(0L, 0L, 0, 0f, 0f, 0);
        ui(new Runnable() {
            @Override
            public void run() {
                cameraView.mTapGestureLayout = new TapGestureLayout(cameraView.getContext()) {
                    public boolean onTouchEvent(MotionEvent event) { return true; }
                };
                cameraView.mTapGestureLayout.setGestureType(Gesture.TAP);
            }
        });
        mockController.mFocusStarted = false;
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS);
        cameraView.dispatchTouchEvent(event);
        assertTrue(mockController.mFocusStarted);

        // Try with FOCUS_WITH_MARKER
        mockController.mFocusStarted = false;
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);
        cameraView.dispatchTouchEvent(event);
        assertTrue(mockController.mFocusStarted);
    }

    @Test
    public void testGestureAction_zoom() {
        mockController.mockStarted(true);
        mockController.mZoomChanged = false;
        MotionEvent event = MotionEvent.obtain(0L, 0L, 0, 0f, 0f, 0);
        ui(new Runnable() {
            @Override
            public void run() {
                cameraView.mPinchGestureLayout = new PinchGestureLayout(cameraView.getContext()) {
                    public boolean onTouchEvent(MotionEvent event) { return true; }
                };
                cameraView.mPinchGestureLayout.setGestureType(Gesture.PINCH);
                cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);

            }
        });

        // If factor is 0, we return the same value. The controller should not be notified.
        cameraView.mPinchGestureLayout.mFactor = 0f;
        cameraView.dispatchTouchEvent(event);
        assertFalse(mockController.mZoomChanged);

        // For larger factors, the value is scaled. The controller should be notified.
        cameraView.mPinchGestureLayout.mFactor = 1f;
        cameraView.dispatchTouchEvent(event);
        assertTrue(mockController.mZoomChanged);
    }

    @Test
    public void testGestureAction_exposureCorrection() {
        // This needs a valid CameraOptions value.
        CameraOptions o = mock(CameraOptions.class);
        when(o.getExposureCorrectionMinValue()).thenReturn(-10f);
        when(o.getExposureCorrectionMaxValue()).thenReturn(10f);
        mockController.setMockCameraOptions(o);
        mockController.mockStarted(true);
        mockController.mExposureCorrectionChanged = false;
        MotionEvent event = MotionEvent.obtain(0L, 0L, 0, 0f, 0f, 0);
        ui(new Runnable() {
            @Override
            public void run() {
                cameraView.mScrollGestureLayout = new ScrollGestureLayout(cameraView.getContext()) {
                    public boolean onTouchEvent(MotionEvent event) { return true; }
                };
                cameraView.mScrollGestureLayout.setGestureType(Gesture.SCROLL_HORIZONTAL);
                cameraView.mapGesture(Gesture.SCROLL_HORIZONTAL, GestureAction.EXPOSURE_CORRECTION);
            }
        });

        // If factor is 0, we return the same value. The controller should not be notified.
        cameraView.mScrollGestureLayout.mFactor = 0f;
        cameraView.dispatchTouchEvent(event);
        assertFalse(mockController.mExposureCorrectionChanged);

        // For larger factors, the value is scaled. The controller should be notified.
        cameraView.mScrollGestureLayout.mFactor = 1f;
        cameraView.dispatchTouchEvent(event);
        assertTrue(mockController.mExposureCorrectionChanged);
    }

    //endregion

    //region testMeasure

    private void mockPreviewSize() {
        Size size = new Size(900, 1600);
        mockController.setMockPreviewSize(size);
    }

    @Test
    public void testMeasure_early() {
        mockController.setMockPreviewSize(null);
        cameraView.measure(
                makeMeasureSpec(500, EXACTLY),
                makeMeasureSpec(500, EXACTLY));
        assertEquals(cameraView.getMeasuredWidth(), 500);
        assertEquals(cameraView.getMeasuredHeight(), 500);
    }

    @Test
    public void testMeasure_matchParentBoth() {
        mockPreviewSize();

        // Respect parent/layout constraints on both dimensions.
        cameraView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        cameraView.measure(
                makeMeasureSpec(500, EXACTLY),
                makeMeasureSpec(500, EXACTLY));
        assertEquals(cameraView.getMeasuredWidth(), 500);
        assertEquals(cameraView.getMeasuredHeight(), 500);

        // Even if the parent ViewGroup passes AT_MOST
        cameraView.measure(
                makeMeasureSpec(500, AT_MOST),
                makeMeasureSpec(500, AT_MOST));
        assertEquals(cameraView.getMeasuredWidth(), 500);
        assertEquals(cameraView.getMeasuredHeight(), 500);

        cameraView.measure(
                makeMeasureSpec(500, EXACTLY),
                makeMeasureSpec(500, AT_MOST));
        assertEquals(cameraView.getMeasuredWidth(), 500);
        assertEquals(cameraView.getMeasuredHeight(), 500);
    }

    @Test
    public void testMeasure_wrapContentBoth() {
        mockPreviewSize();

        // Respect parent constraints, but fit aspect ratio.
        // Fit into a 160x160 parent so we espect final width to be 90.
        cameraView.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        cameraView.measure(
                makeMeasureSpec(160, AT_MOST),
                makeMeasureSpec(160, AT_MOST));
        assertEquals(cameraView.getMeasuredWidth(), 90);
        assertEquals(cameraView.getMeasuredHeight(), 160);
    }

    @Test
    public void testMeasure_wrapContentSingle() {
        mockPreviewSize();

        // Respect MATCH_PARENT on height, change width to fit the aspect ratio.
        cameraView.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT));
        cameraView.measure(
                makeMeasureSpec(160, AT_MOST),
                makeMeasureSpec(160, AT_MOST));
        assertEquals(cameraView.getMeasuredWidth(), 90);
        assertEquals(cameraView.getMeasuredHeight(), 160);

        // Respect MATCH_PARENT on width. Enlarge height trying to fit aspect ratio as much as possible.
        cameraView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        cameraView.measure(
                makeMeasureSpec(160, AT_MOST),
                makeMeasureSpec(160, AT_MOST));
        assertEquals(cameraView.getMeasuredWidth(), 160);
        assertEquals(cameraView.getMeasuredHeight(), 160);
    }

    @Test
    public void testMeasure_scrollableContainer() {
        mockPreviewSize();

        // Assume a vertical scroll view. It will pass UNSPECIFIED as height.
        // We respect MATCH_PARENT on width (160), and enlarge height to match the aspect ratio.
        cameraView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        cameraView.measure(
                makeMeasureSpec(160, AT_MOST),
                makeMeasureSpec(0, UNSPECIFIED));
        assertEquals(cameraView.getMeasuredWidth(), 160);
        assertEquals(cameraView.getMeasuredHeight(), 160f * (16f / 9f), 1f); // Leave a margin

        // Assume a view scrolling in both dimensions. It will pass UNSPECIFIED.
        // In this case we must fit the exact preview dimension.
        cameraView.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        cameraView.measure(
                makeMeasureSpec(0, UNSPECIFIED),
                makeMeasureSpec(0, UNSPECIFIED));
        assertEquals(cameraView.getMeasuredWidth(), 900);
        assertEquals(cameraView.getMeasuredHeight(), 1600);
    }

    //endregion

    //region Zoom, ExposureCorrection

    @Test
    public void testZoom() {
        cameraView.setZoom(0.5f);
        assertEquals(cameraView.getZoom(), 0.5f, 0f);
        cameraView.setZoom(-10f);
        assertEquals(cameraView.getZoom(), 0f, 0f);
        cameraView.setZoom(10f);
        assertEquals(cameraView.getZoom(), 1f, 0f);
    }

    @Test
    public void testExposureCorrection() {
        // This needs a valid CameraOptions value.
        CameraOptions o = mock(CameraOptions.class);
        when(o.getExposureCorrectionMinValue()).thenReturn(-10f);
        when(o.getExposureCorrectionMaxValue()).thenReturn(10f);
        mockController.setMockCameraOptions(o);

        cameraView.setExposureCorrection(5f);
        assertEquals(cameraView.getExposureCorrection(), 5f, 0f);
        cameraView.setExposureCorrection(-100f);
        assertEquals(cameraView.getExposureCorrection(), -10f, 0f);
        cameraView.setExposureCorrection(100f);
        assertEquals(cameraView.getExposureCorrection(), 10f, 0f);
    }

    //endregion

    //region testLocation

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testSetLocation() {
        cameraView.setLocation(50d, -50d);
        assertEquals(50d, mockController.mLocation.getLatitude(), 0);
        assertEquals(-50d, mockController.mLocation.getLongitude(), 0);
        assertEquals(0, mockController.mLocation.getAltitude(), 0);
        assertEquals("Unknown", mockController.mLocation.getProvider());
        assertEquals(System.currentTimeMillis(), mockController.mLocation.getTime(), 1000f);

        Location source = new Location("Provider");
        source.setTime(5000);
        source.setLatitude(10d);
        source.setLongitude(-10d);
        source.setAltitude(50d);
        cameraView.setLocation(source);
        Location other = cameraView.getLocation();
        assertEquals(10d, other.getLatitude(), 0d);
        assertEquals(-10d, other.getLongitude(), 0d);
        assertEquals(50d, other.getAltitude(), 0d);
        assertEquals("Provider", other.getProvider());
        assertEquals(5000, other.getTime());
    }

    //endregion

    //region test autofocus

    @Test(expected = IllegalArgumentException.class)
    public void testStartAutoFocus_illegal() {
        cameraView.startAutoFocus(-1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartAutoFocus_illegal2() {
        cameraView.setLeft(0);
        cameraView.setRight(100);
        cameraView.setTop(0);
        cameraView.setBottom(100);
        cameraView.startAutoFocus(200, 200);
    }

    @Test
    public void testStartAutoFocus() {
        cameraView.setLeft(0);
        cameraView.setRight(100);
        cameraView.setTop(0);
        cameraView.setBottom(100);
        cameraView.startAutoFocus(50, 50);
        assertTrue(mockController.mFocusStarted);
    }

    //endregion

    //region test setParameters

    @Test
    public void testSetPlaySounds() {
        cameraView.setPlaySounds(true);
        assertEquals(cameraView.getPlaySounds(), true);
        cameraView.setPlaySounds(false);
        assertEquals(cameraView.getPlaySounds(), false);
    }

    @Test
    public void testSetFlash() {
        cameraView.set(Flash.TORCH);
        assertEquals(cameraView.getFlash(), Flash.TORCH);
        cameraView.set(Flash.OFF);
        assertEquals(cameraView.getFlash(), Flash.OFF);
    }

    @Test
    public void testSetFacing() {
        cameraView.set(Facing.FRONT);
        assertEquals(cameraView.getFacing(), Facing.FRONT);
        cameraView.set(Facing.BACK);
        assertEquals(cameraView.getFacing(), Facing.BACK);
    }

    @Test
    public void testToggleFacing() {
        cameraView.set(Facing.FRONT);
        cameraView.toggleFacing();
        assertEquals(cameraView.getFacing(), Facing.BACK);
        cameraView.toggleFacing();
        assertEquals(cameraView.getFacing(), Facing.FRONT);
    }

    @Test
    public void testSetGrid() {
        cameraView.set(Grid.DRAW_3X3);
        assertEquals(cameraView.getGrid(), Grid.DRAW_3X3);
        cameraView.set(Grid.OFF);
        assertEquals(cameraView.getGrid(), Grid.OFF);
    }

    @Test
    public void testSetWhiteBalance() {
        cameraView.set(WhiteBalance.CLOUDY);
        assertEquals(cameraView.getWhiteBalance(), WhiteBalance.CLOUDY);
        cameraView.set(WhiteBalance.AUTO);
        assertEquals(cameraView.getWhiteBalance(), WhiteBalance.AUTO);
    }

    @Test
    public void testMode() {
        cameraView.set(Mode.VIDEO);
        assertEquals(cameraView.getMode(), Mode.VIDEO);
        cameraView.set(Mode.PICTURE);
        assertEquals(cameraView.getMode(), Mode.PICTURE);
    }

    @Test
    public void testHdr() {
        cameraView.set(Hdr.ON);
        assertEquals(cameraView.getHdr(), Hdr.ON);
        cameraView.set(Hdr.OFF);
        assertEquals(cameraView.getHdr(), Hdr.OFF);
    }

    @Test
    public void testAudio() {
        cameraView.set(Audio.ON);
        assertEquals(cameraView.getAudio(), Audio.ON);
        cameraView.set(Audio.OFF);
        assertEquals(cameraView.getAudio(), Audio.OFF);
    }

    @Test
    public void testVideoCodec() {
        cameraView.set(VideoCodec.H_263);
        assertEquals(cameraView.getVideoCodec(), VideoCodec.H_263);
        cameraView.set(VideoCodec.H_264);
        assertEquals(cameraView.getVideoCodec(), VideoCodec.H_264);
    }

    @Test
    public void testPreviewSizeSelector() {
        SizeSelector source = SizeSelectors.minHeight(50);
        cameraView.setPreviewSize(source);
        SizeSelector result = mockController.getPreviewSizeSelector();
        assertNotNull(result);
        assertEquals(result, source);
    }

    @Test
    public void testPictureSizeSelector() {
        SizeSelector source = SizeSelectors.minHeight(50);
        cameraView.setPictureSize(source);
        SizeSelector result = mockController.getPictureSizeSelector();
        assertNotNull(result);
        assertEquals(result, source);
    }

    @Test
    public void testVideoSizeSelector() {
        SizeSelector source = SizeSelectors.minHeight(50);
        cameraView.setVideoSize(source);
        SizeSelector result = mockController.getVideoSizeSelector();
        assertNotNull(result);
        assertEquals(result, source);
    }

    @Test
    public void testVideoMaxSize() {
        cameraView.setVideoMaxSize(5000);
        assertEquals(cameraView.getVideoMaxSize(), 5000);
    }

    @Test
    public void testVideoMaxDuration() {
        cameraView.setVideoMaxDuration(5000);
        assertEquals(cameraView.getVideoMaxDuration(), 5000);
    }

    //endregion

    //region Lists of listeners and processors

    @SuppressWarnings("UseBulkOperation")
    @Test
    public void testCameraListenerList() {
        assertTrue(cameraView.mListeners.isEmpty());

        CameraListener listener = new CameraListener() {};
        cameraView.addCameraListener(listener);
        assertEquals(cameraView.mListeners.size(), 1);

        cameraView.removeCameraListener(listener);
        assertEquals(cameraView.mListeners.size(), 0);

        cameraView.addCameraListener(listener);
        cameraView.addCameraListener(listener);
        assertEquals(cameraView.mListeners.size(), 2);

        cameraView.clearCameraListeners();
        assertTrue(cameraView.mListeners.isEmpty());

        // Ensure this does not throw a ConcurrentModificationException
        cameraView.addCameraListener(new CameraListener() {});
        cameraView.addCameraListener(new CameraListener() {});
        cameraView.addCameraListener(new CameraListener() {});
        for (CameraListener test : cameraView.mListeners) {
            cameraView.mListeners.remove(test);
        }
    }

    @SuppressWarnings({"NullableProblems", "UseBulkOperation"})
    @Test
    public void testFrameProcessorsList() {
        assertTrue(cameraView.mFrameProcessors.isEmpty());

        FrameProcessor processor = new FrameProcessor() {
            public void process(@NonNull Frame frame) {}
        };
        cameraView.addFrameProcessor(processor);
        assertEquals(cameraView.mFrameProcessors.size(), 1);

        cameraView.removeFrameProcessor(processor);
        assertEquals(cameraView.mFrameProcessors.size(), 0);

        cameraView.addFrameProcessor(processor);
        cameraView.addFrameProcessor(processor);
        assertEquals(cameraView.mFrameProcessors.size(), 2);

        cameraView.clearFrameProcessors();
        assertTrue(cameraView.mFrameProcessors.isEmpty());

        // Ensure this does not throw a ConcurrentModificationException
        cameraView.addFrameProcessor(new FrameProcessor() { public void process(@NonNull Frame f) {} });
        cameraView.addFrameProcessor(new FrameProcessor() { public void process(@NonNull Frame f) {} });
        cameraView.addFrameProcessor(new FrameProcessor() { public void process(@NonNull Frame f) {} });
        for (FrameProcessor test : cameraView.mFrameProcessors) {
            cameraView.mFrameProcessors.remove(test);
        }
    }

    //endregion

    // TODO: test permissions
}

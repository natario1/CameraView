package com.otaliastudios.cameraview;


import android.app.Instrumentation;
import android.content.Context;
import android.hardware.Camera;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
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
public class CameraViewTest {

    private CameraView cameraView;
    private MockCameraController mockController;
    private Preview mockPreview;

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Context context = InstrumentationRegistry.getContext();
                cameraView = new CameraView(context) {
                    @Override
                    protected CameraController instantiateCameraController(CameraCallbacks callbacks, Preview preview) {
                        mockController = new MockCameraController(callbacks, preview);
                        return mockController;
                    }

                    @Override
                    protected Preview instantiatePreview(Context context, ViewGroup container) {
                        mockPreview = new MockPreview(context, container);
                        return mockPreview;
                    }
                };
            }
        });
    }

    @After
    public void tearDown() {
        cameraView = null;
        mockController = null;
        mockPreview = null;
    }

    //region testDefaults

    @Test
    public void testNotStarted() {
        assertFalse(cameraView.isStarted());
        assertNull(cameraView.getCameraOptions());
        assertNull(cameraView.getExtraProperties());
        assertNull(cameraView.getPreviewSize());
        assertNull(cameraView.getCaptureSize());
        assertNull(cameraView.getSnapshotSize());
    }

    @Test
    public void testDefaults() {
        // CameraController
        assertEquals(cameraView.getFlash(), Flash.DEFAULT);
        assertEquals(cameraView.getFacing(), Facing.DEFAULT);
        assertEquals(cameraView.getGrid(), Grid.DEFAULT);
        assertEquals(cameraView.getWhiteBalance(), WhiteBalance.DEFAULT);
        assertEquals(cameraView.getSessionType(), SessionType.DEFAULT);
        assertEquals(cameraView.getHdr(), Hdr.DEFAULT);
        assertEquals(cameraView.getVideoQuality(), VideoQuality.DEFAULT);

        // Self managed
        assertEquals(cameraView.getExposureCorrection(), 0f, 0f);
        assertEquals(cameraView.getZoom(), 0f, 0f);
        assertEquals(cameraView.getCropOutput(), CameraView.DEFAULT_CROP_OUTPUT);
        assertEquals(cameraView.getJpegQuality(), CameraView.DEFAULT_JPEG_QUALITY);
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

    //region testMeasure

    private void mockPreviewSize() {
        Size size = new Size(900, 1600);
        mockController.setMockPreviewSize(size);
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

    // TODO: test setLocation

    // TODO: test touch events

    // TODO: test permissions and start() stop() isStarted()

    // TODO: test CameraCallbacks

}

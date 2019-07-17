package com.otaliastudios.cameraview;


import android.content.Context;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.engine.CameraEngine;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;
import com.otaliastudios.cameraview.internal.utils.Op;
import com.otaliastudios.cameraview.engine.MockCameraEngine;
import com.otaliastudios.cameraview.markers.AutoFocusMarker;
import com.otaliastudios.cameraview.markers.AutoFocusTrigger;
import com.otaliastudios.cameraview.markers.MarkerLayout;
import com.otaliastudios.cameraview.preview.MockCameraPreview;
import com.otaliastudios.cameraview.preview.CameraPreview;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link CameraView#mCameraCallbacks} dispatch functions.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class CameraViewCallbacksTest extends BaseTest {

    private CameraView camera;
    private CameraListener listener;
    private FrameProcessor processor;
    private MockCameraEngine mockController;
    private MockCameraPreview mockPreview;
    private Op<Boolean> op;

    @Before
    public void setUp() {
        ui(new Runnable() {
            @Override
            public void run() {
                Context context = context();
                listener = mock(CameraListener.class);
                processor = mock(FrameProcessor.class);
                camera = new CameraView(context) {

                    @NonNull
                    @Override
                    protected CameraEngine instantiateCameraEngine(@NonNull Engine engine, @NonNull CameraEngine.Callback callback) {
                        mockController = new MockCameraEngine(callback);
                        return mockController;
                    }

                    @NonNull
                    @Override
                    protected CameraPreview instantiatePreview(@NonNull Preview preview, @NonNull Context context, @NonNull ViewGroup container) {
                        mockPreview = new MockCameraPreview(context, container);
                        return mockPreview;
                    }

                    @Override
                    protected boolean checkPermissions(@NonNull Audio audio) {
                        return true;
                    }
                };
                camera.doInstantiatePreview();
                camera.addCameraListener(listener);
                camera.addFrameProcessor(processor);
                op = new Op<>(true);
            }
        });
    }

    @After
    public void tearDown() {
        camera = null;
        mockController = null;
        mockPreview = null;
        listener = null;
    }

    // Completes our op.
    private Stubber completeTask() {
        return doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                op.end(true);
                return null;
            }
        });
    }

    @Test
    public void testDontDispatchIfRemoved() {
        camera.removeCameraListener(listener);
        completeTask().when(listener).onCameraOpened(null);
        camera.mCameraCallbacks.dispatchOnCameraOpened(null);

        assertNull(op.await(500));
        verify(listener, never()).onCameraOpened(null);
    }

    @Test
    public void testDontDispatchIfCleared() {
        camera.clearCameraListeners();
        completeTask().when(listener).onCameraOpened(null);
        camera.mCameraCallbacks.dispatchOnCameraOpened(null);

        assertNull(op.await(500));
        verify(listener, never()).onCameraOpened(null);
    }

    @Test
    public void testDispatchOnCameraOpened() {
        completeTask().when(listener).onCameraOpened(null);
        camera.mCameraCallbacks.dispatchOnCameraOpened(null);

        assertNotNull(op.await(500));
        verify(listener, times(1)).onCameraOpened(null);
    }

    @Test
    public void testDispatchOnCameraClosed() {
        completeTask().when(listener).onCameraClosed();
        camera.mCameraCallbacks.dispatchOnCameraClosed();

        assertNotNull(op.await(500));
        verify(listener, times(1)).onCameraClosed();
    }

    @Test
    public void testDispatchOnVideoRecordingStart() {
        completeTask().when(listener).onVideoRecordingStart();
        camera.mCameraCallbacks.dispatchOnVideoRecordingStart();

        assertNotNull(op.await(500));
        verify(listener, times(1)).onVideoRecordingStart();
    }

    @Test
    public void testDispatchOnVideoRecordingEnd() {
        completeTask().when(listener).onVideoRecordingEnd();
        camera.mCameraCallbacks.dispatchOnVideoRecordingEnd();

        assertNotNull(op.await(500));
        verify(listener, times(1)).onVideoRecordingEnd();
    }

    @Test
    public void testDispatchOnVideoTaken() {
        VideoResult.Stub stub = new VideoResult.Stub();
        completeTask().when(listener).onVideoTaken(any(VideoResult.class));
        camera.mCameraCallbacks.dispatchOnVideoTaken(stub);

        assertNotNull(op.await(500));
        verify(listener, times(1)).onVideoTaken(any(VideoResult.class));
    }

    @Test
    public void testDispatchOnPictureTaken() {
        PictureResult.Stub stub = new PictureResult.Stub();
        completeTask().when(listener).onPictureTaken(any(PictureResult.class));
        camera.mCameraCallbacks.dispatchOnPictureTaken(stub);
        assertNotNull(op.await(500));
        verify(listener, times(1)).onPictureTaken(any(PictureResult.class));
    }

    @Test
    public void testDispatchOnZoomChanged() {
        completeTask().when(listener).onZoomChanged(eq(0f), eq(new float[]{0, 1}), nullable(PointF[].class));
        camera.mCameraCallbacks.dispatchOnZoomChanged(0f, null);

        assertNotNull(op.await(500));
        verify(listener, times(1)).onZoomChanged(eq(0f), eq(new float[]{0, 1}), nullable(PointF[].class));
    }

    @Test
    public void testDispatchOnExposureCorrectionChanged() {
        completeTask().when(listener).onExposureCorrectionChanged(0f, null, null);
        camera.mCameraCallbacks.dispatchOnExposureCorrectionChanged(0f, null, null);

        assertNotNull(op.await(500));
        verify(listener, times(1)).onExposureCorrectionChanged(0f, null, null);
    }

    @Test
    public void testDispatchOnFocusStart() {
        // Enable tap gesture.
        // Can't mock package protected. camera.mTapGestureFinder = mock(TapGestureLayout.class);
        camera.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS);
        AutoFocusMarker marker = mock(AutoFocusMarker.class);
        MarkerLayout markerLayout = mock(MarkerLayout.class);
        camera.setAutoFocusMarker(marker);
        camera.mMarkerLayout = markerLayout;

        PointF point = new PointF();
        completeTask().when(listener).onAutoFocusStart(point);
        camera.mCameraCallbacks.dispatchOnFocusStart(Gesture.TAP, point);

        assertNotNull(op.await(500));
        verify(listener, times(1)).onAutoFocusStart(point);
        verify(marker, times(1)).onAutoFocusStart(AutoFocusTrigger.GESTURE, point);
        verify(markerLayout, times(1)).onEvent(eq(MarkerLayout.TYPE_AUTOFOCUS), any(PointF[].class));
    }

    @Test
    public void testDispatchOnFocusEnd() {
        // Enable tap gesture.
        // Can't mock package protected. camera.mTapGestureFinder = mock(TapGestureLayout.class);
        camera.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS);
        AutoFocusMarker marker = mock(AutoFocusMarker.class);
        camera.setAutoFocusMarker(marker);

        PointF point = new PointF();
        boolean success = true;
        completeTask().when(listener).onAutoFocusEnd(success, point);
        camera.mCameraCallbacks.dispatchOnFocusEnd(Gesture.TAP, success, point);

        assertNotNull(op.await(500));
        verify(listener, times(1)).onAutoFocusEnd(success, point);
        verify(marker, times(1)).onAutoFocusEnd(AutoFocusTrigger.GESTURE, success, point);

        // Can't mock package protected. verify(camera.mTapGestureFinder, times(1)).onAutoFocusEnd(success);
    }

    @Test
    public void testOrientationCallbacks() {
        completeTask().when(listener).onOrientationChanged(anyInt());
        camera.mCameraCallbacks.onDeviceOrientationChanged(90);
        assertNotNull(op.await(500));
        verify(listener, times(1)).onOrientationChanged(anyInt());
    }

    // TODO: test onShutter, here or elsewhere

    @Test
    public void testCameraError() {
        CameraException error = new CameraException(new RuntimeException("Error"));
        completeTask().when(listener).onCameraError(error);

        camera.mCameraCallbacks.dispatchError(error);
        assertNotNull(op.await(500));
        verify(listener, times(1)).onCameraError(error);
    }

    @Test
    public void testProcessFrame() {
        Frame mock = mock(Frame.class);
        completeTask().when(processor).process(mock);
        camera.mCameraCallbacks.dispatchFrame(mock);

        assertNotNull(op.await(500));
        verify(processor, times(1)).process(mock);
    }
}

package com.otaliastudios.cameraview;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ViewGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class CameraCallbacksTest extends BaseTest {

    private CameraView camera;
    private CameraView.CameraCallbacks callbacks;
    private CameraListener listener;
    private MockCameraController mockController;
    private MockPreview mockPreview;
    private Task<Boolean> task;


    @Before
    public void setUp() {
        ui(new Runnable() {
            @Override
            public void run() {
                Context context = context();
                listener = mock(CameraListener.class);
                camera = new CameraView(context) {
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

                    @Override
                    protected boolean checkPermissions(SessionType sessionType, Audio audio) {
                        return true;
                    }
                };
                camera.addCameraListener(listener);
                callbacks = camera.mCameraCallbacks;
                task = new Task<>();
                task.listen();
            }
        });
    }

    @After
    public void tearDown() {
        camera = null;
        mockController = null;
        mockPreview = null;
        callbacks = null;
        listener = null;
    }

    // Completes our task.
    private Answer completeTask() {
        return new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                task.end(true);
                return null;
            }
        };
    }

    @Test
    public void testDontDispatchIfRemoved() {
        camera.removeCameraListener(listener);
        doAnswer(completeTask()).when(listener).onCameraOpened(null);
        callbacks.dispatchOnCameraOpened(null);

        assertNull(task.await(200));
        verify(listener, never()).onCameraOpened(null);
    }

    @Test
    public void testDontDispatchIfCleared() {
        camera.clearCameraListeners();
        doAnswer(completeTask()).when(listener).onCameraOpened(null);
        callbacks.dispatchOnCameraOpened(null);

        assertNull(task.await(200));
        verify(listener, never()).onCameraOpened(null);
    }

    @Test
    public void testDispatchOnCameraOpened() {
        doAnswer(completeTask()).when(listener).onCameraOpened(null);
        callbacks.dispatchOnCameraOpened(null);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onCameraOpened(null);
    }

    @Test
    public void testDispatchOnCameraClosed() {
        doAnswer(completeTask()).when(listener).onCameraClosed();
        callbacks.dispatchOnCameraClosed();

        assertNotNull(task.await(200));
        verify(listener, times(1)).onCameraClosed();
    }

    @Test
    public void testDispatchOnVideoTaken() {
        doAnswer(completeTask()).when(listener).onVideoTaken(null);
        callbacks.dispatchOnVideoTaken(null);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onVideoTaken(null);
    }

    @Test
    public void testDispatchOnZoomChanged() {
        doAnswer(completeTask()).when(listener).onZoomChanged(anyFloat(), any(float[].class), any(PointF[].class));
        callbacks.dispatchOnZoomChanged(0f, null);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onZoomChanged(anyFloat(), any(float[].class), any(PointF[].class));
    }

    @Test
    public void testDispatchOnExposureCorrectionChanged() {
        doAnswer(completeTask()).when(listener).onExposureCorrectionChanged(0f, null, null);
        callbacks.dispatchOnExposureCorrectionChanged(0f, null, null);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onExposureCorrectionChanged(0f, null, null);
    }

    @Test
    public void testDispatchOnFocusStart() {
        // Enable tap gesture.
        // Can't mock package protected. camera.mTapGestureLayout = mock(TapGestureLayout.class);
        camera.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);

        PointF point = new PointF();
        doAnswer(completeTask()).when(listener).onFocusStart(point);
        callbacks.dispatchOnFocusStart(Gesture.TAP, point);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onFocusStart(point);
        // Can't mock package protected. verify(camera.mTapGestureLayout, times(1)).onFocusStart(point);
    }

    @Test
    public void testDispatchOnFocusEnd() {
        // Enable tap gesture.
        // Can't mock package protected. camera.mTapGestureLayout = mock(TapGestureLayout.class);
        camera.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);

        PointF point = new PointF();
        boolean success = true;
        doAnswer(completeTask()).when(listener).onFocusEnd(success, point);
        callbacks.dispatchOnFocusEnd(Gesture.TAP, success, point);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onFocusEnd(success, point);
        // Can't mock package protected. verify(camera.mTapGestureLayout, times(1)).onFocusEnd(success);
    }

    @Test
    public void testOrientationCallbacks_deviceOnly() {
        doAnswer(completeTask()).when(listener).onOrientationChanged(anyInt());

        // Assert not called. Both methods must be called.
        callbacks.onDeviceOrientationChanged(0);
        assertNull(task.await(200));
        verify(listener, never()).onOrientationChanged(anyInt());
    }

    @Test
    public void testOrientationCallbacks_displayOnly() {
        doAnswer(completeTask()).when(listener).onOrientationChanged(anyInt());

        // Assert not called. Both methods must be called.
        callbacks.onDisplayOffsetChanged(0);
        assertNull(task.await(200));
        verify(listener, never()).onOrientationChanged(anyInt());
    }

    @Test
    public void testOrientationCallbacks_both() {
        doAnswer(completeTask()).when(listener).onOrientationChanged(anyInt());

        // Assert called.
        callbacks.onDisplayOffsetChanged(0);
        callbacks.onDeviceOrientationChanged(90);
        assertNotNull(task.await(200));
        verify(listener, times(1)).onOrientationChanged(anyInt());
    }


    @Test
    public void testProcessJpeg() {
        int[] viewDim = new int[]{ 200, 200 };
        int[] imageDim = new int[]{ 1000, 1600 };

        // With crop flag: expect a 1:1 ratio.
        int[] output = testProcessImage(true, true, viewDim, imageDim);
        assertEquals(output[0], 1000);
        assertEquals(output[1], 1000);

        // Without crop flag: expect original ratio.
        output = testProcessImage(true, false, viewDim, imageDim);
        assertEquals(output[0], imageDim[0]);
        assertEquals(output[1], imageDim[1]);
    }

    @Test
    public void testProcessYuv() {
        int[] viewDim = new int[]{ 200, 200 };
        int[] imageDim = new int[]{ 1000, 1600 };

        // With crop flag: expect a 1:1 ratio.
        int[] output = testProcessImage(false, true, viewDim, imageDim);
        assertEquals(output[0], 1000);
        assertEquals(output[1], 1000);

        // Without crop flag: expect original ratio.
        output = testProcessImage(false, false, viewDim, imageDim);
        assertEquals(output[0], imageDim[0]);
        assertEquals(output[1], imageDim[1]);
    }

    private int[] testProcessImage(boolean jpeg, boolean crop, int[] viewDim, int[] imageDim) {
        // End our task when onPictureTaken is called. Take note of the result.
        final Task<byte[]> jpegTask = new Task<>();
        jpegTask.listen();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                jpegTask.end((byte[]) invocation.getArguments()[0]);
                return null;
            }
        }).when(listener).onPictureTaken(any(byte[].class));

        // Fake our own dimensions.
        camera.setTop(0);
        camera.setBottom(viewDim[1]);
        camera.setLeft(0);
        camera.setRight(viewDim[0]);

        // Ensure the image will (not) be cropped.
        camera.setCropOutput(crop);
        mockPreview.setIsCropping(crop);

        // Create fake JPEG array and trigger the process.
        if (jpeg) {
            callbacks.processImage(mockJpeg(imageDim[0], imageDim[1]), true, false);
        } else {
            callbacks.processSnapshot(mockYuv(imageDim[0], imageDim[1]), true, false);
        }

        // Wait for result and get out dimensions.
        byte[] result = jpegTask.await(800);
        assertNotNull(result);
        Bitmap bitmap = BitmapFactory.decodeByteArray(result, 0, result.length);
        return new int[]{ bitmap.getWidth(), bitmap.getHeight() };
    }
}

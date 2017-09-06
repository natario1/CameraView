package com.otaliastudios.cameraview;


import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

public abstract class PreviewTest extends BaseTest {

    protected abstract Preview createPreview(Context context, ViewGroup parent);

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    private Preview preview;
    private Preview.SurfaceCallback callback;
    private Size surfaceSize;
    private Task<Void> surfaceAvailability;

    @Before
    public void setUp() {
        ui(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                preview = createPreview(a, a.getContentView());
                surfaceSize = a.getContentSize();

                callback = mock(Preview.SurfaceCallback.class);
                preview.setSurfaceCallback(callback);

                surfaceAvailability = new Task<>();
                surfaceAvailability.listen();
                surfaceAvailability.start();
                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        surfaceAvailability.end(null);
                        return null;
                    }
                }).when(callback).onSurfaceAvailable();
            }
        });
    }

    @Test
    public void testDefaults() {
        ViewGroup parent = rule.getActivity().getContentView();
        assertNotNull(preview.getView());
        assertEquals(parent.getChildAt(0), preview.getView());
        assertNotNull(preview.getOutputClass());
    }

    @Test
    public void testDesiredSize() {
        preview.setDesiredSize(160, 90);
        assertEquals(160, preview.getDesiredSize().getWidth());
        assertEquals(90, preview.getDesiredSize().getHeight());
    }

    @Test
    public void testSurfaceAvailable() {
        surfaceAvailability.await();

        // Wait for surface to be available.
        verify(callback, times(1)).onSurfaceAvailable();
        assertEquals(surfaceSize.getWidth(), preview.getSurfaceSize().getWidth());
        assertEquals(surfaceSize.getHeight(), preview.getSurfaceSize().getHeight());
    }

    @Test
    public void testSurfaceDestroyed() {
        surfaceAvailability.await();

        // Trigger a destroy.
        ui(new Runnable() {
            @Override
            public void run() {
                rule.getActivity().getContentView().removeAllViews();
            }
        });
        assertEquals(0, preview.getSurfaceSize().getWidth());
        assertEquals(0, preview.getSurfaceSize().getHeight());
    }

    @Test
    public void testCropCenter() throws Exception {
        surfaceAvailability.await();

        // Not cropping.
        preview.mCropTask.listen();
        preview.setDesiredSize(100, 100); // Wait...
        preview.mCropTask.await();
        assertEquals(100f / 100f, getScaledAspectRatio(), 0.01f);
        assertFalse(preview.isCropping());

        // Cropping.
        preview.mCropTask.listen();
        preview.setDesiredSize(160, 50); // Wait...
        preview.mCropTask.await();
        assertEquals(160f / 50f, getScaledAspectRatio(), 0.01f);
        assertTrue(preview.isCropping());

        // Not cropping.
        preview.mCropTask.listen();
        preview.onSurfaceSizeChanged(1600, 500); // Wait...
        preview.mCropTask.await();
        assertEquals(160f / 50f, getScaledAspectRatio(), 0.01f);
        assertFalse(preview.isCropping());
    }

    private float getScaledAspectRatio() {
        Size size = preview.getSurfaceSize();
        int newWidth = (int) (((float) size.getWidth()) * preview.getView().getScaleX());
        int newHeight = (int) (((float) size.getHeight()) * preview.getView().getScaleY());
        return AspectRatio.of(newWidth, newHeight).toFloat();
    }
}

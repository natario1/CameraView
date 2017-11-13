package com.otaliastudios.cameraview;


import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.view.ViewGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

public abstract class PreviewTest extends BaseTest {

    protected abstract CameraPreview createPreview(Context context, ViewGroup parent, CameraPreview.SurfaceCallback callback);

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    protected CameraPreview preview;
    protected Size surfaceSize;
    private CameraPreview.SurfaceCallback callback;
    private Task<Boolean> availability;

    @Before
    public void setUp() {
        availability = new Task<>(true);

        ui(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                surfaceSize = a.getContentSize();

                callback = mock(CameraPreview.SurfaceCallback.class);
                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (availability != null) availability.end(true);
                        return null;
                    }
                }).when(callback).onSurfaceAvailable();
                preview = createPreview(a, a.getContentView(), callback);
            }
        });
    }

    // Wait for surface to be available.
    protected void ensureAvailable() {
        assertNotNull(availability.await(2000));
    }

    // Trigger a destroy.
    protected void ensureDestroyed() {
        ui(new Runnable() {
            @Override
            public void run() {
                rule.getActivity().getContentView().removeView(preview.getView());
            }
        });
        idle();
    }

    @After
    public void tearDown() {
        preview = null;
        callback = null;
        surfaceSize = null;
        availability = null;
    }

    @Test
    public void testDefaults() {
        ensureAvailable();
        assertTrue(preview.isReady());
        assertNotNull(preview.getView());
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
        ensureAvailable();
        verify(callback, times(1)).onSurfaceAvailable();
        assertEquals(surfaceSize.getWidth(), preview.getSurfaceSize().getWidth());
        assertEquals(surfaceSize.getHeight(), preview.getSurfaceSize().getHeight());
    }

    @Test
    public void testSurfaceDestroyed() {
        ensureAvailable();
        ensureDestroyed();
        assertEquals(0, preview.getSurfaceSize().getWidth());
        assertEquals(0, preview.getSurfaceSize().getHeight());
    }

    @Test
    public void testCropCenter() throws Exception {
        ensureAvailable();

        // This is given by the activity, it's the fixed size.
        float view = getViewAspectRatio();

        // If we apply a desired size with same aspect ratio, there should be no crop.
        setDesiredAspectRatio(view);
        assertFalse(preview.isCropping());

        // If we apply a different aspect ratio, there should be cropping.
        float desired = view * 1.2f;
        if (preview.supportsCropping()) {
            setDesiredAspectRatio(desired);
            assertTrue(preview.isCropping());
        }

        // Since desired is 'desired', let's fake a new view size that is consistent with it.
        // Ensure crop is not happening anymore.
        preview.mCropTask.listen();
        preview.onSurfaceSizeChanged((int) (50f * desired), 50); // Wait...
        preview.mCropTask.await();
        assertEquals(desired, getViewAspectRatioWithScale(), 0.01f);
        assertFalse(preview.isCropping());
    }

    private void setDesiredAspectRatio(float desiredAspectRatio) {
        preview.mCropTask.listen();
        preview.setDesiredSize((int) (10f * desiredAspectRatio), 10); // Wait...
        preview.mCropTask.await();
        assertEquals(desiredAspectRatio, getViewAspectRatioWithScale(), 0.01f);

    }

    private float getViewAspectRatio() {
        Size size = preview.getSurfaceSize();
        return AspectRatio.of(size.getWidth(), size.getHeight()).toFloat();
    }

    private float getViewAspectRatioWithScale() {
        Size size = preview.getSurfaceSize();
        int newWidth = (int) (((float) size.getWidth()) * preview.getView().getScaleX());
        int newHeight = (int) (((float) size.getHeight()) * preview.getView().getScaleY());
        return AspectRatio.of(newWidth, newHeight).toFloat();
    }
}

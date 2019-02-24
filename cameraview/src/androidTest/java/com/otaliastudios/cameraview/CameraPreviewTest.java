package com.otaliastudios.cameraview;


import android.content.Context;
import androidx.test.rule.ActivityTestRule;
import android.view.ViewGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

public abstract class CameraPreviewTest extends BaseTest {

    protected abstract CameraPreview createPreview(Context context, ViewGroup parent, CameraPreview.SurfaceCallback callback);

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    @SuppressWarnings("WeakerAccess")
    protected CameraPreview preview;
    @SuppressWarnings("WeakerAccess")
    protected Size surfaceSize;
    private CameraPreview.SurfaceCallback callback;

    private Task<Boolean> available;
    private Task<Boolean> destroyed;

    @Before
    public void setUp() {
        available = new Task<>(true);
        destroyed = new Task<>(true);

        ui(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                surfaceSize = a.getContentSize();
                callback = mock(CameraPreview.SurfaceCallback.class);

                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        if (available != null) available.end(true);
                        return null;
                    }
                }).when(callback).onSurfaceAvailable();

                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        if (destroyed != null) destroyed.end(true);
                        return null;
                    }
                }).when(callback).onSurfaceDestroyed();

                preview = createPreview(a, a.getContentView(), callback);
            }
        });
    }

    // Wait for surface to be available.
    protected void ensureAvailable() {
        assertNotNull(available.await(2000));
    }

    // Trigger a destroy.
    protected void ensureDestroyed() {
        ui(new Runnable() {
            @Override
            public void run() {
                rule.getActivity().getContentView().removeView(preview.getRootView());
            }
        });
        assertNotNull(destroyed.await(2000));
    }

    @After
    public void tearDown() {
        preview = null;
        callback = null;
        surfaceSize = null;
        available = null;
        destroyed = null;
    }

    @Test
    public void testDefaults() {
        ensureAvailable();
        assertTrue(preview.hasSurface());
        assertNotNull(preview.getView());
        assertNotNull(preview.getRootView());
        assertNotNull(preview.getOutputClass());
    }

    @Test
    public void testDesiredSize() {
        preview.setStreamSize(160, 90, false);
        assertEquals(160, preview.getStreamSize().getWidth());
        assertEquals(90, preview.getStreamSize().getHeight());
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
        // This might be called twice in Texture because it overrides ensureDestroyed method
        verify(callback, atLeastOnce()).onSurfaceDestroyed();
        assertEquals(0, preview.getSurfaceSize().getWidth());
        assertEquals(0, preview.getSurfaceSize().getHeight());
    }

    @Test
    public void testCropCenter() {
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
        preview.dispatchOnSurfaceSizeChanged((int) (50f * desired), 50); // Wait...
        preview.mCropTask.await();
        assertEquals(desired, getViewAspectRatioWithScale(), 0.01f);
        assertFalse(preview.isCropping());
    }

    private void setDesiredAspectRatio(float desiredAspectRatio) {
        preview.mCropTask.listen();
        preview.setStreamSize((int) (10f * desiredAspectRatio), 10, false); // Wait...
        preview.mCropTask.await();
        assertEquals(desiredAspectRatio, getViewAspectRatioWithScale(), 0.01f);

    }

    private float getViewAspectRatio() {
        Size size = preview.getSurfaceSize();
        return AspectRatio.of(size.getWidth(), size.getHeight()).toFloat();
    }

    private float getViewAspectRatioWithScale() {
        Size size = preview.getSurfaceSize();
        int newWidth = (int) (((float) size.getWidth()) * getCropScaleX());
        int newHeight = (int) (((float) size.getHeight()) * getCropScaleY());
        return AspectRatio.of(newWidth, newHeight).toFloat();
    }

    abstract protected float getCropScaleX();

    abstract protected float getCropScaleY();
}

package com.otaliastudios.cameraview;


import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Semaphore;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

public abstract class PreviewTest {

    protected abstract Preview createPreview(Context context, ViewGroup parent);

    private Preview preview;
    private ViewGroup parent;
    private Preview.SurfaceCallback callback;
    private Size surfaceSize = new Size(1000, 1000);

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Context context = rule.getActivity();

                // Using a parent so we know its size.
                parent = new FrameLayout(context);
                parent.setLayoutParams(new ViewGroup.LayoutParams(
                        surfaceSize.getWidth(), surfaceSize.getHeight()));
                preview = createPreview(context, parent);
                callback = mock(Preview.SurfaceCallback.class);
                preview.setSurfaceCallback(callback);

                // Add all to a decor view and to the activity.
                FrameLayout decor = new FrameLayout(context);
                decor.addView(parent);
                rule.getActivity().setContentView(decor);
            }
        });
    }

    @Test
    public void testDefaults() {
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
    public void testSurfaceSize() {
        preview.onSurfaceAvailable(500, 1000);
        assertEquals(500, preview.getSurfaceSize().getWidth());
        assertEquals(1000, preview.getSurfaceSize().getHeight());

        preview.onSurfaceSizeChanged(50, 100);
        assertEquals(50, preview.getSurfaceSize().getWidth());
        assertEquals(100, preview.getSurfaceSize().getHeight());

        preview.onSurfaceDestroyed();
        assertEquals(0, preview.getSurfaceSize().getWidth());
        assertEquals(0, preview.getSurfaceSize().getHeight());
    }

    @Test
    public void testCallbacks() {
        preview.onSurfaceAvailable(500, 1000);
        verify(callback, times(1)).onSurfaceAvailable();

        preview.onSurfaceSizeChanged(50, 100);
        verify(callback, times(1)).onSurfaceChanged();
    }

    @Test
    public void testCropCenter() throws Exception {
        Task cropTask = preview.mCropTask;

        // If aspect ratio is different, there should be a crop.
        preview.onSurfaceAvailable(
                surfaceSize.getWidth(),
                surfaceSize.getHeight());

        // Not cropping.
        cropTask.listen();
        preview.setDesiredSize(100, 100); // Wait...
        cropTask.await();
        assertEquals(100f / 100f, getScaledAspectRatio(), 0.01f);
        assertFalse(preview.isCropping());

        // Cropping.
        cropTask.listen();
        preview.setDesiredSize(160, 50); // Wait...
        cropTask.await();
        assertEquals(160f / 50f, getScaledAspectRatio(), 0.01f);
        assertTrue(preview.isCropping());

        // Not cropping.
        cropTask.listen();
        preview.onSurfaceSizeChanged(1600, 500); // Wait...
        cropTask.await();
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

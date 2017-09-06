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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

public abstract class PreviewTest {

    protected abstract Preview createPreview(Context context, ViewGroup parent);

    private Preview preview;
    private ViewGroup parent;
    private Preview.SurfaceCallback callback;
    private Semaphore lock;

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Context context = rule.getActivity();
                parent = new FrameLayout(context);
                preview = createPreview(context, parent);
                callback = mock(Preview.SurfaceCallback.class);
                preview.setSurfaceCallback(callback);
                rule.getActivity().setContentView(parent);

                lock = new Semaphore(1, true);
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
        preview.setCropListener(new Preview.CropListener() {
            @Override
            public void onPreCrop() {
                try {
                    lock.acquire();
                } catch (Exception e) {
                    e.printStackTrace();
                };
            }

            @Override
            public void onPostCrop() {
                lock.release();
            }
        });

        // If aspect ratio is different, there should be a crop.
        Size s = new Size(1000, 1000);
        preview.onSurfaceAvailable(s.getWidth(), s.getHeight());

        // Not cropping.
        preview.setDesiredSize(100, 100); // Wait...
        lock.acquire();
        assertEquals(AspectRatio.of(100, 100), getScaledAspectRatio());
        assertFalse(preview.isCropping());
        lock.release();

        // Cropping.
        preview.setDesiredSize(160, 50); // Wait...
        lock.acquire();
        assertEquals(AspectRatio.of(160, 50), getScaledAspectRatio());
        assertTrue(preview.isCropping());
        lock.release();

        // Not cropping.
        preview.onSurfaceSizeChanged(1600, 500); // Wait...
        lock.acquire();
        assertEquals(AspectRatio.of(160, 50), getScaledAspectRatio());
        assertFalse(preview.isCropping());
        lock.release();
    }

    private AspectRatio getScaledAspectRatio() {
        Size size = preview.getSurfaceSize();
        int newWidth = (int) ((float) size.getWidth() * preview.getView().getScaleX());
        int newHeight = (int) ((float) size.getHeight() * preview.getView().getScaleY());
        return AspectRatio.of(newWidth, newHeight);
    }
}

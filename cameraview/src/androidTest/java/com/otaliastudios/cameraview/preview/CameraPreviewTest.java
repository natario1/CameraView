package com.otaliastudios.cameraview.preview;


import android.content.Context;
import androidx.test.rule.ActivityTestRule;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.TestActivity;
import com.otaliastudios.cameraview.tools.Op;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

public abstract class CameraPreviewTest<T extends CameraPreview> extends BaseTest {

    private final static long DELAY = 4000;

    protected abstract T createPreview(Context context, ViewGroup parent);

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    protected T preview;
    @SuppressWarnings("WeakerAccess")
    protected Size surfaceSize;
    private CameraPreview.SurfaceCallback callback;

    private Op<Boolean> available;
    private Op<Boolean> destroyed;

    @Before
    public void setUp() {
        available = new Op<>();
        destroyed = new Op<>();

        uiSync(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                surfaceSize = a.getContentSize();
                callback = mock(CameraPreview.SurfaceCallback.class);

                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        if (available != null) available.controller().end(true);
                        return null;
                    }
                }).when(callback).onSurfaceAvailable();

                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        if (destroyed != null) destroyed.controller().end(true);
                        return null;
                    }
                }).when(callback).onSurfaceDestroyed();

                preview = createPreview(a, a.getContentView());
                preview.setSurfaceCallback(callback);
            }
        });
    }

    // Wait for surface to be available.
    protected void ensureAvailable() {
        assertNotNull(available.await(DELAY));
    }

    // Trigger a destroy.
    protected void ensureDestroyed() {
        uiSync(new Runnable() {
            @Override
            public void run() {
                rule.getActivity().getContentView().removeView(preview.getRootView());
            }
        });
        assertNotNull(destroyed.await(DELAY));
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
        preview.setStreamSize(160, 90);
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
        preview.mCropCallback = mock(CameraPreview.CropCallback.class);
        Op<Void> op = new Op<>();
        doEndOp(op, null).when(preview.mCropCallback).onCrop();
        preview.dispatchOnSurfaceSizeChanged((int) (50f * desired), 50);

        op.await(); // Wait...
        assertEquals(desired, getViewAspectRatioWithScale(), 0.01f);
        assertFalse(preview.isCropping());
    }

    private void setDesiredAspectRatio(float desiredAspectRatio) {
        preview.mCropCallback = mock(CameraPreview.CropCallback.class);
        Op<Void> op = new Op<>();
        doEndOp(op, null).when(preview.mCropCallback).onCrop();
        preview.setStreamSize((int) (10f * desiredAspectRatio), 10);

        op.await(); // Wait...
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

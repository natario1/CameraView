package com.otaliastudios.cameraview.overlay;


import android.graphics.Canvas;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseEglTest;
import com.otaliastudios.cameraview.internal.GlTextureDrawer;
import com.otaliastudios.cameraview.size.Size;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class OverlayDrawerTest extends BaseEglTest {

    @Test
    public void testDraw() {
        Overlay overlay = mock(Overlay.class);
        OverlayDrawer drawer = new OverlayDrawer(overlay, new Size(WIDTH, HEIGHT));
        drawer.draw(Overlay.Target.PICTURE_SNAPSHOT);
        verify(overlay, times(1)).drawOn(
                eq(Overlay.Target.PICTURE_SNAPSHOT),
                any(Canvas.class));
    }

    @Test
    public void testGetTransform() {
        // We'll check that the transform is not all zeros, which is highly unlikely
        // (the default transform should be the identity matrix)
        OverlayDrawer drawer = new OverlayDrawer(mock(Overlay.class), new Size(WIDTH, HEIGHT));
        drawer.draw(Overlay.Target.PICTURE_SNAPSHOT);
        assertThat(drawer.getTransform(), new BaseMatcher<float[]>() {
            public void describeTo(Description description) { }
            public boolean matches(Object item) {
                float[] array = (float[]) item;
                for (float value : array) {
                    if (value != 0.0F) return true;
                }
                return false;
            }
        });
    }

    @Test
    public void testRender() {
        OverlayDrawer drawer = new OverlayDrawer(mock(Overlay.class), new Size(WIDTH, HEIGHT));
        drawer.mTextureDrawer = spy(drawer.mTextureDrawer);

        drawer.draw(Overlay.Target.PICTURE_SNAPSHOT);
        drawer.render(0L);
        verify(drawer.mTextureDrawer, times(1)).draw(0L);
    }

    @Test
    public void testRelease() {
        OverlayDrawer drawer = new OverlayDrawer(mock(Overlay.class), new Size(WIDTH, HEIGHT));
        GlTextureDrawer textureDrawer = spy(drawer.mTextureDrawer);
        drawer.mTextureDrawer = textureDrawer;
        drawer.release();
        verify(textureDrawer, times(1)).release();
    }
}

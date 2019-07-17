package com.otaliastudios.cameraview.overlay;


import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class OverlayLayoutTest extends BaseTest {

    private OverlayLayout overlayLayout;

    @Before
    public void setUp() {
        overlayLayout = spy(new OverlayLayout(getContext()));
    }

    @After
    public void tearDown() {
        overlayLayout = null;
    }

    @Test
    public void testIsOverlay_LayoutParams() {
        ViewGroup.LayoutParams params;

        params = new ViewGroup.LayoutParams(10, 10);
        assertFalse(overlayLayout.isOverlay(params));

        params = new OverlayLayout.LayoutParams(10, 10);
        assertTrue(overlayLayout.isOverlay(params));
    }

    @Test
    public void testIsOverlay_attributeSet() throws Exception {
        int layout1 = com.otaliastudios.cameraview.test.R.layout.overlay;
        int layout2 = com.otaliastudios.cameraview.test.R.layout.not_overlay;

        AttributeSet set1 = getAttributeSet(layout1);
        assertTrue(overlayLayout.isOverlay(set1));

        AttributeSet set2 = getAttributeSet(layout2);
        assertFalse(overlayLayout.isOverlay(set2));
    }

    @NonNull
    private AttributeSet getAttributeSet(int layout) throws Exception {
        // Get the attribute set in the correct state: use a parser and move to START_TAG
        XmlResourceParser parser = getContext().getResources().getLayout(layout);
        //noinspection StatementWithEmptyBody
        while (parser.next() != XmlResourceParser.START_TAG) {}
        return Xml.asAttributeSet(parser);
    }

    @Test
    public void testLayoutParams_drawsOn() {
        OverlayLayout.LayoutParams params = new OverlayLayout.LayoutParams(10, 10);

        assertFalse(params.drawsOn(Overlay.Target.PREVIEW));
        assertFalse(params.drawsOn(Overlay.Target.PICTURE_SNAPSHOT));
        assertFalse(params.drawsOn(Overlay.Target.VIDEO_SNAPSHOT));

        params.drawOnPreview = true;
        assertTrue(params.drawsOn(Overlay.Target.PREVIEW));
        params.drawOnPictureSnapshot = true;
        assertTrue(params.drawsOn(Overlay.Target.PICTURE_SNAPSHOT));
        params.drawOnVideoSnapshot = true;
        assertTrue(params.drawsOn(Overlay.Target.VIDEO_SNAPSHOT));
    }

    @Test
    public void testLayoutParams_toString() {
        OverlayLayout.LayoutParams params = new OverlayLayout.LayoutParams(10, 10);
        String string = params.toString();
        assertTrue(string.contains("drawOnPreview"));
        assertTrue(string.contains("drawOnPictureSnapshot"));
        assertTrue(string.contains("drawOnVideoSnapshot"));
    }

    @Test
    public void testDrawChild() {
        Canvas canvas = new Canvas();
        OverlayLayout.LayoutParams params = new OverlayLayout.LayoutParams(10, 10);
        View child = new View(getContext());
        child.setLayoutParams(params);
        when(overlayLayout.doDrawChild(canvas, child, 0)).thenReturn(true);

        overlayLayout.currentTarget = Overlay.Target.PREVIEW;
        assertFalse(overlayLayout.drawChild(canvas, child, 0));
        params.drawOnPreview = true;
        assertTrue(overlayLayout.drawChild(canvas, child, 0));

        overlayLayout.currentTarget = Overlay.Target.PICTURE_SNAPSHOT;
        assertFalse(overlayLayout.drawChild(canvas, child, 0));
        params.drawOnPictureSnapshot = true;
        assertTrue(overlayLayout.drawChild(canvas, child, 0));

        overlayLayout.currentTarget = Overlay.Target.VIDEO_SNAPSHOT;
        assertFalse(overlayLayout.drawChild(canvas, child, 0));
        params.drawOnVideoSnapshot = true;
        assertTrue(overlayLayout.drawChild(canvas, child, 0));
    }

    @UiThreadTest
    @Test
    public void testDraw() {
        Canvas canvas = new Canvas();
        when(overlayLayout.drawsOn(Overlay.Target.PREVIEW)).thenReturn(false);
        overlayLayout.draw(canvas);
        verify(overlayLayout, never()).drawOn(Overlay.Target.PREVIEW, canvas);

        when(overlayLayout.drawsOn(Overlay.Target.PREVIEW)).thenReturn(true);
        overlayLayout.draw(canvas);
        verify(overlayLayout, times(1)).drawOn(Overlay.Target.PREVIEW, canvas);
    }

    @UiThreadTest
    @Test
    public void testDrawOn() {
        Canvas canvas = spy(new Canvas());
        View child = new View(getContext());
        OverlayLayout.LayoutParams params = new OverlayLayout.LayoutParams(10, 10);
        params.drawOnPreview = true;
        params.drawOnPictureSnapshot = true;
        params.drawOnVideoSnapshot = true;
        overlayLayout.addView(child, params);

        overlayLayout.drawOn(Overlay.Target.PREVIEW, canvas);
        verify(canvas, never()).scale(anyFloat(), anyFloat());
        verify(overlayLayout, times(1)).doDrawChild(eq(canvas), eq(child), anyLong());
        reset(canvas);
        reset(overlayLayout);

        overlayLayout.drawOn(Overlay.Target.PICTURE_SNAPSHOT, canvas);
        verify(canvas, times(1)).scale(anyFloat(), anyFloat());
        verify(overlayLayout, times(1)).doDrawChild(eq(canvas), eq(child), anyLong());
        reset(canvas);
        reset(overlayLayout);

        overlayLayout.drawOn(Overlay.Target.VIDEO_SNAPSHOT, canvas);
        verify(canvas, times(1)).scale(anyFloat(), anyFloat());
        verify(overlayLayout, times(1)).doDrawChild(eq(canvas), eq(child), anyLong());
        reset(canvas);
        reset(overlayLayout);
    }
}

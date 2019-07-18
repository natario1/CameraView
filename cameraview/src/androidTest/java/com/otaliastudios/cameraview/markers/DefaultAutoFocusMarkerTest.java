package com.otaliastudios.cameraview.markers;


import android.graphics.PointF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.annotation.UiThreadTest;

import com.otaliastudios.cameraview.BaseTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class DefaultAutoFocusMarkerTest extends BaseTest {

    private DefaultAutoFocusMarker marker;

    @Before
    public void setUp() {
        marker = new DefaultAutoFocusMarker();
    }

    @After
    public void tearDown() {
        marker = null;
    }

    @Test
    public void testOnAttach() {
        assertNull(marker.mContainer);
        assertNull(marker.mFill);
        ViewGroup container = new FrameLayout(getContext());
        View result = marker.onAttach(getContext(), container);
        assertNotNull(result);
        assertNotNull(marker.mContainer);
        assertNotNull(marker.mFill);
    }

    @UiThreadTest
    @Test
    public void testOnAutoFocusStart() {
        View mockContainer = spy(new View(getContext()));
        View mockFill = spy(new View(getContext()));
        marker.mContainer = mockContainer;
        marker.mFill = mockFill;
        marker.onAutoFocusStart(AutoFocusTrigger.GESTURE, new PointF());
        verify(mockContainer, atLeastOnce()).clearAnimation();
        verify(mockFill, atLeastOnce()).clearAnimation();
        verify(mockContainer, atLeastOnce()).animate();
        verify(mockFill, atLeastOnce()).animate();
    }

    @UiThreadTest
    @Test
    public void testOnAutoFocusStart_fromMethod() {
        View mockContainer = spy(new View(getContext()));
        View mockFill = spy(new View(getContext()));
        marker.mContainer = mockContainer;
        marker.mFill = mockFill;
        marker.onAutoFocusStart(AutoFocusTrigger.METHOD, new PointF());
        verify(mockContainer, never()).clearAnimation();
        verify(mockFill, never()).clearAnimation();
        verify(mockContainer, never()).animate();
        verify(mockFill, never()).animate();
    }

    @UiThreadTest
    @Test
    public void testOnAutoFocusEnd() {
        View mockContainer = spy(new View(getContext()));
        View mockFill = spy(new View(getContext()));
        marker.mContainer = mockContainer;
        marker.mFill = mockFill;
        marker.onAutoFocusEnd(AutoFocusTrigger.GESTURE, true, new PointF());
        verify(mockContainer, atLeastOnce()).animate();
        verify(mockFill, atLeastOnce()).animate();
    }

    @UiThreadTest
    @Test
    public void testOnAutoFocusEnd_fromMethod() {
        View mockContainer = spy(new View(getContext()));
        View mockFill = spy(new View(getContext()));
        marker.mContainer = mockContainer;
        marker.mFill = mockFill;
        marker.onAutoFocusEnd(AutoFocusTrigger.METHOD, true, new PointF());
        verify(mockContainer, never()).animate();
        verify(mockFill, never()).animate();
    }
}

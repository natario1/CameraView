package com.otaliastudios.cameraview.markers;


import android.annotation.TargetApi;
import android.content.Context;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.TestActivity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import androidx.test.annotation.UiThreadTest;
import androidx.test.rule.ActivityTestRule;

@TargetApi(17)
public class MarkerLayoutTest extends BaseTest {


    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    private MarkerLayout markerLayout;
    private AutoFocusMarker autoFocusMarker;

    @Before
    public void setUp() {
        uiSync(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                markerLayout = Mockito.spy(new MarkerLayout(a));
                a.inflate(markerLayout);
                autoFocusMarker = Mockito.spy(new DefaultAutoFocusMarker());
            }
        });
    }

    @Test
    @UiThreadTest
    public void testOnMarker_callsOnAttach() {
        markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
        Mockito.verify(autoFocusMarker, Mockito.times(1)).onAttach(
                Mockito.any(Context.class),
                Mockito.eq(markerLayout));
    }

    @Test
    @UiThreadTest
    public void testOnMarker_addsView() {
        Assert.assertEquals(markerLayout.getChildCount(), 0);
        markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
        Assert.assertEquals(markerLayout.getChildCount(), 1);
    }

    @Test
    @UiThreadTest
    public void testOnMarker_removesView() {
        markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
        Assert.assertEquals(markerLayout.getChildCount(), 1);
        markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
        Assert.assertEquals(markerLayout.getChildCount(), 1);
        markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, null);
        Assert.assertEquals(markerLayout.getChildCount(), 0);

        Mockito.verify(autoFocusMarker, Mockito.times(2)).onAttach(
                Mockito.any(Context.class),
                Mockito.eq(markerLayout));
    }
}

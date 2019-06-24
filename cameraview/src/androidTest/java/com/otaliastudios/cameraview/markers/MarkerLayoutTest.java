package com.otaliastudios.cameraview.markers;


import android.annotation.TargetApi;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.TestActivity;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureLayout;
import com.otaliastudios.cameraview.internal.utils.Task;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;

@TargetApi(17)
public class MarkerLayoutTest extends BaseTest {


    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    private MarkerLayout markerLayout;
    private AutoFocusMarker autoFocusMarker;

    @Before
    public void setUp() {
        ui(new Runnable() {
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
    public void testOnMarker_callsOnAttach() {
        markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
        Mockito.verify(autoFocusMarker, Mockito.times(1)).onAttach(
                Mockito.any(Context.class),
                Mockito.eq(markerLayout));
    }

    @Test
    public void testOnMarker_addsView() {
        Assert.assertEquals(markerLayout.getChildCount(), 0);
        markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
        Assert.assertEquals(markerLayout.getChildCount(), 1);
    }

    @Test
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

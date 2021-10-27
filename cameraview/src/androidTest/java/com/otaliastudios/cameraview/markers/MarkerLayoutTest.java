package com.otaliastudios.cameraview.markers;


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PointF;
import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.TestActivity;
import com.otaliastudios.cameraview.tools.SdkExclude;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Not clear why, but for some reason on API 28+ the UiThreadTests here crash for an internal NPE
 * in FrameLayout.onMeasure.
 */
@SdkExclude(minSdkVersion = 28)
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
                markerLayout = spy(new MarkerLayout(a));
                a.inflate(markerLayout);
                autoFocusMarker = spy(new DefaultAutoFocusMarker());
            }
        });
    }

    @Test
    public void testOnMarker_callsOnAttach() {
        uiSync(new Runnable() {
            @Override
            public void run() {
                markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
                Mockito.verify(autoFocusMarker, times(1)).onAttach(
                        Mockito.any(Context.class),
                        Mockito.eq(markerLayout));
            }
        });
    }

    @Test
    public void testOnMarker_addsView() {
        uiSync(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(markerLayout.getChildCount(), 0);
                markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
                Assert.assertEquals(markerLayout.getChildCount(), 1);
            }
        });
    }

    @Test
    public void testOnMarker_removesView() {
        uiSync(new Runnable() {
            @Override
            public void run() {
                markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
                Assert.assertEquals(markerLayout.getChildCount(), 1);
                markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, autoFocusMarker);
                Assert.assertEquals(markerLayout.getChildCount(), 1);
                markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, null);
                Assert.assertEquals(markerLayout.getChildCount(), 0);

                Mockito.verify(autoFocusMarker, times(2)).onAttach(
                        Mockito.any(Context.class),
                        Mockito.eq(markerLayout));
            }
        });
    }

    @Test
    public void testOnEvent() {
        uiSync(new Runnable() {
            @Override
            public void run() {
                final View mockView = spy(new View(getContext()));
                // These fail, however it's not really needed.
                // when(mockView.getWidth()).thenReturn(50);
                // when(mockView.getHeight()).thenReturn(50);
                AutoFocusMarker mockMarker = new AutoFocusMarker() {
                    public void onAutoFocusStart(@NonNull AutoFocusTrigger trigger, @NonNull PointF point) { }
                    public void onAutoFocusEnd(@NonNull AutoFocusTrigger trigger, boolean successful, @NonNull PointF point) { }

                    @Override
                    public View onAttach(@NonNull Context context, @NonNull ViewGroup container) {
                        return mockView;
                    }
                };
                markerLayout.onMarker(MarkerLayout.TYPE_AUTOFOCUS, mockMarker);
                reset(mockView);
                markerLayout.onEvent(MarkerLayout.TYPE_AUTOFOCUS, new PointF[]{new PointF(0, 0)});
                verify(mockView, times(1)).clearAnimation();
                verify(mockView, times(1)).setTranslationX(anyFloat());
                verify(mockView, times(1)).setTranslationY(anyFloat());
            }
        });
    }
}

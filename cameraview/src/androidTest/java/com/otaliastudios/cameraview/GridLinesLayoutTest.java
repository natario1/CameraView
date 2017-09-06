package com.otaliastudios.cameraview;


import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class GridLinesLayoutTest extends BaseTest {

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    private GridLinesLayout layout;

    @Before
    public void setUp() {
        ui(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                layout = new GridLinesLayout(a);
                layout.setGridMode(Grid.OFF);
                a.getContentView().addView(layout);
                layout.vert = mock(ColorDrawable.class);
                layout.horiz = mock(ColorDrawable.class);
            }
        });
    }

    @Test
    public void testOff() {
        layout.drawTask.listen();
        layout.setGridMode(Grid.OFF);
        layout.drawTask.await();
        verify(layout.vert, never()).draw(any(Canvas.class));
        verify(layout.horiz, never()).draw(any(Canvas.class));
    }

    @Test
    public void test3x3() {
        layout.drawTask.listen();
        layout.setGridMode(Grid.DRAW_3X3);
        layout.drawTask.await();
        verify(layout.vert, times(2)).draw(any(Canvas.class));
        verify(layout.horiz, times(2)).draw(any(Canvas.class));
    }

    @Test
    public void testPhi() {
        layout.drawTask.listen();
        layout.setGridMode(Grid.DRAW_PHI);
        layout.drawTask.await();
        verify(layout.vert, times(2)).draw(any(Canvas.class));
        verify(layout.horiz, times(2)).draw(any(Canvas.class));
    }

    @Test
    public void test4x4() {
        layout.drawTask.listen();
        layout.setGridMode(Grid.DRAW_4X4);
        layout.drawTask.await();
        verify(layout.vert, times(3)).draw(any(Canvas.class));
        verify(layout.horiz, times(3)).draw(any(Canvas.class));
    }

}

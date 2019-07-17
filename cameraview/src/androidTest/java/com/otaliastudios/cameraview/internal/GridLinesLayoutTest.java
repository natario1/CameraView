package com.otaliastudios.cameraview.internal;


import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.TestActivity;
import com.otaliastudios.cameraview.controls.Grid;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class GridLinesLayoutTest extends BaseTest {

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    private GridLinesLayout layout;

    @Before
    public void setUp() {
        uiSync(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                layout = new GridLinesLayout(a);
                layout.setGridMode(Grid.OFF);
                layout.drawOp.listen();
                a.getContentView().addView(layout);
            }
        });
        // Wait for first draw.
        layout.drawOp.await(1000);
    }

    private int setGridAndWait(Grid value) {
        layout.drawOp.listen();
        layout.setGridMode(value);
        Integer result = layout.drawOp.await(1000);
        assertNotNull(result);
        return result;
    }

    @Test
    public void testOff() {
        int linesDrawn = setGridAndWait(Grid.OFF);
        assertEquals(linesDrawn, 0);
    }

    @Test
    public void test3x3() {
        int linesDrawn = setGridAndWait(Grid.DRAW_3X3);
        assertEquals(linesDrawn, 2);
    }

    @Test
    public void testPhi() {
        int linesDrawn = setGridAndWait(Grid.DRAW_PHI);
        assertEquals(linesDrawn, 2);
    }

    @Test
    public void test4x4() {
        int linesDrawn = setGridAndWait(Grid.DRAW_4X4);
        assertEquals(linesDrawn, 3);
    }

}

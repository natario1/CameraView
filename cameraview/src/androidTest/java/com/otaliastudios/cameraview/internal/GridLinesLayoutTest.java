package com.otaliastudios.cameraview.internal;


import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.TestActivity;
import com.otaliastudios.cameraview.controls.Grid;
import com.otaliastudios.cameraview.tools.Op;
import com.otaliastudios.cameraview.tools.Retry;
import com.otaliastudios.cameraview.tools.RetryRule;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class GridLinesLayoutTest extends BaseTest {

    @Rule
    public ActivityTestRule<TestActivity> rule = new ActivityTestRule<>(TestActivity.class);

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    private GridLinesLayout layout;

    @NonNull
    private Op<Integer> getDrawOp() {
        final Op<Integer> op = new Op<>();
        layout.callback = mock(GridLinesLayout.DrawCallback.class);
        doEndOp(op, 0).when(layout.callback).onDraw(anyInt());
        return op;
    }

    @Before
    public void setUp() {
        uiSync(new Runnable() {
            @Override
            public void run() {
                TestActivity a = rule.getActivity();
                layout = new GridLinesLayout(a);
                layout.setGridMode(Grid.OFF);
                Op<Integer> op = getDrawOp();
                a.getContentView().addView(layout);
                op.await(1000);
            }
        });
    }

    private int setGridAndWait(Grid value) {
        layout.setGridMode(value);
        Op<Integer> op = getDrawOp();
        Integer result = op.await(1000);
        assertNotNull(result);
        return result;
    }

    @Retry
    @Test
    public void testOff() {
        int linesDrawn = setGridAndWait(Grid.OFF);
        assertEquals(0, linesDrawn);
    }

    @Retry
    @Test
    public void test3x3() {
        int linesDrawn = setGridAndWait(Grid.DRAW_3X3);
        assertEquals(2, linesDrawn);
    }

    @Retry
    @Test
    public void testPhi() {
        int linesDrawn = setGridAndWait(Grid.DRAW_PHI);
        assertEquals(2, linesDrawn);
    }

    @Retry
    @Test
    public void test4x4() {
        int linesDrawn = setGridAndWait(Grid.DRAW_4X4);
        assertEquals(3, linesDrawn);
    }

}

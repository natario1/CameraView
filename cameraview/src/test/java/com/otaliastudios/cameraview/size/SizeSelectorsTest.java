package com.otaliastudios.cameraview.size;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SizeSelectorsTest {

    private List<Size> input;

    @Before
    public void setUp() {
        input = Arrays.asList(
                new Size(100, 200),
                new Size(150, 300),
                new Size(600, 900),
                new Size(600, 600),
                new Size(1600, 900),
                new Size(30, 40),
                new Size(40, 30),
                new Size(2000, 4000)
        );
    }

    @Test
    public void testWithFilter() {
        SizeSelector selector = SizeSelectors.withFilter(new SizeSelectors.Filter() {
            @Override
            public boolean accepts(@NonNull Size size) {
                return size.getWidth() == 600;
            }
        });
        List<Size> list = selector.select(input);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0), new Size(600, 900));
        assertEquals(list.get(1), new Size(600, 600));
    }

    @Test
    public void testMaxWidth() {
        SizeSelector selector = SizeSelectors.maxWidth(50);
        List<Size> list = selector.select(input);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0), new Size(30, 40));
        assertEquals(list.get(1), new Size(40, 30));
    }

    @Test
    public void testMinWidth() {
        SizeSelector selector = SizeSelectors.minWidth(1000);
        List<Size> list = selector.select(input);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0), new Size(1600, 900));
        assertEquals(list.get(1), new Size(2000, 4000));
    }

    @Test
    public void testMaxHeight() {
        SizeSelector selector = SizeSelectors.maxHeight(50);
        List<Size> list = selector.select(input);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0), new Size(30, 40));
        assertEquals(list.get(1), new Size(40, 30));
    }

    @Test
    public void testMinHeight() {
        SizeSelector selector = SizeSelectors.minHeight(1000);
        List<Size> list = selector.select(input);
        assertEquals(list.size(), 1);
        assertEquals(list.get(0), new Size(2000, 4000));
    }

    @Test
    public void testAspectRatio() {
        SizeSelector selector = SizeSelectors.aspectRatio(AspectRatio.of(16, 9), 0);
        List<Size> list = selector.select(input);
        assertEquals(list.size(), 1);
        assertEquals(list.get(0), new Size(1600, 900));

        selector = SizeSelectors.aspectRatio(AspectRatio.of(1, 2), 0);
        list = selector.select(input);
        assertEquals(list.size(), 3);
        assertEquals(list.get(0), new Size(100, 200));
        assertEquals(list.get(1), new Size(150, 300));
        assertEquals(list.get(2), new Size(2000, 4000));
    }

    @Test
    public void testMax() {
        SizeSelector selector = SizeSelectors.biggest();
        List<Size> list = selector.select(input);
        assertEquals(list.size(), input.size());
        assertEquals(list.get(0), new Size(2000, 4000));
    }

    @Test
    public void testMin() {
        SizeSelector selector = SizeSelectors.smallest();
        List<Size> list = selector.select(input);
        assertEquals(list.size(), input.size());
        assertTrue(list.get(0).equals(new Size(30, 40)) || list.get(0).equals(new Size(40, 30)));
    }

    @Test
    public void testMaxArea() {
        SizeSelector selector = SizeSelectors.maxArea(100 * 100);
        List<Size> list = selector.select(input);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0), new Size(30, 40));
        assertEquals(list.get(1), new Size(40, 30));
    }

    @Test
    public void testMinArea() {
        SizeSelector selector = SizeSelectors.minArea(1000 * 1000); // 1 MP
        List<Size> list = selector.select(input);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0), new Size(1600, 900));
        assertEquals(list.get(1), new Size(2000, 4000));
    }

    @Test
    public void testAnd() {
        SizeSelector selector = SizeSelectors.and(
                SizeSelectors.aspectRatio(AspectRatio.of(1, 2), 0),
                SizeSelectors.maxWidth(100)
        );
        List<Size> list = selector.select(input);
        assertEquals(list.size(), 1);
        assertEquals(list.get(0), new Size(100, 200));
    }

    @Test
    public void testOrNotPassed() {
        SizeSelector mock = mock(SizeSelector.class);
        SizeSelector selector = SizeSelectors.or(
                SizeSelectors.aspectRatio(AspectRatio.of(1, 2), 0),
                mock
        );

        // The first gives some result so the second is not queried.
        selector.select(input);
        verify(mock, never()).select(anyListOf(Size.class));
    }

    @Test
    public void testOrPassed() {
        SizeSelector mock = mock(SizeSelector.class);
        SizeSelector selector = SizeSelectors.or(
                SizeSelectors.minHeight(600000),
                mock
        );

        // The first gives no result so the second is queried.
        selector.select(input);
        verify(mock, times(1)).select(anyListOf(Size.class));
    }
}

package com.otaliastudios.cameraview.filter;


import android.opengl.GLES20;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseEglTest;
import com.otaliastudios.cameraview.filters.AutoFixFilter;
import com.otaliastudios.cameraview.filters.BrightnessFilter;
import com.otaliastudios.cameraview.filters.DuotoneFilter;
import com.otaliastudios.cameraview.filters.VignetteFilter;
import com.otaliastudios.cameraview.internal.GlTextureDrawer;
import com.otaliastudios.opengl.program.GlProgram;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class MultiFilterTest extends BaseEglTest {

    @Test
    public void testConstructor1() {
        MultiFilter multiFilter = new MultiFilter(
                new DuotoneFilter(),
                new AutoFixFilter()
        );
        assertEquals(2, multiFilter.filters.size());
    }

    @Test
    public void testConstructor2() {
        List<Filter> filters = new ArrayList<>();
        filters.add(new DuotoneFilter());
        filters.add(new AutoFixFilter());
        MultiFilter multiFilter = new MultiFilter(filters);
        assertEquals(2, multiFilter.filters.size());
    }

    @Test
    public void testAddFilter() {
        MultiFilter multiFilter = new MultiFilter();
        assertEquals(0, multiFilter.filters.size());
        multiFilter.addFilter(new DuotoneFilter());
        assertEquals(1, multiFilter.filters.size());
        multiFilter.addFilter(new AutoFixFilter());
        assertEquals(2, multiFilter.filters.size());
    }

    @Test
    public void testAddFilter_multi() {
        MultiFilter multiFilter = new MultiFilter(new DuotoneFilter());
        assertEquals(1, multiFilter.filters.size());
        MultiFilter other = new MultiFilter(
                new AutoFixFilter(),
                new BrightnessFilter(),
                new VignetteFilter());
        multiFilter.addFilter(other);
        assertEquals(4, multiFilter.filters.size());
    }

    @Test
    public void testSetSize() {
        DuotoneFilter filter = new DuotoneFilter();
        MultiFilter multiFilter = new MultiFilter(filter);
        MultiFilter.State state = multiFilter.states.get(filter);
        assertNotNull(state);
        assertNull(state.size);
        multiFilter.setSize(WIDTH, HEIGHT);
        assertNotNull(state.size);
    }

    @Test
    public void testCopy() {
        DuotoneFilter filter = spy(new DuotoneFilter());
        MultiFilter multiFilter = new MultiFilter(filter);
        MultiFilter multiFilterCopy = (MultiFilter) multiFilter.copy();
        assertEquals(1, multiFilterCopy.filters.size());
        verify(filter, times(1)).onCopy();
    }

    @Test
    public void testParameter1() {
        DuotoneFilter filter = new DuotoneFilter();
        MultiFilter multiFilter = new MultiFilter(filter);
        float desired = 0.21582F; // whatever
        multiFilter.setParameter1(desired);
        assertEquals(desired, multiFilter.getParameter1(), 0.001F);
        assertEquals(desired, filter.getParameter1(), 0.001F);
    }

    @Test
    public void testParameter2() {
        DuotoneFilter filter = new DuotoneFilter();
        MultiFilter multiFilter = new MultiFilter(filter);
        float desired = 0.21582F; // whatever
        multiFilter.setParameter2(desired);
        assertEquals(desired, multiFilter.getParameter2(), 0.001F);
        assertEquals(desired, filter.getParameter2(), 0.001F);
    }

    @Test
    public void testOnCreate_isLazy() {
        DuotoneFilter filter = spy(new DuotoneFilter());
        MultiFilter multiFilter = new MultiFilter(filter);

        int program = GlProgram.create(multiFilter.getVertexShader(),
                multiFilter.getFragmentShader());
        multiFilter.onCreate(program);
        verify(filter, never()).onCreate(anyInt());

        multiFilter.onDestroy();
        GLES20.glDeleteProgram(program);
        verify(filter, never()).onDestroy();
    }

    @Test
    public void testDraw_simple() {
        DuotoneFilter filter = spy(new DuotoneFilter());
        MultiFilter multiFilter = new MultiFilter(filter);
        multiFilter.setSize(WIDTH, HEIGHT);
        GlTextureDrawer drawer = new GlTextureDrawer();
        drawer.setFilter(multiFilter);
        float[] matrix = drawer.getTextureTransform();
        drawer.draw(0L);
        drawer.release();

        // The child should have experienced the whole lifecycle.
        verify(filter, atLeastOnce()).getVertexShader();
        verify(filter, atLeastOnce()).getFragmentShader();
        verify(filter, atLeastOnce()).setSize(anyInt(), anyInt());
        verify(filter, times(1)).onCreate(anyInt());
        verify(filter, times(1)).draw(0L, matrix);
        verify(filter, times(1)).onDestroy();
    }

    @Test
    public void testDraw_multi() {
        // Want to test that when filter1 is drawn, the current framebuffer is a
        // non-default one. When filter2 is drawn, the current framebuffer is 0.
        final DuotoneFilter filter1 = spy(new DuotoneFilter());
        final DuotoneFilter filter2 = spy(new DuotoneFilter());
        final MultiFilter multiFilter = new MultiFilter(filter1, filter2);
        multiFilter.setSize(WIDTH, HEIGHT);
        GlTextureDrawer drawer = new GlTextureDrawer();
        drawer.setFilter(multiFilter);
        float[] matrix = drawer.getTextureTransform();
        final int[] result = new int[1];

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                MultiFilter.State state = multiFilter.states.get(filter1);
                assertNotNull(state);
                assertTrue(state.isProgramCreated);
                assertTrue(state.isFramebufferCreated);

                GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, result, 0);
                // assertTrue(result[0] != 0);
                return null;
            }
        }).when(filter1).draw(0L, matrix);

        // Note: second filter is drawn with the identity matrix!
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                // The last filter has no FBO / texture.
                MultiFilter.State state = multiFilter.states.get(filter2);
                assertNotNull(state);
                assertTrue(state.isProgramCreated);
                assertFalse(state.isFramebufferCreated);

                GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, result, 0);
                assertEquals(0, result[0]);
                return null;

            }
        }).when(filter2).draw(eq(0L), any(float[].class));

        drawer.draw(0L);
        drawer.release();

        // Verify that both are drawn.
        verify(filter1, times(1)).draw(0L, matrix);
        verify(filter2, times(1)).draw(eq(0L), any(float[].class));
    }

}

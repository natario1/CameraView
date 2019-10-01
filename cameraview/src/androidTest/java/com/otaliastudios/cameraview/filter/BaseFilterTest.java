package com.otaliastudios.cameraview.filter;


import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseEglTest;
import com.otaliastudios.cameraview.internal.GlUtils;
import com.otaliastudios.cameraview.internal.egl.EglViewport;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class BaseFilterTest extends BaseEglTest {

    public static class TestFilter extends BaseFilter implements TwoParameterFilter {

        private float param1;
        private float param2;

        @NonNull
        @Override
        public String getFragmentShader() {
            return createDefaultFragmentShader();
        }

        @Override
        public void setParameter1(float value) {
            param1 = value;
        }

        @Override
        public void setParameter2(float value) {
            param2 = value;
        }

        @Override
        public float getParameter1() {
            return param1;
        }

        @Override
        public float getParameter2() {
            return param2;
        }
    }

    private TestFilter filter;

    @Test
    public void testCreateDefaultFragmentShader() {
        filter = new TestFilter();
        filter.fragmentTextureCoordinateName = "XXX";
        String defaultFragmentShader = filter.createDefaultFragmentShader();
        assertNotNull(defaultFragmentShader);
        assertTrue(defaultFragmentShader.contains(filter.fragmentTextureCoordinateName));
    }

    @Test
    public void testCreateDefaultVertexShader() {
        filter = new TestFilter();
        filter.vertexModelViewProjectionMatrixName = "AAA";
        filter.vertexPositionName = "BBB";
        filter.vertexTextureCoordinateName = "CCC";
        filter.vertexTransformMatrixName = "DDD";
        filter.fragmentTextureCoordinateName = "EEE";
        String defaultVertexShader = filter.createDefaultVertexShader();
        assertNotNull(defaultVertexShader);
        assertTrue(defaultVertexShader.contains(filter.vertexModelViewProjectionMatrixName));
        assertTrue(defaultVertexShader.contains(filter.vertexPositionName));
        assertTrue(defaultVertexShader.contains(filter.vertexTextureCoordinateName));
        assertTrue(defaultVertexShader.contains(filter.vertexTransformMatrixName));
        assertTrue(defaultVertexShader.contains(filter.fragmentTextureCoordinateName));
    }

    @Test
    public void testOnProgramCreate() {
        filter = new TestFilter();
        int handle = GlUtils.createProgram(filter.getVertexShader(), filter.getFragmentShader());
        filter.onCreate(handle);
        assertTrue(filter.programHandle >= 0);
        filter.onDestroy();
        assertTrue(filter.programHandle < 0);
        GLES20.glDeleteProgram(handle);
    }

    @Test
    public void testDraw_whenInvalid() {
        filter = spy(new TestFilter());
        float[] matrix = new float[16];
        filter.draw(0L, matrix);
        verify(filter, never()).onPreDraw(0L, matrix);
        verify(filter, never()).onDraw(0L);
        verify(filter, never()).onPostDraw(0L);
    }

    @Test
    public void testDraw() {
        // Use an EglViewport which cares about GL setup.
        filter = spy(new TestFilter());
        EglViewport viewport = new EglViewport(filter);
        int texture = viewport.createTexture();

        float[] matrix = new float[16];
        viewport.drawFrame(0L, texture, matrix);
        verify(filter, times(1)).onPreDraw(0L, matrix);
        verify(filter, times(1)).onDraw(0L);
        verify(filter, times(1)).onPostDraw(0L);

        viewport.release();
    }

    @Test(expected = RuntimeException.class)
    public void testOnCopy_invalid() {
        // Anonymous inner classes do not have a public constructor.
        Filter filter = new BaseFilter() {
            @NonNull
            @Override
            public String getFragmentShader() {
                return "whatever";
            }
        };
        filter.copy();
    }

    @Test
    public void testOnCopy() {
        filter = new TestFilter();
        BaseFilter other = filter.copy();
        assertTrue(other instanceof TestFilter);
    }

    @Test
    public void testCopy_withSize() {
        filter = new TestFilter();
        filter.setSize(WIDTH, HEIGHT);
        BaseFilter other = filter.copy();
        assertEquals(WIDTH, other.size.getWidth());
        assertEquals(HEIGHT, other.size.getHeight());
    }

    @Test
    public void testCopy_withParameter1() {
        filter = new TestFilter();
        filter.setParameter1(0.5F);
        TestFilter other = (TestFilter) filter.copy();
        assertEquals(filter.getParameter1(), other.getParameter1(), 0.001F);
    }

    @Test
    public void testCopy_withParameter2() {
        filter = new TestFilter();
        filter.setParameter2(0.5F);
        TestFilter other = (TestFilter) filter.copy();
        assertEquals(filter.getParameter2(), other.getParameter2(), 0.001F);
    }
}

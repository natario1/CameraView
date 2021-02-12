package com.otaliastudios.cameraview.filter;


import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class SimpleFilterTest extends BaseTest {

    @Test
    public void testGetFragmentShader() {
        String shader = "shader";
        SimpleFilter filter = new SimpleFilter(shader);
        assertEquals(shader, filter.getFragmentShader());
    }

    @Test
    public void testCopy() {
        String shader = "shader";
        SimpleFilter filter = new SimpleFilter(shader);
        BaseFilter copy = filter.copy();
        assertTrue(copy instanceof SimpleFilter);
        SimpleFilter simpleCopy = (SimpleFilter) copy;
        assertEquals(shader, simpleCopy.getFragmentShader());
    }
}

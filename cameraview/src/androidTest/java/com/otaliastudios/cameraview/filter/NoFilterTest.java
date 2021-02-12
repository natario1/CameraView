package com.otaliastudios.cameraview.filter;


import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class NoFilterTest extends BaseTest {

    public static class DummyFilter extends BaseFilter {
        @NonNull
        @Override
        public String getFragmentShader() {
            return "whatever";
        }
    }

    @Test
    public void testGetFragmentShader() {
        NoFilter filter = new NoFilter();
        String defaultFragmentShader = new DummyFilter().createDefaultFragmentShader();
        assertEquals(defaultFragmentShader, filter.getFragmentShader());
    }
}

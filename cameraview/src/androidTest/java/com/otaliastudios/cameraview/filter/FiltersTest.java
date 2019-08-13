package com.otaliastudios.cameraview.filter;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class FiltersTest extends BaseTest {

    @Test
    public void testNewInstance() {
        // At least tests that all our default filters have a no-args constructor.
        Filters[] filtersArray = Filters.values();
        for (Filters filters : filtersArray) {
            Filter filter = filters.newInstance();
            assertNotNull(filter);
        }
    }
}

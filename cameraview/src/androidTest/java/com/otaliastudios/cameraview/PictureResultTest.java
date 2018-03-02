package com.otaliastudios.cameraview;


import android.location.Location;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class PictureResultTest extends BaseTest {

    private PictureResult result = new PictureResult();

    @Test
    public void testResult() {
        int rotation = 90;
        Size size = new Size(20, 120);
        byte[] jpeg = new byte[]{2, 4, 1, 5, 2};
        Location location = Mockito.mock(Location.class);
        boolean isSnapshot = true;

        result.rotation = rotation;
        result.size = size;
        result.jpeg = jpeg;
        result.location = location;
        result.isSnapshot = isSnapshot;

        assertEquals(result.getRotation(), rotation);
        assertEquals(result.getSize(), size);
        assertEquals(result.getJpeg(), jpeg);
        assertEquals(result.getLocation(), location);
        assertEquals(result.isSnapshot(), isSnapshot);
    }
}

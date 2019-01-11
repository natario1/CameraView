package com.otaliastudios.cameraview;


import android.location.Location;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class PictureResultTest extends BaseTest {

    private PictureResult result = new PictureResult();

    @Test
    public void testResult() {
        int format = PictureResult.FORMAT_JPEG;
        int rotation = 90;
        Size size = new Size(20, 120);
        byte[] jpeg = new byte[]{2, 4, 1, 5, 2};
        Location location = Mockito.mock(Location.class);
        boolean isSnapshot = true;
        Facing facing = Facing.FRONT;

        result. format = format;
        result.rotation = rotation;
        result.size = size;
        result.data = jpeg;
        result.location = location;
        result.facing = facing;
        //noinspection ConstantConditions
        result.isSnapshot = isSnapshot;

        assertEquals(result.getFormat(), format);
        assertEquals(result.getRotation(), rotation);
        assertEquals(result.getSize(), size);
        assertEquals(result.getData(), jpeg);
        assertEquals(result.getLocation(), location);
        //noinspection ConstantConditions
        assertEquals(result.isSnapshot(), isSnapshot);
        assertEquals(result.getFacing(), facing);
    }
}

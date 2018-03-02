package com.otaliastudios.cameraview;


import android.hardware.Camera;
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
public class VideoResultTest extends BaseTest {

    private VideoResult result = new VideoResult();

    @Test
    public void testResult() {
        File file = Mockito.mock(File.class);
        int rotation = 90;
        Size size = new Size(20, 120);
        VideoCodec codec = VideoCodec.H_263;
        Location location = Mockito.mock(Location.class);
        boolean isSnapshot = true;

        result.file = file;
        result.rotation = rotation;
        result.size = size;
        result.codec = codec;
        result.location = location;
        result.isSnapshot = isSnapshot;

        assertEquals(result.getFile(), file);
        assertEquals(result.getRotation(), rotation);
        assertEquals(result.getSize(), size);
        assertEquals(result.getCodec(), codec);
        assertEquals(result.getLocation(), location);
        assertEquals(result.isSnapshot(), isSnapshot);
    }
}

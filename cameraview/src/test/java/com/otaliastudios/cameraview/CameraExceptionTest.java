package com.otaliastudios.cameraview;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CameraExceptionTest {

    @Test
    public void testConstructor() {
        RuntimeException cause = new RuntimeException("Error");
        CameraException camera = new CameraException(cause);
        assertEquals(cause, camera.getCause());
    }
}

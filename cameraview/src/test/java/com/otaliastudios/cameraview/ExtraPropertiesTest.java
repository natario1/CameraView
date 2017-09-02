package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.util.SizeF;

import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ExtraPropertiesTest {

    @Test
    public void testConstructor1() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getVerticalViewAngle()).thenReturn(10f);
        when(params.getHorizontalViewAngle()).thenReturn(5f);
        ExtraProperties e = new ExtraProperties(params);
        assertEquals(e.getVerticalViewingAngle(), 10f, 0f);
        assertEquals(e.getHorizontalViewingAngle(), 5f, 0f);
    }

}

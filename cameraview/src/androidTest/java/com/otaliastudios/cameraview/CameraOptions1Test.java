package com.otaliastudios.cameraview;


import android.hardware.Camera;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.InstrumentationTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraOptions1Test {

    @Test
    public void testEmpty() {
        CameraOptions o = new CameraOptions(mock(Camera.Parameters.class));
        assertTrue(o.getSupportedWhiteBalance().isEmpty());
        assertTrue(o.getSupportedFlash().isEmpty());
        assertTrue(o.getSupportedHdr().isEmpty());
        assertFalse(o.isAutoFocusSupported());
        assertFalse(o.isExposureCorrectionSupported());
        assertFalse(o.isVideoSnapshotSupported());
        assertFalse(o.isZoomSupported());
        assertEquals(o.getExposureCorrectionMaxValue(), 0f, 0);
        assertEquals(o.getExposureCorrectionMinValue(), 0f, 0);
    }

    @Test
    public void testFacing() {
        Set<Integer> supported = new HashSet<>();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            supported.add(cameraInfo.facing);
        }

        CameraOptions o = new CameraOptions(mock(Camera.Parameters.class));
        Mapper m = new Mapper.Mapper1();
        Set<Facing> s = o.getSupportedFacing();
        assertEquals(o.getSupportedFacing().size(), supported.size());
        for (Facing facing : s) {
            assertTrue(supported.contains(m.<Integer>map(facing)));
        }
    }

    @Test
    public void testWhiteBalance() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedWhiteBalance()).thenReturn(Arrays.asList(
                Camera.Parameters.WHITE_BALANCE_AUTO, // Supported
                Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT, // Supported
                Camera.Parameters.WHITE_BALANCE_SHADE // Not supported
        ));

        CameraOptions o = new CameraOptions(params);
        assertEquals(o.getSupportedWhiteBalance().size(), 2);
        assertTrue(o.getSupportedWhiteBalance().contains(WhiteBalance.AUTO));
        assertTrue(o.getSupportedWhiteBalance().contains(WhiteBalance.CLOUDY));
        assertTrue(o.supports(WhiteBalance.AUTO));
        assertTrue(o.supports(WhiteBalance.CLOUDY));
    }

    @Test
    public void testFlash() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedFlashModes()).thenReturn(Arrays.asList(
                Camera.Parameters.FLASH_MODE_AUTO, // Supported
                Camera.Parameters.FLASH_MODE_TORCH, // Supported
                Camera.Parameters.FLASH_MODE_RED_EYE // Not supported
        ));

        CameraOptions o = new CameraOptions(params);
        assertEquals(o.getSupportedFlash().size(), 2);
        assertTrue(o.getSupportedFlash().contains(Flash.AUTO));
        assertTrue(o.getSupportedFlash().contains(Flash.TORCH));
        assertTrue(o.supports(Flash.AUTO));
        assertTrue(o.supports(Flash.TORCH));
    }

    @Test
    public void testHdr() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedSceneModes()).thenReturn(Arrays.asList(
                Camera.Parameters.SCENE_MODE_AUTO, // Supported
                Camera.Parameters.SCENE_MODE_HDR, // Supported
                Camera.Parameters.SCENE_MODE_FIREWORKS // Not supported
        ));

        CameraOptions o = new CameraOptions(params);
        assertEquals(o.getSupportedHdr().size(), 2);
        assertTrue(o.getSupportedHdr().contains(Hdr.OFF));
        assertTrue(o.getSupportedHdr().contains(Hdr.ON));
        assertTrue(o.supports(Hdr.OFF));
        assertTrue(o.supports(Hdr.ON));
    }

    @Test
    public void testBooleanFlags() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.isVideoSnapshotSupported()).thenReturn(true);
        when(params.isZoomSupported()).thenReturn(true);
        when(params.getSupportedFocusModes()).thenReturn(Arrays.asList(Camera.Parameters.FOCUS_MODE_AUTO));
        CameraOptions o = new CameraOptions(params);
        assertTrue(o.isVideoSnapshotSupported());
        assertTrue(o.isZoomSupported());
        assertTrue(o.isAutoFocusSupported());
    }

    @Test
    public void testExposureCorrection() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getMaxExposureCompensation()).thenReturn(10);
        when(params.getMinExposureCompensation()).thenReturn(-10);
        when(params.getExposureCompensationStep()).thenReturn(0.5f);
        CameraOptions o = new CameraOptions(params);
        assertTrue(o.isExposureCorrectionSupported());
        assertEquals(o.getExposureCorrectionMinValue(), -10f * 0.5f, 0f);
        assertEquals(o.getExposureCorrectionMaxValue(), 10f * 0.5f, 0f);
    }

}

package com.otaliastudios.cameraview;


import android.hardware.Camera;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class Mapper1Test extends BaseTest {

    private Mapper mapper = new Mapper1();

    @Test
    public void testMap() {
        assertEquals(mapper.map(Flash.OFF), Camera.Parameters.FLASH_MODE_OFF);
        assertEquals(mapper.map(Flash.ON), Camera.Parameters.FLASH_MODE_ON);
        assertEquals(mapper.map(Flash.AUTO), Camera.Parameters.FLASH_MODE_AUTO);
        assertEquals(mapper.map(Flash.TORCH), Camera.Parameters.FLASH_MODE_TORCH);

        assertEquals(mapper.map(Facing.BACK), Camera.CameraInfo.CAMERA_FACING_BACK);
        assertEquals(mapper.map(Facing.FRONT), Camera.CameraInfo.CAMERA_FACING_FRONT);

        assertEquals(mapper.map(Hdr.OFF), Camera.Parameters.SCENE_MODE_AUTO);
        assertEquals(mapper.map(Hdr.ON), Camera.Parameters.SCENE_MODE_HDR);

        assertEquals(mapper.map(WhiteBalance.AUTO), Camera.Parameters.WHITE_BALANCE_AUTO);
        assertEquals(mapper.map(WhiteBalance.DAYLIGHT), Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
        assertEquals(mapper.map(WhiteBalance.CLOUDY), Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
        assertEquals(mapper.map(WhiteBalance.INCANDESCENT), Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
        assertEquals(mapper.map(WhiteBalance.FLUORESCENT), Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
    }


    @Test
    public void testUnmap() {
        assertEquals(Flash.OFF, mapper.unmapFlash(Camera.Parameters.FLASH_MODE_OFF));
        assertEquals(Flash.ON, mapper.unmapFlash(Camera.Parameters.FLASH_MODE_ON));
        assertEquals(Flash.AUTO, mapper.unmapFlash(Camera.Parameters.FLASH_MODE_AUTO));
        assertEquals(Flash.TORCH, mapper.unmapFlash(Camera.Parameters.FLASH_MODE_TORCH));

        assertEquals(Facing.BACK, mapper.unmapFacing(Camera.CameraInfo.CAMERA_FACING_BACK));
        assertEquals(Facing.FRONT, mapper.unmapFacing(Camera.CameraInfo.CAMERA_FACING_FRONT));

        assertEquals(Hdr.OFF, mapper.unmapHdr(Camera.Parameters.SCENE_MODE_AUTO));
        assertEquals(Hdr.ON, mapper.unmapHdr(Camera.Parameters.SCENE_MODE_HDR));

        assertEquals(WhiteBalance.AUTO, mapper.unmapWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO));
        assertEquals(WhiteBalance.DAYLIGHT, mapper.unmapWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT));
        assertEquals(WhiteBalance.CLOUDY, mapper.unmapWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT));
        assertEquals(WhiteBalance.INCANDESCENT, mapper.unmapWhiteBalance(Camera.Parameters.WHITE_BALANCE_INCANDESCENT));
        assertEquals(WhiteBalance.FLUORESCENT, mapper.unmapWhiteBalance(Camera.Parameters.WHITE_BALANCE_FLUORESCENT));
    }
}

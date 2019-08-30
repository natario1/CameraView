package com.otaliastudios.cameraview.engine.mappers;


import android.hardware.Camera;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.engine.mappers.Camera1Mapper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class Camera1MapperTest extends BaseTest {

    private Camera1Mapper mapper = Camera1Mapper.get();

    @Test
    public void testMap() {
        assertEquals(mapper.mapFlash(Flash.OFF), Camera.Parameters.FLASH_MODE_OFF);
        assertEquals(mapper.mapFlash(Flash.ON), Camera.Parameters.FLASH_MODE_ON);
        assertEquals(mapper.mapFlash(Flash.AUTO), Camera.Parameters.FLASH_MODE_AUTO);
        assertEquals(mapper.mapFlash(Flash.TORCH), Camera.Parameters.FLASH_MODE_TORCH);

        assertEquals(mapper.mapFacing(Facing.BACK), Camera.CameraInfo.CAMERA_FACING_BACK);
        assertEquals(mapper.mapFacing(Facing.FRONT), Camera.CameraInfo.CAMERA_FACING_FRONT);

        assertEquals(mapper.mapHdr(Hdr.OFF), Camera.Parameters.SCENE_MODE_AUTO);
        assertEquals(mapper.mapHdr(Hdr.ON), Camera.Parameters.SCENE_MODE_HDR);

        assertEquals(mapper.mapWhiteBalance(WhiteBalance.AUTO), Camera.Parameters.WHITE_BALANCE_AUTO);
        assertEquals(mapper.mapWhiteBalance(WhiteBalance.DAYLIGHT), Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
        assertEquals(mapper.mapWhiteBalance(WhiteBalance.CLOUDY), Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
        assertEquals(mapper.mapWhiteBalance(WhiteBalance.INCANDESCENT), Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
        assertEquals(mapper.mapWhiteBalance(WhiteBalance.FLUORESCENT), Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
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

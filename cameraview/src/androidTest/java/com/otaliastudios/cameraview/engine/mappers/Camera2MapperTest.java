package com.otaliastudios.cameraview.engine.mappers;


import android.hardware.Camera;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.WhiteBalance;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;

import static android.hardware.camera2.CameraMetadata.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



@RunWith(AndroidJUnit4.class)
@SmallTest
public class Camera2MapperTest extends BaseTest {

    private Camera2Mapper mapper = Camera2Mapper.get();

    @Test
    public void testMap() {
        List<Pair<Integer, Integer>> values = mapper.mapFlash(Flash.OFF);
        assertEquals(2, values.size());
        assertTrue(values.contains(new Pair<>(CONTROL_AE_MODE_ON, FLASH_MODE_OFF)));
        assertTrue(values.contains(new Pair<>(CONTROL_AE_MODE_OFF, FLASH_MODE_OFF)));
        values = mapper.mapFlash(Flash.TORCH);
        assertEquals(2, values.size());
        assertTrue(values.contains(new Pair<>(CONTROL_AE_MODE_ON, FLASH_MODE_TORCH)));
        assertTrue(values.contains(new Pair<>(CONTROL_AE_MODE_OFF, FLASH_MODE_TORCH)));
        values = mapper.mapFlash(Flash.AUTO);
        assertEquals(2, values.size());
        assertTrue(values.contains(new Pair<>(CONTROL_AE_MODE_ON_AUTO_FLASH, FLASH_MODE_OFF)));
        assertTrue(values.contains(new Pair<>(CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE, FLASH_MODE_OFF)));
        values = mapper.mapFlash(Flash.ON);
        assertEquals(1, values.size());
        assertTrue(values.contains(new Pair<>(CONTROL_AE_MODE_ON_ALWAYS_FLASH, FLASH_MODE_OFF)));

        assertEquals(mapper.mapFacing(Facing.BACK), LENS_FACING_BACK);
        assertEquals(mapper.mapFacing(Facing.FRONT), LENS_FACING_FRONT);

        assertEquals(mapper.mapHdr(Hdr.OFF), CONTROL_SCENE_MODE_DISABLED);
        assertEquals(mapper.mapHdr(Hdr.ON), CONTROL_SCENE_MODE_HDR);

        assertEquals(mapper.mapWhiteBalance(WhiteBalance.AUTO), CONTROL_AWB_MODE_AUTO);
        assertEquals(mapper.mapWhiteBalance(WhiteBalance.DAYLIGHT), CONTROL_AWB_MODE_DAYLIGHT);
        assertEquals(mapper.mapWhiteBalance(WhiteBalance.CLOUDY), CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
        assertEquals(mapper.mapWhiteBalance(WhiteBalance.INCANDESCENT), CONTROL_AWB_MODE_INCANDESCENT);
        assertEquals(mapper.mapWhiteBalance(WhiteBalance.FLUORESCENT), CONTROL_AWB_MODE_FLUORESCENT);
    }


    @Test
    public void testUnmap() {
        Set<Flash> values;
        values = mapper.unmapFlash(CONTROL_AE_MODE_OFF);
        assertEquals(values.size(), 2);
        assertTrue(values.contains(Flash.OFF));
        assertTrue(values.contains(Flash.TORCH));
        values = mapper.unmapFlash(CONTROL_AE_MODE_ON);
        assertEquals(values.size(), 2);
        assertTrue(values.contains(Flash.OFF));
        assertTrue(values.contains(Flash.TORCH));
        values = mapper.unmapFlash(CONTROL_AE_MODE_ON_ALWAYS_FLASH);
        assertEquals(values.size(), 1);
        assertTrue(values.contains(Flash.ON));
        values = mapper.unmapFlash(CONTROL_AE_MODE_ON_AUTO_FLASH);
        assertEquals(values.size(), 1);
        assertTrue(values.contains(Flash.AUTO));
        values = mapper.unmapFlash(CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
        assertEquals(values.size(), 1);
        assertTrue(values.contains(Flash.AUTO));
        values = mapper.unmapFlash(CONTROL_AE_MODE_ON_EXTERNAL_FLASH);
        assertEquals(values.size(), 0);

        assertEquals(Facing.BACK, mapper.unmapFacing(LENS_FACING_BACK));
        assertEquals(Facing.FRONT, mapper.unmapFacing(LENS_FACING_FRONT));

        assertEquals(Hdr.OFF, mapper.unmapHdr(CONTROL_SCENE_MODE_DISABLED));
        assertEquals(Hdr.ON, mapper.unmapHdr(CONTROL_SCENE_MODE_HDR));

        assertEquals(WhiteBalance.AUTO, mapper.unmapWhiteBalance(CONTROL_AWB_MODE_AUTO));
        assertEquals(WhiteBalance.DAYLIGHT, mapper.unmapWhiteBalance(CONTROL_AWB_MODE_DAYLIGHT));
        assertEquals(WhiteBalance.CLOUDY, mapper.unmapWhiteBalance(CONTROL_AWB_MODE_CLOUDY_DAYLIGHT));
        assertEquals(WhiteBalance.INCANDESCENT, mapper.unmapWhiteBalance(CONTROL_AWB_MODE_INCANDESCENT));
        assertEquals(WhiteBalance.FLUORESCENT, mapper.unmapWhiteBalance(CONTROL_AWB_MODE_FLUORESCENT));
    }
}

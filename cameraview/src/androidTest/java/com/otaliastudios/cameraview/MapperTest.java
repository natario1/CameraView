package com.otaliastudios.cameraview;


import android.hardware.Camera;
import android.media.MediaRecorder;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class MapperTest extends BaseTest {

    private Mapper mapper = new Mapper() {
        <T> T map(Flash flash) { return null; }
        <T> T map(Facing facing) { return null; }
        <T> T map(WhiteBalance whiteBalance) { return null; }
        <T> T map(Hdr hdr) { return null; }
        <T> Flash unmapFlash(T cameraConstant) { return null; }
        <T> Facing unmapFacing(T cameraConstant) { return null; }
        <T> WhiteBalance unmapWhiteBalance(T cameraConstant) { return null; }
        <T> Hdr unmapHdr(T cameraConstant) { return null; }
    };

    @Test
    public void testMap() {
        assertEquals(mapper.map(VideoCodec.DEVICE_DEFAULT), MediaRecorder.VideoEncoder.DEFAULT);
        assertEquals(mapper.map(VideoCodec.H_263), MediaRecorder.VideoEncoder.H263);
        assertEquals(mapper.map(VideoCodec.H_264), MediaRecorder.VideoEncoder.H264);
    }
}

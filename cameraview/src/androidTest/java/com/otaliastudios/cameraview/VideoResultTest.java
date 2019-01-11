package com.otaliastudios.cameraview;


import android.location.Location;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

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
        int maxDuration = 1234;
        long maxFileSize = 500000;
        int reason = VideoResult.REASON_MAX_DURATION_REACHED;
        int videoFrameRate = 30;
        int videoBitRate = 300000;
        int audioBitRate = 30000;
        Audio audio = Audio.ON;
        Facing facing = Facing.FRONT;

        result.file = file;
        result.rotation = rotation;
        result.size = size;
        result.codec = codec;
        result.location = location;
        result.isSnapshot = isSnapshot;
        result.maxDuration = maxDuration;
        result.maxSize = maxFileSize;
        result.endReason = reason;
        result.videoFrameRate = videoFrameRate;
        result.videoBitRate = videoBitRate;
        result.audioBitRate = audioBitRate;
        result.audio = audio;
        result.facing = facing;

        assertEquals(result.getFile(), file);
        assertEquals(result.getRotation(), rotation);
        assertEquals(result.getSize(), size);
        assertEquals(result.getVideoCodec(), codec);
        assertEquals(result.getLocation(), location);
        assertEquals(result.isSnapshot(), isSnapshot);
        assertEquals(result.getMaxSize(), maxFileSize);
        assertEquals(result.getMaxDuration(), maxDuration);
        assertEquals(result.getTerminationReason(), reason);
        assertEquals(result.getVideoFrameRate(), videoFrameRate);
        assertEquals(result.getVideoBitRate(), videoBitRate);
        assertEquals(result.getAudioBitRate(), audioBitRate);
        assertEquals(result.getAudio(), audio);
        assertEquals(result.getFacing(), facing);

    }
}

package com.otaliastudios.cameraview.internal;


import android.media.MediaCodecInfo;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class DeviceEncodersTest extends BaseTest {

    // This is guaranteed to work, see
    // https://developer.android.com/guide/topics/media/media-formats
    private final static Size GUARANTEED_SIZE = new Size(176, 144);

    private boolean enabled;

    @Before
    public void setUp() {
        enabled = DeviceEncoders.ENABLED;
    }

    @After
    public void tearDown() {
        DeviceEncoders.ENABLED = enabled;
    }

    @NonNull
    private DeviceEncoders create() {
        return new DeviceEncoders(DeviceEncoders.MODE_RESPECT_ORDER,
                "video/avc",
                "audio/mp4a-latm",
                0,
                0);
    }

    @Test
    public void testGetDeviceEncoders() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            List<MediaCodecInfo> infos = deviceEncoders.getDeviceEncoders();
            for (MediaCodecInfo info : infos) {
                assertTrue(info.isEncoder());
            }
        }
    }

    @Test
    public void testIsHardwareEncoder() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            assertFalse(deviceEncoders.isHardwareEncoder("OMX.google.encoder"));
            assertTrue(deviceEncoders.isHardwareEncoder("OMX.other.encoder"));
        }
    }

    @Test
    public void testFindDeviceEncoder() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            List<MediaCodecInfo> allEncoders = deviceEncoders.getDeviceEncoders();
            MediaCodecInfo encoder = deviceEncoders.findDeviceEncoder(allEncoders,
                    "video/avc", DeviceEncoders.MODE_RESPECT_ORDER, 0);
            assertNotNull(encoder);
            List<String> encoderTypes = Arrays.asList(encoder.getSupportedTypes());
            assertTrue(encoderTypes.contains("video/avc"));
        }
    }

    @Test
    public void testGetVideoEncoder() {
        if (DeviceEncoders.ENABLED) {
            DeviceEncoders deviceEncoders = create();
            assertNotNull(deviceEncoders.getVideoEncoder());
        }

        DeviceEncoders.ENABLED = false;
        DeviceEncoders deviceEncoders = create();
        assertNull(deviceEncoders.getVideoEncoder());
    }

    @Test
    public void testGetAudioEncoder() {
        if (DeviceEncoders.ENABLED) {
            DeviceEncoders deviceEncoders = create();
            assertNotNull(deviceEncoders.getAudioEncoder());
        }

        DeviceEncoders.ENABLED = false;
        DeviceEncoders deviceEncoders = create();
        assertNull(deviceEncoders.getAudioEncoder());
    }

    @Test
    public void testGetSupportedVideoSize_disabled() {
        DeviceEncoders.ENABLED = false;
        DeviceEncoders deviceEncoders = create();
        Size input = new Size(GUARANTEED_SIZE.getWidth(), GUARANTEED_SIZE.getHeight());
        Size output = deviceEncoders.getSupportedVideoSize(input);
        assertSame(input, output);
    }

    @Test
    public void testGetSupportedVideoSize_scalesDown() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            Size input = new Size(
                    GUARANTEED_SIZE.getWidth() * 1000,
                    GUARANTEED_SIZE.getHeight() * 1000);
            try {
                Size output = deviceEncoders.getSupportedVideoSize(input);
                assertTrue(AspectRatio.of(input).matches(output, 0.01F));
            } catch (RuntimeException e) {
                // The scaled down size happens to be not supported.
                // I see no way of testing this easily if we're not sure of supported ranges.
                // This depends highly on the alignment since scaling down, while keeping AR,
                // can change the alignment and require width / height changes.
            }
        }
    }

    @Test
    public void testGetSupportedVideoSize_aligns() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            Size input = new Size(GUARANTEED_SIZE.getWidth() + 1,
                    GUARANTEED_SIZE.getHeight() + 1);
            Size output = deviceEncoders.getSupportedVideoSize(input);
            assertTrue(output.getWidth() <= input.getWidth());
            assertTrue(output.getHeight() <= input.getHeight());
        }
    }

    @Test
    public void testGetSupportedVideoBitRate_disabled() {
        DeviceEncoders.ENABLED = false;
        DeviceEncoders deviceEncoders = create();
        int input = 1000;
        int output = deviceEncoders.getSupportedVideoBitRate(input);
        assertEquals(input, output);
    }

    @Test
    public void testGetSupportedVideoBitRate_enabled() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            // Ensure it's clamped: we can pass a negative value and check it's >= 0.
            int input = -1000;
            int output = deviceEncoders.getSupportedVideoBitRate(input);
            assertNotEquals(input, output);
            assertTrue(output >= 0);
        }
    }

    @Test
    public void testGetSupportedAudioBitRate_disabled() {
        DeviceEncoders.ENABLED = false;
        DeviceEncoders deviceEncoders = create();
        int input = 1000;
        int output = deviceEncoders.getSupportedAudioBitRate(input);
        assertEquals(input, output);
    }

    @Test
    public void testGetSupportedAudioBitRate_enabled() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            // Ensure it's clamped: we can pass a negative value and check it's >= 0.
            int input = -1000;
            int output = deviceEncoders.getSupportedAudioBitRate(input);
            assertNotEquals(input, output);
            assertTrue(output >= 0);
        }
    }

    @Test
    public void testGetSupportedFrameRate_disabled() {
        DeviceEncoders.ENABLED = false;
        DeviceEncoders deviceEncoders = create();
        int input = 1000;
        int output = deviceEncoders.getSupportedVideoFrameRate(GUARANTEED_SIZE, input);
        assertEquals(input, output);
    }

    @Test
    public void testGetSupportedFrameRate_enabled() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            // Ensure it's clamped: we can pass a negative value and check it's >= 0.
            int input = -10;
            Size inputSize = deviceEncoders.getSupportedVideoSize(GUARANTEED_SIZE);
            int output = deviceEncoders.getSupportedVideoFrameRate(inputSize, input);
            assertNotEquals(input, output);
            assertTrue(output >= 0);
        }
    }
}

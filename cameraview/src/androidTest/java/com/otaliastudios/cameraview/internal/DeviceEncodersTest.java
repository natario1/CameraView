package com.otaliastudios.cameraview.internal;


import android.media.MediaCodecInfo;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.TestActivity;
import com.otaliastudios.cameraview.controls.Grid;
import com.otaliastudios.cameraview.size.Size;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class DeviceEncodersTest extends BaseTest {

    // This is guaranteed to work, see
    // https://developer.android.com/guide/topics/media/media-formats
    private final static Size GUARANTEED_SIZE = new Size(360, 380);

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
        return new DeviceEncoders("video/avc", "audio/mp4a-latm");
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
            MediaCodecInfo encoder = deviceEncoders.findDeviceEncoder(allEncoders, "video/avc");
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

    @Test(expected = RuntimeException.class)
    public void testGetSupportedVideoSize_hugeWidth() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            Size input = new Size(Integer.MAX_VALUE, GUARANTEED_SIZE.getHeight());
            deviceEncoders.getSupportedVideoSize(input);
        } else {
            throw new RuntimeException("Test should pass.");
        }
    }

    @Test(expected = RuntimeException.class)
    public void testGetSupportedVideoSize_hugeHeight() {
        DeviceEncoders deviceEncoders = create();
        if (DeviceEncoders.ENABLED) {
            Size input = new Size(GUARANTEED_SIZE.getWidth(), Integer.MAX_VALUE);
            deviceEncoders.getSupportedVideoSize(input);
        } else {
            throw new RuntimeException("Test should pass.");
        }
    }

    @Test
    public void testGetSupportedVideoSize_alignsSize() {
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

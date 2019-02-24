package com.otaliastudios.cameraview;


import android.hardware.Camera;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraOptions1Test extends BaseTest {

    @Test
    public void testEmpty() {
        CameraOptions o = new CameraOptions(mock(Camera.Parameters.class), false);
        assertTrue(o.getSupportedPictureAspectRatios().isEmpty());
        assertTrue(o.getSupportedPictureSizes().isEmpty());
        assertTrue(o.getSupportedWhiteBalance().isEmpty());
        assertEquals(1, o.getSupportedFlash().size()); // Flash.OFF is always there
        assertEquals(1, o.getSupportedHdr().size()); // Hdr.OFF is always there
        assertFalse(o.isAutoFocusSupported());
        assertFalse(o.isExposureCorrectionSupported());
        assertFalse(o.isZoomSupported());
        assertEquals(o.getExposureCorrectionMaxValue(), 0f, 0);
        assertEquals(o.getExposureCorrectionMinValue(), 0f, 0);
    }

    private Camera.Size mockCameraSize(int width, int height) {
        Camera.Size cs = mock(Camera.Size.class);
        cs.width = width;
        cs.height = height;
        return cs;
    }

    @Test
    public void testPictureSizes() {
        List<Camera.Size> sizes = Arrays.asList(
                mockCameraSize(100, 200),
                mockCameraSize(50, 50),
                mockCameraSize(1600, 900),
                mockCameraSize(1000, 2000)
        );
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedPictureSizes()).thenReturn(sizes);
        CameraOptions o = new CameraOptions(params, false);
        Collection<Size> supportedSizes = o.getSupportedPictureSizes();
        assertEquals(supportedSizes.size(), sizes.size());
        for (Camera.Size size : sizes) {
            Size internalSize = new Size(size.width, size.height);
            assertTrue(supportedSizes.contains(internalSize));
        }
    }

    @Test
    public void testPictureSizesFlip() {
        List<Camera.Size> sizes = Arrays.asList(
                mockCameraSize(100, 200),
                mockCameraSize(50, 50),
                mockCameraSize(1600, 900),
                mockCameraSize(1000, 2000)
        );
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedPictureSizes()).thenReturn(sizes);
        CameraOptions o = new CameraOptions(params, true);
        Collection<Size> supportedSizes = o.getSupportedPictureSizes();
        assertEquals(supportedSizes.size(), sizes.size());
        for (Camera.Size size : sizes) {
            Size internalSize = new Size(size.width, size.height).flip();
            assertTrue(supportedSizes.contains(internalSize));
        }
    }

    @Test
    public void testPictureAspectRatio() {
        List<Camera.Size> sizes = Arrays.asList(
                mockCameraSize(100, 200),
                mockCameraSize(50, 50),
                mockCameraSize(1600, 900),
                mockCameraSize(1000, 2000)
        );

        Set<AspectRatio> expected = new HashSet<>();
        expected.add(AspectRatio.of(1, 2));
        expected.add(AspectRatio.of(1, 1));
        expected.add(AspectRatio.of(16, 9));

        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedPictureSizes()).thenReturn(sizes);
        CameraOptions o = new CameraOptions(params, false);
        Collection<AspectRatio> supportedRatios = o.getSupportedPictureAspectRatios();
        assertEquals(supportedRatios.size(), expected.size());
        for (AspectRatio ratio : expected) {
            assertTrue(supportedRatios.contains(ratio));
        }
    }


    @Test
    public void testVideoSizes() {
        List<Camera.Size> sizes = Arrays.asList(
                mockCameraSize(100, 200),
                mockCameraSize(50, 50),
                mockCameraSize(1600, 900),
                mockCameraSize(1000, 2000)
        );
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedVideoSizes()).thenReturn(sizes);
        CameraOptions o = new CameraOptions(params, false);
        Collection<Size> supportedSizes = o.getSupportedVideoSizes();
        assertEquals(supportedSizes.size(), sizes.size());
        for (Camera.Size size : sizes) {
            Size internalSize = new Size(size.width, size.height);
            assertTrue(supportedSizes.contains(internalSize));
        }
    }

    @Test
    public void testVideoSizesNull() {
        // When videoSizes is null, we take the preview sizes.
        List<Camera.Size> sizes = Arrays.asList(
                mockCameraSize(100, 200),
                mockCameraSize(50, 50),
                mockCameraSize(1600, 900),
                mockCameraSize(1000, 2000)
        );
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedVideoSizes()).thenReturn(null);
        when(params.getSupportedPreviewSizes()).thenReturn(sizes);
        CameraOptions o = new CameraOptions(params, false);
        Collection<Size> supportedSizes = o.getSupportedVideoSizes();
        assertEquals(supportedSizes.size(), sizes.size());
        for (Camera.Size size : sizes) {
            Size internalSize = new Size(size.width, size.height);
            assertTrue(supportedSizes.contains(internalSize));
        }
    }

    @Test
    public void testVideoSizesFlip() {
        List<Camera.Size> sizes = Arrays.asList(
                mockCameraSize(100, 200),
                mockCameraSize(50, 50),
                mockCameraSize(1600, 900),
                mockCameraSize(1000, 2000)
        );
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedVideoSizes()).thenReturn(sizes);
        CameraOptions o = new CameraOptions(params, true);
        Collection<Size> supportedSizes = o.getSupportedVideoSizes();
        assertEquals(supportedSizes.size(), sizes.size());
        for (Camera.Size size : sizes) {
            Size internalSize = new Size(size.width, size.height).flip();
            assertTrue(supportedSizes.contains(internalSize));
        }
    }

    @Test
    public void testVideoAspectRatio() {
        List<Camera.Size> sizes = Arrays.asList(
                mockCameraSize(100, 200),
                mockCameraSize(50, 50),
                mockCameraSize(1600, 900),
                mockCameraSize(1000, 2000)
        );

        Set<AspectRatio> expected = new HashSet<>();
        expected.add(AspectRatio.of(1, 2));
        expected.add(AspectRatio.of(1, 1));
        expected.add(AspectRatio.of(16, 9));

        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedVideoSizes()).thenReturn(sizes);
        CameraOptions o = new CameraOptions(params, false);
        Collection<AspectRatio> supportedRatios = o.getSupportedVideoAspectRatios();
        assertEquals(supportedRatios.size(), expected.size());
        for (AspectRatio ratio : expected) {
            assertTrue(supportedRatios.contains(ratio));
        }
    }

    @Test
    public void testGestureActions() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedFocusModes()).thenReturn(Collections.<String>emptyList());
        when(params.isZoomSupported()).thenReturn(true);
        when(params.getMaxExposureCompensation()).thenReturn(0);
        when(params.getMinExposureCompensation()).thenReturn(0);

        CameraOptions o = new CameraOptions(params, false);
        assertFalse(o.supports(GestureAction.FOCUS));
        assertFalse(o.supports(GestureAction.FOCUS_WITH_MARKER));
        assertTrue(o.supports(GestureAction.CAPTURE));
        assertTrue(o.supports(GestureAction.NONE));
        assertTrue(o.supports(GestureAction.ZOOM));
        assertFalse(o.supports(GestureAction.EXPOSURE_CORRECTION));
    }

    @Test
    public void testAlwaysSupportedControls() {
        // Grid, VideoQuality, SessionType and Audio are always supported.
        Camera.Parameters params = mock(Camera.Parameters.class);
        CameraOptions o = new CameraOptions(params, false);

        Collection<Grid> grids = o.getSupportedControls(Grid.class);
        Collection<VideoCodec> video = o.getSupportedControls(VideoCodec.class);
        Collection<Mode> sessions = o.getSupportedControls(Mode.class);
        Collection<Audio> audio = o.getSupportedControls(Audio.class);
        assertEquals(grids.size(), Grid.values().length);
        assertEquals(video.size(), VideoCodec.values().length);
        assertEquals(sessions.size(), Mode.values().length);
        assertEquals(audio.size(), Audio.values().length);
    }

    @Test
    public void testFacing() {
        Set<Integer> supported = new HashSet<>();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            supported.add(cameraInfo.facing);
        }

        CameraOptions o = new CameraOptions(mock(Camera.Parameters.class), false);
        Mapper m = new Mapper1();
        Collection<Facing> s = o.getSupportedControls(Facing.class);
        assertEquals(s.size(), supported.size());
        for (Facing facing : s) {
            assertTrue(supported.contains(m.<Integer>map(facing)));
            assertTrue(o.supports(facing));
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

        CameraOptions o = new CameraOptions(params, false);
        Collection<WhiteBalance> w = o.getSupportedControls(WhiteBalance.class);
        assertEquals(w.size(), 2);
        assertTrue(w.contains(WhiteBalance.AUTO));
        assertTrue(w.contains(WhiteBalance.CLOUDY));
        assertTrue(o.supports(WhiteBalance.AUTO));
        assertTrue(o.supports(WhiteBalance.CLOUDY));
    }

    @Test
    public void testFlash() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getSupportedFlashModes()).thenReturn(Arrays.asList(
                Camera.Parameters.FLASH_MODE_OFF, // Supported
                Camera.Parameters.FLASH_MODE_AUTO, // Supported
                Camera.Parameters.FLASH_MODE_TORCH, // Supported
                Camera.Parameters.FLASH_MODE_RED_EYE // Not supported
        ));

        CameraOptions o = new CameraOptions(params, false);
        Collection<Flash> f = o.getSupportedControls(Flash.class);
        assertEquals(f.size(), 3);
        assertTrue(f.contains(Flash.OFF));
        assertTrue(f.contains(Flash.AUTO));
        assertTrue(f.contains(Flash.TORCH));

        assertTrue(o.supports(Flash.OFF));
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

        CameraOptions o = new CameraOptions(params, false);
        Collection<Hdr> h = o.getSupportedControls(Hdr.class);
        assertEquals(h.size(), 2);
        assertTrue(h.contains(Hdr.OFF));
        assertTrue(h.contains(Hdr.ON));

        assertTrue(o.supports(Hdr.OFF));
        assertTrue(o.supports(Hdr.ON));
    }

    @Test
    public void testBooleanFlags() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.isVideoSnapshotSupported()).thenReturn(true);
        when(params.isZoomSupported()).thenReturn(true);
        //noinspection ArraysAsListWithZeroOrOneArgument
        when(params.getSupportedFocusModes()).thenReturn(Arrays.asList(Camera.Parameters.FOCUS_MODE_AUTO));
        CameraOptions o = new CameraOptions(params, false);
        assertTrue(o.isZoomSupported());
        assertTrue(o.isAutoFocusSupported());
    }

    @Test
    public void testExposureCorrection() {
        Camera.Parameters params = mock(Camera.Parameters.class);
        when(params.getMaxExposureCompensation()).thenReturn(10);
        when(params.getMinExposureCompensation()).thenReturn(-10);
        when(params.getExposureCompensationStep()).thenReturn(0.5f);
        CameraOptions o = new CameraOptions(params, false);
        assertTrue(o.isExposureCorrectionSupported());
        assertEquals(o.getExposureCorrectionMinValue(), -10f * 0.5f, 0f);
        assertEquals(o.getExposureCorrectionMaxValue(), 10f * 0.5f, 0f);
    }

}

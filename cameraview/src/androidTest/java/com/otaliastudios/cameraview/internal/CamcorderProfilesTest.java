package com.otaliastudios.cameraview.internal;


import android.media.CamcorderProfile;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.internal.CamcorderProfiles;
import com.otaliastudios.cameraview.tools.SdkExclude;
import com.otaliastudios.cameraview.size.Size;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CamcorderProfilesTest extends BaseTest {

    private String getCameraId() {
        if (CameraUtils.hasCameras(getContext())) {
            return "0";
        }
        return null;
    }

    @Test
    public void testInvalidCameraReturnsLowest() {
        CamcorderProfile invalid = CamcorderProfiles.get("invalid", new Size(100, 100));
        CamcorderProfile lowest = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        assertEquals(lowest.videoFrameWidth, invalid.videoFrameWidth);
        assertEquals(lowest.videoFrameHeight, invalid.videoFrameHeight);
    }

    /**
     * For some reason this fails on emulator 26.
     */
    @SdkExclude(minSdkVersion = 26, maxSdkVersion = 26)
    @Test
    public void testGet() {
        String cameraId = getCameraId();
        if (cameraId == null) return;
        int cameraIdInt = Integer.parseInt(cameraId);

        // Not much we can test. Let's just ask for lowest and highest.
        CamcorderProfile low = CamcorderProfiles.get(cameraId, new Size(1, 1));
        CamcorderProfile high = CamcorderProfiles.get(cameraId, new Size(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Compare with lowest
        CamcorderProfile lowest = CamcorderProfile.get(cameraIdInt, CamcorderProfile.QUALITY_LOW);
        CamcorderProfile highest = CamcorderProfile.get(cameraIdInt, CamcorderProfile.QUALITY_HIGH);
        assertEquals(lowest.videoFrameWidth, low.videoFrameWidth);
        assertEquals(lowest.videoFrameHeight, low.videoFrameHeight);
        assertEquals(highest.videoFrameWidth, high.videoFrameWidth);
        assertEquals(highest.videoFrameHeight, high.videoFrameHeight);
    }
}

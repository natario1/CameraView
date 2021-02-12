package com.otaliastudios.cameraview.internal;

import android.annotation.SuppressLint;
import android.media.CamcorderProfile;
import android.os.Build;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.size.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Wraps the {@link android.media.CamcorderProfile} static utilities.
 */
public class CamcorderProfiles {

    private static final String TAG = CamcorderProfiles.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressLint("UseSparseArrays")
    private static Map<Size, Integer> sizeToProfileMap = new HashMap<>();

    static {
        sizeToProfileMap.put(new Size(176, 144), CamcorderProfile.QUALITY_QCIF);
        sizeToProfileMap.put(new Size(320, 240), CamcorderProfile.QUALITY_QVGA);
        sizeToProfileMap.put(new Size(352, 288), CamcorderProfile.QUALITY_CIF);
        sizeToProfileMap.put(new Size(720, 480), CamcorderProfile.QUALITY_480P);
        sizeToProfileMap.put(new Size(1280, 720), CamcorderProfile.QUALITY_720P);
        sizeToProfileMap.put(new Size(1920, 1080), CamcorderProfile.QUALITY_1080P);
        if (Build.VERSION.SDK_INT >= 21) {
            sizeToProfileMap.put(new Size(3840, 2160),
                    CamcorderProfile.QUALITY_2160P);
        }
    }


    /**
     * Returns a CamcorderProfile that's somewhat coherent with the target size,
     * to ensure we get acceptable video/audio parameters for MediaRecorders
     * (most notably the bitrate).
     *
     * @param cameraId the camera2 id
     * @param targetSize the target video size
     * @return a profile
     */
    @NonNull
    public static CamcorderProfile get(@NonNull String cameraId, @NonNull Size targetSize) {
        // It seems that the way to do this is to use Integer.parseInt().
        try {
            int camera1Id = Integer.parseInt(cameraId);
            return get(camera1Id, targetSize);
        } catch (NumberFormatException e) {
            LOG.w("NumberFormatException for Camera2 id:", cameraId);
            return CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        }
    }

    /**
     * Returns a CamcorderProfile that's somewhat coherent with the target size,
     * to ensure we get acceptable video/audio parameters for MediaRecorders
     * (most notably the bitrate).
     *
     * @param cameraId the camera id
     * @param targetSize the target video size
     * @return a profile
     */
    @NonNull
    public static CamcorderProfile get(int cameraId, @NonNull Size targetSize) {
        final long targetArea = (long) targetSize.getWidth() * targetSize.getHeight();
        List<Size> sizes = new ArrayList<>(sizeToProfileMap.keySet());
        Collections.sort(sizes, new Comparator<Size>() {
            @Override
            public int compare(Size s1, Size s2) {
                long a1 = Math.abs(s1.getWidth() * s1.getHeight() - targetArea);
                long a2 = Math.abs(s2.getWidth() * s2.getHeight() - targetArea);
                //noinspection UseCompareMethod
                return (a1 < a2) ? -1 : ((a1 == a2) ? 0 : 1);
            }
        });
        while (sizes.size() > 0) {
            Size candidate = sizes.remove(0);
            //noinspection ConstantConditions
            int quality = sizeToProfileMap.get(candidate);
            if (CamcorderProfile.hasProfile(cameraId, quality)) {
                return CamcorderProfile.get(cameraId, quality);
            }
        }
        // Should never happen, but fallback to low.
        return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
    }
}

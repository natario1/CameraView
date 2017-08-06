package com.flurgle.camerakit;


import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;

import java.util.ArrayList;
import java.util.List;

/**
 * Options telling you what is available and what is not.
 */
@SuppressWarnings("deprecation")
public class CameraOptions {

    private List<Integer> supportedWhiteBalance = new ArrayList<>(5);
    private List<Integer> supportedFacing = new ArrayList<>(2);
    private List<Integer> supportedFlash = new ArrayList<>(3);
    private List<Integer> supportedFocus = new ArrayList<>(3);

    private boolean zoomSupported;
    private boolean videoSnapshotSupported;

    CameraOptions(Camera.Parameters params) {
        List<String> strings;
        MapperImpl mapper = new MapperImpl.Mapper1();

        // Facing
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            supportedFacing.add(mapper.unmapFacing(cameraInfo.facing));
        }

        // WB
        strings = params.getSupportedWhiteBalance();
        if (strings != null) {
            for (String string : strings) {
                supportedWhiteBalance.add(mapper.unmapWhiteBalance(string));
            }
        }

        // Flash
        strings = params.getSupportedFlashModes();
        if (strings != null) {
            for (String string : strings) {
                supportedFlash.add(mapper.unmapFlash(string));
            }
        }

        // Focus
        strings = params.getSupportedFocusModes(); // Never null.
        for (String string : strings) {
            supportedFocus.add(mapper.unmapFocus(string));
        }

        zoomSupported = params.isZoomSupported();
        videoSnapshotSupported = params.isVideoSnapshotSupported();



    }

    @TargetApi(21)
    CameraOptions(CameraCharacteristics params) {}

    public List<Integer> getSupportedFacing() {
        return supportedFacing;
    }

    public List<Integer> getSupportedFlash() {
        return supportedFlash;
    }

    public List<Integer> getSupportedWhiteBalance() {
        return supportedWhiteBalance;
    }

    public List<Integer> getSupportedFocus() {
        return supportedFocus;
    }

    public boolean isZoomSupported() {
        return zoomSupported;
    }

    public boolean isVideoSnapshotSupported() {
        return videoSnapshotSupported;
    }
}

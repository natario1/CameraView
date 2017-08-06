package com.flurgle.camerakit;


import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Options telling you what is available and what is not.
 */
public class CameraOptions {

    private Set<Integer> supportedWhiteBalance = new HashSet<>(5);
    private Set<Integer> supportedFacing = new HashSet<>(2);
    private Set<Integer> supportedFlash = new HashSet<>(4);
    private Set<Integer> supportedFocus = new HashSet<>(4);

    private boolean zoomSupported;
    private boolean videoSnapshotSupported;


    // Camera1 constructor.
    @SuppressWarnings("deprecation")
    CameraOptions(Camera.Parameters params) {
        List<String> strings;
        Mapper mapper = new Mapper.Mapper1();

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


    // Camera2 constructor.
    @TargetApi(21)
    CameraOptions(CameraCharacteristics params) {}


    /**
     * Set of supported facing values.
     *
     * @see CameraConstants#FACING_BACK
     * @see CameraConstants#FACING_FRONT
     * @return a set of supported values.
     */
    @NonNull
    public Set<Integer> getSupportedFacing() {
        return supportedFacing;
    }


    /**
     * Set of supported flash values.
     *
     * @see CameraConstants#FLASH_AUTO
     * @see CameraConstants#FLASH_OFF
     * @see CameraConstants#FLASH_ON
     * @see CameraConstants#FLASH_TORCH
     * @return a set of supported values.
     */
    @NonNull
    public Set<Integer> getSupportedFlash() {
        return supportedFlash;
    }


    /**
     * Set of supported white balance values.
     *
     * @see CameraConstants#WHITE_BALANCE_AUTO
     * @see CameraConstants#WHITE_BALANCE_CLOUDY
     * @see CameraConstants#WHITE_BALANCE_DAYLIGHT
     * @see CameraConstants#WHITE_BALANCE_FLUORESCENT
     * @see CameraConstants#WHITE_BALANCE_INCANDESCENT
     * @return a set of supported values.
     */
    @NonNull
    public Set<Integer> getSupportedWhiteBalance() {
        return supportedWhiteBalance;
    }


    /**
     * Set of supported focus values.
     *
     * @see CameraConstants#FOCUS_FIXED
     * @see CameraConstants#FOCUS_CONTINUOUS
     * @see CameraConstants#FOCUS_TAP
     * @see CameraConstants#FOCUS_TAP_WITH_MARKER
     * @return a set of supported values.
     */
    @NonNull
    public Set<Integer> getSupportedFocus() {
        return supportedFocus;
    }


    /**
     * Whether zoom is supported. If this is false, pinch-to-zoom
     * will not work and {@link CameraView#setZoom(float)} will have no effect.
     *
     * @return whether zoom is supported.
     */
    public boolean isZoomSupported() {
        return zoomSupported;
    }


    /**
     * Whether video snapshots are supported. If this is false, taking pictures
     * while recording a video will have no effect.
     *
     * @return whether video snapshot is supported.
     */
    public boolean isVideoSnapshotSupported() {
        return videoSnapshotSupported;
    }
}

package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.support.annotation.NonNull;

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
    private boolean exposureCorrectionSupported;
    private float exposureCorrectionMinValue;
    private float exposureCorrectionMaxValue;


    // Camera1 constructor.
    @SuppressWarnings("deprecation")
    CameraOptions(Camera.Parameters params) {
        List<String> strings;
        Mapper mapper = new Mapper.Mapper1();

        // Facing
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            Integer value = mapper.unmapFacing(cameraInfo.facing);
            if (value != null) supportedFacing.add(value);
        }

        // WB
        strings = params.getSupportedWhiteBalance();
        if (strings != null) {
            for (String string : strings) {
                Integer value = mapper.unmapWhiteBalance(string);
                if (value != null) supportedWhiteBalance.add(value);
            }
        }

        // Flash
        strings = params.getSupportedFlashModes();
        if (strings != null) {
            for (String string : strings) {
                Integer value = mapper.unmapFlash(string);
                if (value != null) supportedFlash.add(value);
            }
        }

        // Focus
        strings = params.getSupportedFocusModes(); // Never null.
        for (String string : strings) {
            Integer value = mapper.unmapFocus(string);
            if (value != null) supportedFocus.add(value);
        }

        zoomSupported = params.isZoomSupported();
        videoSnapshotSupported = params.isVideoSnapshotSupported();

        // Exposure correction
        float step = params.getExposureCompensationStep();
        exposureCorrectionMinValue = (float) params.getMinExposureCompensation() * step;
        exposureCorrectionMaxValue = (float) params.getMaxExposureCompensation() * step;
        exposureCorrectionSupported = params.getMinExposureCompensation() != 0
                || params.getMaxExposureCompensation() != 0;
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


    /**
     * Whether exposure correction is supported. If this is false, calling
     * {@link CameraView#setExposureCorrection(float)} has no effect.
     *
     * @see #getExposureCorrectionMinValue()
     * @see #getExposureCorrectionMaxValue()
     * @return whether exposure correction is supported.
     */
    public boolean isExposureCorrectionSupported() {
        return exposureCorrectionSupported;
    }


    /**
     * The minimum value of negative exposure correction, in EV stops.
     * This is presumably negative or 0 if not supported.
     *
     * @return min EV value
     */
    public float getExposureCorrectionMinValue() {
        return exposureCorrectionMinValue;
    }


    /**
     * The maximum value of positive exposure correction, in EV stops.
     * This is presumably positive or 0 if not supported.
     *
     * @return max EV value
     */
    public float getExposureCorrectionMaxValue() {
        return exposureCorrectionMaxValue;
    }
}

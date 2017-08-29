package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Options telling you what is available and what is not.
 */
public class CameraOptions {

    private Set<WhiteBalance> supportedWhiteBalance = new HashSet<>(5);
    private Set<Facing> supportedFacing = new HashSet<>(2);
    private Set<Flash> supportedFlash = new HashSet<>(4);
    private Set<Hdr> supportedHdr = new HashSet<>(2);

    private boolean zoomSupported;
    private boolean videoSnapshotSupported;
    private boolean exposureCorrectionSupported;
    private float exposureCorrectionMinValue;
    private float exposureCorrectionMaxValue;
    private boolean autoFocusSupported;


    // Camera1 constructor.
    @SuppressWarnings("deprecation")
    CameraOptions(Camera.Parameters params) {
        List<String> strings;
        Mapper mapper = new Mapper.Mapper1();

        // Facing
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            Facing value = mapper.unmapFacing(cameraInfo.facing);
            if (value != null) supportedFacing.add(value);
        }

        // WB
        strings = params.getSupportedWhiteBalance();
        if (strings != null) {
            for (String string : strings) {
                WhiteBalance value = mapper.unmapWhiteBalance(string);
                if (value != null) supportedWhiteBalance.add(value);
            }
        }

        // Flash
        strings = params.getSupportedFlashModes();
        if (strings != null) {
            for (String string : strings) {
                Flash value = mapper.unmapFlash(string);
                if (value != null) supportedFlash.add(value);
            }
        }

        // Hdr
        strings = params.getSupportedSceneModes();
        if (strings != null) {
            for (String string : strings) {
                Hdr value = mapper.unmapHdr(string);
                if (value != null) supportedHdr.add(value);
            }
        }

        zoomSupported = params.isZoomSupported();
        videoSnapshotSupported = params.isVideoSnapshotSupported();
        autoFocusSupported = params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO);

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
     * Shorthand for getSupportedFacing().contains(value).
     *
     * @param facing value
     * @return whether it's supported
     */
    public boolean supports(Facing facing) {
        return getSupportedFacing().contains(facing);
    }


    /**
     * Shorthand for getSupportedFlash().contains(value).
     *
     * @param flash value
     * @return whether it's supported
     */
    public boolean supports(Flash flash) {
        return getSupportedFlash().contains(flash);
    }


    /**
     * Shorthand for getSupportedWhiteBalance().contains(value).
     *
     * @param whiteBalance value
     * @return whether it's supported
     */
    public boolean supports(WhiteBalance whiteBalance) {
        return getSupportedWhiteBalance().contains(whiteBalance);
    }


    /**
     * Shorthand for getSupportedHdr().contains(value).
     *
     * @param hdr value
     * @return whether it's supported
     */
    public boolean supports(Hdr hdr) {
        return getSupportedHdr().contains(hdr);
    }


    /**
     * Set of supported facing values.
     *
     * @see Facing#BACK
     * @see Facing#FRONT
     * @return a set of supported values.
     */
    @NonNull
    public Set<Facing> getSupportedFacing() {
        return Collections.unmodifiableSet(supportedFacing);
    }


    /**
     * Set of supported flash values.
     *
     * @see Flash#AUTO
     * @see Flash#OFF
     * @see Flash#ON
     * @see Flash#TORCH
     * @return a set of supported values.
     */
    @NonNull
    public Set<Flash> getSupportedFlash() {
        return Collections.unmodifiableSet(supportedFlash);
    }


    /**
     * Set of supported white balance values.
     *
     * @see WhiteBalance#AUTO
     * @see WhiteBalance#INCANDESCENT
     * @see WhiteBalance#FLUORESCENT
     * @see WhiteBalance#DAYLIGHT
     * @see WhiteBalance#CLOUDY
     * @return a set of supported values.
     */
    @NonNull
    public Set<WhiteBalance> getSupportedWhiteBalance() {
        return Collections.unmodifiableSet(supportedWhiteBalance);
    }


    /**
     * Set of supported hdr values.
     *
     * @see Hdr#OFF
     * @see Hdr#ON
     * @return a set of supported values.
     */
    @NonNull
    public Set<Hdr> getSupportedHdr() {
        return Collections.unmodifiableSet(supportedHdr);
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
     * Whether auto focus is supported. This means you can map gestures to
     * {@link GestureAction#FOCUS} or {@link GestureAction#FOCUS_WITH_MARKER}
     * and focus will be changed on tap.
     *
     * @return whether auto focus is supported.
     */
    public boolean isAutoFocusSupported() {
        return autoFocusSupported;
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

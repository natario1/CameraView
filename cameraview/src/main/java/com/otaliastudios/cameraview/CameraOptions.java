package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collection;
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
    private Set<Size> supportedPictureSizes = new HashSet<>(15);
    private Set<Size> supportedVideoSizes = new HashSet<>(5);
    private Set<AspectRatio> supportedPictureAspectRatio = new HashSet<>(4);
    private Set<AspectRatio> supportedVideoAspectRatio = new HashSet<>(3);

    private boolean zoomSupported;
    private boolean exposureCorrectionSupported;
    private float exposureCorrectionMinValue;
    private float exposureCorrectionMaxValue;
    private boolean autoFocusSupported;


    // Camera1 constructor.
    @SuppressWarnings("deprecation")
    CameraOptions(@NonNull Camera.Parameters params, boolean flipSizes) {
        List<String> strings;
        Mapper mapper = new Mapper1();

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
        supportedFlash.add(Flash.OFF);
        strings = params.getSupportedFlashModes();
        if (strings != null) {
            for (String string : strings) {
                Flash value = mapper.unmapFlash(string);
                if (value != null) supportedFlash.add(value);
            }
        }

        // Hdr
        supportedHdr.add(Hdr.OFF);
        strings = params.getSupportedSceneModes();
        if (strings != null) {
            for (String string : strings) {
                Hdr value = mapper.unmapHdr(string);
                if (value != null) supportedHdr.add(value);
            }
        }

        zoomSupported = params.isZoomSupported();
        autoFocusSupported = params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO);

        // Exposure correction
        float step = params.getExposureCompensationStep();
        exposureCorrectionMinValue = (float) params.getMinExposureCompensation() * step;
        exposureCorrectionMaxValue = (float) params.getMaxExposureCompensation() * step;
        exposureCorrectionSupported = params.getMinExposureCompensation() != 0
                || params.getMaxExposureCompensation() != 0;

        // Picture Sizes
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        for (Camera.Size size : sizes) {
            int width = flipSizes ? size.height : size.width;
            int height = flipSizes ? size.width : size.height;
            supportedPictureSizes.add(new Size(width, height));
            supportedPictureAspectRatio.add(AspectRatio.of(width, height));
        }

        // Video Sizes
        List<Camera.Size> vsizes = params.getSupportedVideoSizes();
        if (vsizes != null) {
            for (Camera.Size size : vsizes) {
                int width = flipSizes ? size.height : size.width;
                int height = flipSizes ? size.width : size.height;
                supportedVideoSizes.add(new Size(width, height));
                supportedVideoAspectRatio.add(AspectRatio.of(width, height));
            }
        } else {
            // StackOverflow threads seems to agree that if getSupportedVideoSizes is null, previews can be used.
            List<Camera.Size> fallback = params.getSupportedPreviewSizes();
            for (Camera.Size size : fallback) {
                int width = flipSizes ? size.height : size.width;
                int height = flipSizes ? size.width : size.height;
                supportedVideoSizes.add(new Size(width, height));
                supportedVideoAspectRatio.add(AspectRatio.of(width, height));
            }
        }
    }


    // Camera2 constructor.
    @TargetApi(21)
    CameraOptions(@NonNull CameraCharacteristics params) {}


    /**
     * Shorthand for getSupported*().contains(value).
     *
     * @param control value to check
     * @return whether it's supported
     */
    public boolean supports(@NonNull Control control) {
        return getSupportedControls(control.getClass()).contains(control);
    }


    /**
     * Shorthand for other methods in this class,
     * e.g. supports(GestureAction.ZOOM) == isZoomSupported().
     *
     * @param action value to be checked
     * @return whether it's supported
     */
    public boolean supports(@NonNull GestureAction action) {
        switch (action) {
            case FOCUS:
            case FOCUS_WITH_MARKER:
                return isAutoFocusSupported();
            case CAPTURE:
            case NONE:
                return true;
            case ZOOM:
                return isZoomSupported();
            case EXPOSURE_CORRECTION:
                return isExposureCorrectionSupported();
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    @NonNull
    public <T extends Control> Collection<T> getSupportedControls(@NonNull Class<T> controlClass) {
        if (controlClass.equals(Audio.class)) {
            return (Collection<T>) Arrays.asList(Audio.values());
        } else if (controlClass.equals(Facing.class)) {
            return (Collection<T>) getSupportedFacing();
        } else if (controlClass.equals(Flash.class)) {
            return (Collection<T>) getSupportedFlash();
        } else if (controlClass.equals(Grid.class)) {
            return (Collection<T>) Arrays.asList(Grid.values());
        } else if (controlClass.equals(Hdr.class)) {
            return (Collection<T>) getSupportedHdr();
        } else if (controlClass.equals(Mode.class)) {
            return (Collection<T>) Arrays.asList(Mode.values());
        } else if (controlClass.equals(VideoCodec.class)) {
            return (Collection<T>) Arrays.asList(VideoCodec.values());
        } else if (controlClass.equals(WhiteBalance.class)) {
            return (Collection<T>) getSupportedWhiteBalance();
        }
        // Unrecognized control.
        return Collections.emptyList();
    }


    /**
     * Set of supported picture sizes for the currently opened camera.
     *
     * @return a collection of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Collection<Size> getSupportedPictureSizes() {
        return Collections.unmodifiableSet(supportedPictureSizes);
    }


    /**
     * Set of supported picture aspect ratios for the currently opened camera.
     *
     * @return a collection of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Collection<AspectRatio> getSupportedPictureAspectRatios() {
        return Collections.unmodifiableSet(supportedPictureAspectRatio);
    }


    /**
     * Set of supported video sizes for the currently opened camera.
     *
     * @return a collection of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Collection<Size> getSupportedVideoSizes() {
        return Collections.unmodifiableSet(supportedVideoSizes);
    }


    /**
     * Set of supported picture aspect ratios for the currently opened camera.
     *
     * @return a set of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Collection<AspectRatio> getSupportedVideoAspectRatios() {
        return Collections.unmodifiableSet(supportedVideoAspectRatio);
    }


    /**
     * Set of supported facing values.
     *
     * @see Facing#BACK
     * @see Facing#FRONT
     * @return a collection of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Collection<Facing> getSupportedFacing() {
        return Collections.unmodifiableSet(supportedFacing);
    }


    /**
     * Set of supported flash values.
     *
     * @see Flash#AUTO
     * @see Flash#OFF
     * @see Flash#ON
     * @see Flash#TORCH
     * @return a collection of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Collection<Flash> getSupportedFlash() {
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
     * @return a collection of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Collection<WhiteBalance> getSupportedWhiteBalance() {
        return Collections.unmodifiableSet(supportedWhiteBalance);
    }


    /**
     * Set of supported hdr values.
     *
     * @see Hdr#OFF
     * @see Hdr#ON
     * @return a collection of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Collection<Hdr> getSupportedHdr() {
        return Collections.unmodifiableSet(supportedHdr);
    }

    /**
     * Whether zoom is supported. If this is false, pinch-to-zoom
     * will not work and {@link CameraView#setZoom(float)} will have no effect.
     *
     * @return whether zoom is supported.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isZoomSupported() {
        return zoomSupported;
    }


    /**
     * Whether auto focus is supported. This means you can map gestures to
     * {@link GestureAction#FOCUS} or {@link GestureAction#FOCUS_WITH_MARKER}
     * and focus will be changed on tap.
     *
     * @return whether auto focus is supported.
     */
    @SuppressWarnings("WeakerAccess")
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
    @SuppressWarnings("WeakerAccess")
    public boolean isExposureCorrectionSupported() {
        return exposureCorrectionSupported;
    }


    /**
     * The minimum value of negative exposure correction, in EV stops.
     * This is presumably negative or 0 if not supported.
     *
     * @return min EV value
     */
    @SuppressWarnings("WeakerAccess")
    public float getExposureCorrectionMinValue() {
        return exposureCorrectionMinValue;
    }


    /**
     * The maximum value of positive exposure correction, in EV stops.
     * This is presumably positive or 0 if not supported.
     *
     * @return max EV value
     */
    @SuppressWarnings("WeakerAccess")
    public float getExposureCorrectionMaxValue() {
        return exposureCorrectionMaxValue;
    }
}

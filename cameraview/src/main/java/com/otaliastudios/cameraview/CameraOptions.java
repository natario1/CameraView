package com.otaliastudios.cameraview;


import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Range;
import android.util.Rational;

import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Control;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.engine.mappers.Camera1Mapper;
import com.otaliastudios.cameraview.engine.mappers.Camera2Mapper;
import com.otaliastudios.cameraview.gesture.GestureAction;
import com.otaliastudios.cameraview.controls.Grid;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.VideoCodec;
import com.otaliastudios.cameraview.controls.WhiteBalance;
import com.otaliastudios.cameraview.internal.utils.CamcorderProfiles;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

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
    private float previewFrameRateMinValue;
    private float previewFrameRateMaxValue;


    public CameraOptions(@NonNull Camera.Parameters params, int cameraId, boolean flipSizes) {
        List<String> strings;
        Camera1Mapper mapper = Camera1Mapper.get();

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

        // zoom
        zoomSupported = params.isZoomSupported();

        // autofocus
        autoFocusSupported = params.getSupportedFocusModes()
                .contains(Camera.Parameters.FOCUS_MODE_AUTO);

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
        // As a safety measure, remove Sizes bigger than CamcorderProfile.highest
        CamcorderProfile profile = CamcorderProfiles.get(cameraId,
                new Size(Integer.MAX_VALUE, Integer.MAX_VALUE));
        Size videoMaxSize = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
        List<Camera.Size> vsizes = params.getSupportedVideoSizes();
        if (vsizes != null) {
            for (Camera.Size size : vsizes) {
                if (size.width <= videoMaxSize.getWidth()
                        && size.height <= videoMaxSize.getHeight()) {
                    int width = flipSizes ? size.height : size.width;
                    int height = flipSizes ? size.width : size.height;
                    supportedVideoSizes.add(new Size(width, height));
                    supportedVideoAspectRatio.add(AspectRatio.of(width, height));
                }
            }
        } else {
            // StackOverflow threads seems to agree that if getSupportedVideoSizes is null,
            // previews can be used.
            List<Camera.Size> fallback = params.getSupportedPreviewSizes();
            for (Camera.Size size : fallback) {
                if (size.width <= videoMaxSize.getWidth()
                        && size.height <= videoMaxSize.getHeight()) {
                    int width = flipSizes ? size.height : size.width;
                    int height = flipSizes ? size.width : size.height;
                    supportedVideoSizes.add(new Size(width, height));
                    supportedVideoAspectRatio.add(AspectRatio.of(width, height));
                }
            }
        }

        // Preview FPS
        previewFrameRateMinValue = Float.MAX_VALUE;
        previewFrameRateMaxValue = -Float.MAX_VALUE;
        List<int[]> fpsRanges = params.getSupportedPreviewFpsRange();
        for (int[] fpsRange : fpsRanges) {
            float lower = (float) fpsRange[0] / 1000F;
            float upper = (float) fpsRange[1] / 1000F;
            previewFrameRateMinValue = Math.min(previewFrameRateMinValue, lower);
            previewFrameRateMaxValue = Math.max(previewFrameRateMaxValue, upper);
        }
    }

    // Camera2Engine constructor.
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public CameraOptions(@NonNull CameraManager manager,
                         @NonNull String cameraId,
                         boolean flipSizes) throws CameraAccessException {
        Camera2Mapper mapper = Camera2Mapper.get();
        CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);

        // Facing
        for (String cameraId1 : manager.getCameraIdList()) {
            CameraCharacteristics cameraCharacteristics1 = manager
                    .getCameraCharacteristics(cameraId1);
            Integer cameraFacing = cameraCharacteristics1.get(CameraCharacteristics.LENS_FACING);
            if (cameraFacing != null) {
                Facing value = mapper.unmapFacing(cameraFacing);
                if (value != null) supportedFacing.add(value);
            }
        }

        // WB
        int[] awbModes = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        //noinspection ConstantConditions
        for (int awbMode : awbModes) {
            WhiteBalance value = mapper.unmapWhiteBalance(awbMode);
            if (value != null) supportedWhiteBalance.add(value);
        }

        // Flash
        supportedFlash.add(Flash.OFF);
        Boolean hasFlash = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        if (hasFlash != null && hasFlash) {
            int[] aeModes = cameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            //noinspection ConstantConditions
            for (int aeMode : aeModes) {
                Set<Flash> flashes = mapper.unmapFlash(aeMode);
                supportedFlash.addAll(flashes);
            }
        }

        // HDR
        supportedHdr.add(Hdr.OFF);
        int[] sceneModes = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
        //noinspection ConstantConditions
        for (int sceneMode : sceneModes) {
            Hdr value = mapper.unmapHdr(sceneMode);
            if (value != null) supportedHdr.add(value);
        }

        // Zoom
        Float maxZoom = cameraCharacteristics.get(
                CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        if(maxZoom != null) {
            zoomSupported = maxZoom > 1;
        }


        // AutoFocus
        // This now means 3A metering with respect to a specific region of the screen.
        // Some controls (AF, AE) have special triggers that might or might not be supported.
        // But they can also be on some continuous search mode so that the trigger is not needed.
        // What really matters in my opinion is the availability of regions.
        Integer afRegions = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        Integer aeRegions = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        Integer awbRegions = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_MAX_REGIONS_AWB);
        autoFocusSupported = (afRegions != null && afRegions > 0)
                || (aeRegions != null && aeRegions > 0)
                || (awbRegions != null && awbRegions > 0);

        // Exposure correction
        Range<Integer> exposureRange = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        Rational exposureStep = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        if (exposureRange != null && exposureStep != null && exposureStep.floatValue() != 0) {
            exposureCorrectionMinValue = exposureRange.getLower() / exposureStep.floatValue();
            exposureCorrectionMaxValue = exposureRange.getUpper() / exposureStep.floatValue();
        }
        exposureCorrectionSupported = exposureCorrectionMinValue != 0
                && exposureCorrectionMaxValue != 0;


        // Picture Sizes
        StreamConfigurationMap streamMap = cameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamMap == null) {
            throw new RuntimeException("StreamConfigurationMap is null. Should not happen.");
        }
        android.util.Size[] psizes = streamMap.getOutputSizes(ImageFormat.JPEG);
        for (android.util.Size size : psizes) {
            int width = flipSizes ? size.getHeight() : size.getWidth();
            int height = flipSizes ? size.getWidth() : size.getHeight();
            supportedPictureSizes.add(new Size(width, height));
            supportedPictureAspectRatio.add(AspectRatio.of(width, height));
        }

        // Video Sizes
        // As a safety measure, remove Sizes bigger than CamcorderProfile.highest
        CamcorderProfile profile = CamcorderProfiles.get(cameraId,
                new Size(Integer.MAX_VALUE, Integer.MAX_VALUE));
        Size videoMaxSize = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
        android.util.Size[] vsizes = streamMap.getOutputSizes(MediaRecorder.class);
        for (android.util.Size size : vsizes) {
            if (size.getWidth() <= videoMaxSize.getWidth()
                    && size.getHeight() <= videoMaxSize.getHeight()) {
                int width = flipSizes ? size.getHeight() : size.getWidth();
                int height = flipSizes ? size.getWidth() : size.getHeight();
                supportedVideoSizes.add(new Size(width, height));
                supportedVideoAspectRatio.add(AspectRatio.of(width, height));
            }
        }

        // Preview FPS
        Range<Integer>[] range = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (range != null) {
            previewFrameRateMinValue = Float.MAX_VALUE;
            previewFrameRateMaxValue = -Float.MAX_VALUE;
            for (Range<Integer> fpsRange : range) {
                previewFrameRateMinValue = Math.min(previewFrameRateMinValue, fpsRange.getLower());
                previewFrameRateMaxValue = Math.max(previewFrameRateMaxValue, fpsRange.getUpper());
            }
        } else {
            previewFrameRateMinValue = 0F;
            previewFrameRateMaxValue = 0F;
        }
    }

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
            case AUTO_FOCUS:
                return isAutoFocusSupported();
            case TAKE_PICTURE:
            case FILTER_CONTROL_1:
            case FILTER_CONTROL_2:
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
        } else if (controlClass.equals(Engine.class)) {
            return (Collection<T>) Arrays.asList(Engine.values());
        } else if (controlClass.equals(Preview.class)) {
            return (Collection<T>) Arrays.asList(Preview.values());
        }
        // Unrecognized control.
        return Collections.emptyList();
    }

    /**
     * Set of supported picture sizes for the currently opened camera.
     *
     * @return a collection of supported values.
     */
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
    public boolean isZoomSupported() {
        return zoomSupported;
    }


    /**
     * Whether touch metering (metering with respect to a specific region of the screen) is
     * supported. If it is, you can map gestures to {@link GestureAction#AUTO_FOCUS}
     * and metering will change on tap.
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

    /**
     * The minimum value for the preview frame rate, in frames per second (FPS).
     *
     * @return the min value
     */
    public float getPreviewFrameRateMinValue() {
        return previewFrameRateMinValue;
    }

    /**
     * The maximum value for the preview frame rate, in frames per second (FPS).
     *
     * @return the max value
     */
    public float getPreviewFrameRateMaxValue() {
        return previewFrameRateMaxValue;
    }
}

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
public abstract class CameraOptions {

    protected Set<WhiteBalance> supportedWhiteBalance = new HashSet<>(5);
    protected Set<Facing> supportedFacing = new HashSet<>(2);
    protected Set<Flash> supportedFlash = new HashSet<>(4);
    protected Set<Hdr> supportedHdr = new HashSet<>(2);
    protected Set<Size> supportedPictureSizes = new HashSet<>(15);
    protected Set<Size> supportedVideoSizes = new HashSet<>(5);
    protected Set<AspectRatio> supportedPictureAspectRatio = new HashSet<>(4);
    protected Set<AspectRatio> supportedVideoAspectRatio = new HashSet<>(3);

    protected boolean zoomSupported;
    protected boolean exposureCorrectionSupported;
    protected float exposureCorrectionMinValue;
    protected float exposureCorrectionMaxValue;
    protected boolean autoFocusSupported;
    protected float previewFrameRateMinValue;
    protected float previewFrameRateMaxValue;

    protected CameraOptions() { }

    /**
     * Shorthand for getSupported*().contains(value).
     *
     * @param control value to check
     * @return whether it's supported
     */
    public final boolean supports(@NonNull Control control) {
        return getSupportedControls(control.getClass()).contains(control);
    }

    /**
     * Shorthand for other methods in this class,
     * e.g. supports(GestureAction.ZOOM) == isZoomSupported().
     *
     * @param action value to be checked
     * @return whether it's supported
     */
    public final boolean supports(@NonNull GestureAction action) {
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
    public final <T extends Control> Collection<T> getSupportedControls(
            @NonNull Class<T> controlClass) {
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
    public final Collection<Size> getSupportedPictureSizes() {
        return Collections.unmodifiableSet(supportedPictureSizes);
    }

    /**
     * Set of supported picture aspect ratios for the currently opened camera.
     *
     * @return a collection of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public final Collection<AspectRatio> getSupportedPictureAspectRatios() {
        return Collections.unmodifiableSet(supportedPictureAspectRatio);
    }

    /**
     * Set of supported video sizes for the currently opened camera.
     *
     * @return a collection of supported values.
     */
    @NonNull
    public final Collection<Size> getSupportedVideoSizes() {
        return Collections.unmodifiableSet(supportedVideoSizes);
    }

    /**
     * Set of supported picture aspect ratios for the currently opened camera.
     *
     * @return a set of supported values.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public final Collection<AspectRatio> getSupportedVideoAspectRatios() {
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
    public final Collection<Facing> getSupportedFacing() {
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
    public final Collection<Flash> getSupportedFlash() {
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
    public final Collection<WhiteBalance> getSupportedWhiteBalance() {
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
    public final Collection<Hdr> getSupportedHdr() {
        return Collections.unmodifiableSet(supportedHdr);
    }

    /**
     * Whether zoom is supported. If this is false, pinch-to-zoom
     * will not work and {@link CameraView#setZoom(float)} will have no effect.
     *
     * @return whether zoom is supported.
     */
    public final boolean isZoomSupported() {
        return zoomSupported;
    }


    /**
     * Whether touch metering (metering with respect to a specific region of the screen) is
     * supported. If it is, you can map gestures to {@link GestureAction#AUTO_FOCUS}
     * and metering will change on tap.
     *
     * @return whether auto focus is supported.
     */
    public final boolean isAutoFocusSupported() {
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
    public final boolean isExposureCorrectionSupported() {
        return exposureCorrectionSupported;
    }

    /**
     * The minimum value of negative exposure correction, in EV stops.
     * This is presumably negative or 0 if not supported.
     *
     * @return min EV value
     */
    public final float getExposureCorrectionMinValue() {
        return exposureCorrectionMinValue;
    }


    /**
     * The maximum value of positive exposure correction, in EV stops.
     * This is presumably positive or 0 if not supported.
     *
     * @return max EV value
     */
    public final float getExposureCorrectionMaxValue() {
        return exposureCorrectionMaxValue;
    }

    /**
     * The minimum value for the preview frame rate, in frames per second (FPS).
     *
     * @return the min value
     */
    public final float getPreviewFrameRateMinValue() {
        return previewFrameRateMinValue;
    }

    /**
     * The maximum value for the preview frame rate, in frames per second (FPS).
     *
     * @return the max value
     */
    public final float getPreviewFrameRateMaxValue() {
        return previewFrameRateMaxValue;
    }
}

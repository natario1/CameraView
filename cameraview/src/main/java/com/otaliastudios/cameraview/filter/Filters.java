package com.otaliastudios.cameraview.filter;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filters.AutoFixFilter;
import com.otaliastudios.cameraview.filters.BlackAndWhiteFilter;
import com.otaliastudios.cameraview.filters.ToasterFilter;
import com.otaliastudios.cameraview.filters.BrightnessFilter;
import com.otaliastudios.cameraview.filters.ChromaticAberrationFilter;
import com.otaliastudios.cameraview.filters.ColorspaceFilter;
import com.otaliastudios.cameraview.filters.ContrastFilter;
import com.otaliastudios.cameraview.filters.CrossProcessFilter;
import com.otaliastudios.cameraview.filters.DocumentaryFilter;
import com.otaliastudios.cameraview.filters.DuotoneFilter;
import com.otaliastudios.cameraview.filters.EMInterferenceFilter;
import com.otaliastudios.cameraview.filters.EarlybirdFilter;
import com.otaliastudios.cameraview.filters.EdgeDetectionFilter;
import com.otaliastudios.cameraview.filters.ExposureFilter;
import com.otaliastudios.cameraview.filters.FillLightFilter;
import com.otaliastudios.cameraview.filters.GammaFilter;
import com.otaliastudios.cameraview.filters.GrainFilter;
import com.otaliastudios.cameraview.filters.GrayscaleFilter;
import com.otaliastudios.cameraview.filters.HalftoneFilter;
import com.otaliastudios.cameraview.filters.HighlightShadowFilter;
import com.otaliastudios.cameraview.filters.HueFilter;
import com.otaliastudios.cameraview.filters.InvertColorsFilter;
import com.otaliastudios.cameraview.filters.LomoishFilter;
import com.otaliastudios.cameraview.filters.LuminanceThresholdFilter;
import com.otaliastudios.cameraview.filters.PosterizeFilter;
import com.otaliastudios.cameraview.filters.SaturationFilter;
import com.otaliastudios.cameraview.filters.SepiaFilter;
import com.otaliastudios.cameraview.filters.SharpnessFilter;
import com.otaliastudios.cameraview.filters.TemperatureFilter;
import com.otaliastudios.cameraview.filters.TintFilter;
import com.otaliastudios.cameraview.filters.VignetteFilter;
import com.otaliastudios.cameraview.filters.WavesReflectionFilter;

/**
 * Contains commonly used {@link Filter}s.
 *
 * You can use {@link #newInstance()} to create a new instance and
 * pass it to {@link com.otaliastudios.cameraview.CameraView#setFilter(Filter)}.
 */
public enum Filters {

    /** @see NoFilter */
    NONE(NoFilter.class),

    /** @see AutoFixFilter */
    AUTO_FIX(AutoFixFilter.class),

    /** @see BlackAndWhiteFilter */
    BLACK_AND_WHITE(BlackAndWhiteFilter.class),

    /** @see BrightnessFilter */
    BRIGHTNESS(BrightnessFilter.class),

    /** @see ContrastFilter */
    CONTRAST(ContrastFilter.class),

    /** @see CrossProcessFilter */
    CROSS_PROCESS(CrossProcessFilter.class),

    /** @see DocumentaryFilter */
    DOCUMENTARY(DocumentaryFilter.class),

    /** @see DuotoneFilter */
    DUOTONE(DuotoneFilter.class),

    /** @see FillLightFilter */
    FILL_LIGHT(FillLightFilter.class),

    /** @see GammaFilter */
    GAMMA(GammaFilter.class),

    /** @see GrainFilter */
    GRAIN(GrainFilter.class),

    /** @see GrayscaleFilter */
    GRAYSCALE(GrayscaleFilter.class),

    /** @see HueFilter */
    HUE(HueFilter.class),

    /** @see InvertColorsFilter */
    INVERT_COLORS(InvertColorsFilter.class),

    /** @see LomoishFilter */
    LOMOISH(LomoishFilter.class),

    /** @see PosterizeFilter */
    POSTERIZE(PosterizeFilter.class),

    /** @see SaturationFilter */
    SATURATION(SaturationFilter.class),

    /** @see SepiaFilter */
    SEPIA(SepiaFilter.class),

    /** @see SharpnessFilter */
    SHARPNESS(SharpnessFilter.class),

    /** @see TemperatureFilter */
    TEMPERATURE(TemperatureFilter.class),

    /** @see TintFilter */
    TINT(TintFilter.class),

    /** @see VignetteFilter */
    VIGNETTE(VignetteFilter.class),

    COLOR_SPACE(ColorspaceFilter.class),

    EXPOSURE(ExposureFilter.class),

    HIGHLIGHT_SHADOW(HighlightShadowFilter.class),

    HALF_TONE(HalftoneFilter.class),

    LUMINANCE_THRESHOLD(LuminanceThresholdFilter.class),

    WAVES_REFLECTION(WavesReflectionFilter.class),

    EM_INTERFERENCE(EMInterferenceFilter.class),

    CHROMATIC_ABERRATION(ChromaticAberrationFilter.class),

    EDGE_DETECTION(EdgeDetectionFilter.class),

    EARLY_BIRD(EarlybirdFilter.class),

    BRANNAN(ToasterFilter.class);

    private Class<? extends Filter> filterClass;

    Filters(@NonNull Class<? extends Filter> filterClass) {
        this.filterClass = filterClass;
    }

    /**
     * Returns a new instance of the given filter.
     * @return a new instance
     */
    @NonNull
    public Filter newInstance() {
        try {
            return filterClass.newInstance();
        } catch (IllegalAccessException e) {
            return new NoFilter();
        } catch (InstantiationException e) {
            return new NoFilter();
        }
    }
}

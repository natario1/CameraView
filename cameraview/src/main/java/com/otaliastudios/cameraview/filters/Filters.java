package com.otaliastudios.cameraview.filters;

public enum Filters {
    NO_FILTER,

    AUTO_FIX_FILTER,
    BLACK_AND_WHITE_FILTER,
    BRIGHTNESS_FILTER,
    CONTRAST_FILTER,
    CROSS_PROCESS_FILTER,
    DOCUMENTARY_FILTER,
    DUO_TONE_COLOR_FILTER,
    FILL_LIGHT_FILTER,
    GAMMA_FILTER,
    GRAIN_FILTER,
    GREY_SCALE_FILTER,
    HUE_FILTER,
    INVERT_COLOR_FILTER,
    LAMOISH_FILTER,
    POSTERIZE_FILTER,
    SATURATION_FILTER,
    SEPIA_FILTER,
    SHARPNESS_FILTER,
    TEMPERATURE_FILTER,
    TINT_FILTER,
    VIGNETTE_FILTER;

    public Filter newInstance() {
        Filter shaderEffect;
        switch (this) {

            case AUTO_FIX_FILTER:
                shaderEffect = new AutoFixFilter();
                break;

            case BLACK_AND_WHITE_FILTER:
                shaderEffect = new BlackAndWhiteFilter();
                break;

            case BRIGHTNESS_FILTER:
                shaderEffect = new BrightnessFilter();
                break;

            case CONTRAST_FILTER:
                shaderEffect = new ContrastFilter();
                break;

            case CROSS_PROCESS_FILTER:
                shaderEffect = new CrossProcessFilter();
                break;

            case DOCUMENTARY_FILTER:
                shaderEffect = new DocumentaryFilter();
                break;

            case DUO_TONE_COLOR_FILTER:
                shaderEffect = new DuotoneFilter();
                break;

            case FILL_LIGHT_FILTER:
                shaderEffect = new FillLightFilter();
                break;

            case GAMMA_FILTER:
                shaderEffect = new GammaFilter();
                break;

            case GRAIN_FILTER:
                shaderEffect = new GrainFilter();
                break;

            case GREY_SCALE_FILTER:
                shaderEffect = new GreyScaleFilter();
                break;

            case HUE_FILTER:
                shaderEffect = new HueFilter();
                break;

            case INVERT_COLOR_FILTER:
                shaderEffect = new InvertColorsFilter();
                break;

            case LAMOISH_FILTER:
                shaderEffect = new LamoishFilter();
                break;

            case POSTERIZE_FILTER:
                shaderEffect = new PosterizeFilter();
                break;

            case SATURATION_FILTER:
                shaderEffect = new SaturationFilter();
                break;

            case SEPIA_FILTER:
                shaderEffect = new SepiaFilter();
                break;

            case SHARPNESS_FILTER:
                shaderEffect = new SharpnessFilter();
                break;

            case TEMPERATURE_FILTER:
                shaderEffect = new TemperatureFilter();
                break;

            case TINT_FILTER:
                shaderEffect = new TintFilter();
                break;

            case VIGNETTE_FILTER:
                shaderEffect = new VignetteFilter();
                break;


            case NO_FILTER:
            default:
                shaderEffect = new NoFilter();
        }

        return shaderEffect;
    }
}

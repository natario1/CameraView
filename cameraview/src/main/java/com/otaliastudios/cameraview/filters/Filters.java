package com.otaliastudios.cameraview.filters;

public enum Filters {
    NO_EFFECT,

    AUTO_FIX_EFFECT,
    BLACK_AND_WHITE_EFFECT,
    BRIGHTNESS_EFFECT,
    CONTRAST_EFFECT,
    CROSS_PROCESS_EFFECT,
    DOCUMENTARY_EFFECT,
    DUO_TONE_COLOR_EFFECT,
    FILL_LIGHT_EFFECT,
    GAMMA_EFFECT,
    GRAIN_EFFECT,
    GREY_SCALE_EFFECT,
    HUE_EFFECT,
    INVERT_COLOR_EFFECT,
    LAMOISH_EFFECT,
    POSTERIZE_EFFECT,
    SATURATION_EFFECT,
    SEPIA_EFFECT,
    SHARPNESS_EFFECT,
    TEMPERATURE_EFFECT,
    TINT_EFFECT,
    VIGNETTE_EFFECT;

    public Filter newInstance() {
        Filter shaderEffect;
        switch (this) {

            case AUTO_FIX_EFFECT:
                shaderEffect = new AutoFixEffect();
                break;

            case BLACK_AND_WHITE_EFFECT:
                shaderEffect = new BlackAndWhiteEffect();
                break;

            case BRIGHTNESS_EFFECT:
                shaderEffect = new BrightnessEffect();
                break;

            case CONTRAST_EFFECT:
                shaderEffect = new ContrastEffect();
                break;

            case CROSS_PROCESS_EFFECT:
                shaderEffect = new CrossProcessEffect();
                break;

            case DOCUMENTARY_EFFECT:
                shaderEffect = new DocumentaryEffect();
                break;

            case DUO_TONE_COLOR_EFFECT:
                shaderEffect = new DuotoneEffect();
                break;

            case FILL_LIGHT_EFFECT:
                shaderEffect = new FillLightEffect();
                break;

            case GAMMA_EFFECT:
                shaderEffect = new GammaEffect();
                break;

            case GRAIN_EFFECT:
                shaderEffect = new GrainEffect();
                break;

            case GREY_SCALE_EFFECT:
                shaderEffect = new GreyScaleEffect();
                break;

            case HUE_EFFECT:
                shaderEffect = new HueEffect();
                break;

            case INVERT_COLOR_EFFECT:
                shaderEffect = new InvertColorsEffect();
                break;

            case LAMOISH_EFFECT:
                shaderEffect = new LamoishEffect();
                break;

            case POSTERIZE_EFFECT:
                shaderEffect = new PosterizeEffect();
                break;

            case SATURATION_EFFECT:
                shaderEffect = new SaturationEffect();
                break;

            case SEPIA_EFFECT:
                shaderEffect = new SepiaEffect();
                break;

            case SHARPNESS_EFFECT:
                shaderEffect = new SharpnessEffect();
                break;

            case TEMPERATURE_EFFECT:
                shaderEffect = new TemperatureEffect();
                break;

            case TINT_EFFECT:
                shaderEffect = new TintEffect();
                break;

            case VIGNETTE_EFFECT:
                shaderEffect = new VignetteEffect();
                break;


            case NO_EFFECT:
            default:
                shaderEffect = new NoFilterEffect();
        }

        return shaderEffect;
    }
}

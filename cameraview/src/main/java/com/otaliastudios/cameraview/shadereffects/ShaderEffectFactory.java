package com.otaliastudios.cameraview.shadereffects;

import android.opengl.GLSurfaceView;

import com.otaliastudios.cameraview.shadereffects.effects.AutoFixEffect;
import com.otaliastudios.cameraview.shadereffects.effects.BlackAndWhiteEffect;
import com.otaliastudios.cameraview.shadereffects.effects.BrightnessEffect;
import com.otaliastudios.cameraview.shadereffects.effects.ContrastEffect;
import com.otaliastudios.cameraview.shadereffects.effects.CrossProcessEffect;
import com.otaliastudios.cameraview.shadereffects.effects.DocumentaryEffect;
import com.otaliastudios.cameraview.shadereffects.effects.DuotoneEffect;
import com.otaliastudios.cameraview.shadereffects.effects.FillLightEffect;
import com.otaliastudios.cameraview.shadereffects.effects.GammaEffect;
import com.otaliastudios.cameraview.shadereffects.effects.GrainEffect;
import com.otaliastudios.cameraview.shadereffects.effects.GreyScaleEffect;
import com.otaliastudios.cameraview.shadereffects.effects.HueEffect;
import com.otaliastudios.cameraview.shadereffects.effects.InvertColorsEffect;
import com.otaliastudios.cameraview.shadereffects.effects.LamoishEffect;
import com.otaliastudios.cameraview.shadereffects.effects.NoFilterEffect;
import com.otaliastudios.cameraview.shadereffects.effects.PosterizeEffect;
import com.otaliastudios.cameraview.shadereffects.effects.SaturationEffect;
import com.otaliastudios.cameraview.shadereffects.effects.SepiaEffect;
import com.otaliastudios.cameraview.shadereffects.effects.SharpnessEffect;
import com.otaliastudios.cameraview.shadereffects.effects.TemperatureEffect;
import com.otaliastudios.cameraview.shadereffects.effects.TintEffect;
import com.otaliastudios.cameraview.shadereffects.effects.VignetteEffect;

public class ShaderEffectFactory {

    public enum ShaderEffects {
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
        VIGNETTE_EFFECT
    }

    public static BaseShaderEffect getShaderFromFactory(ShaderEffects effect, GLSurfaceView glSurfaceView) {
        BaseShaderEffect shaderEffect;
        switch (effect) {

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

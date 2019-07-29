package com.otaliastudios.cameraview.shadereffects;

import android.opengl.GLSurfaceView;

import com.otaliastudios.cameraview.shadereffects.effects.BlackAndWhiteEffect;
import com.otaliastudios.cameraview.shadereffects.effects.InvertColorsEffect;
import com.otaliastudios.cameraview.shadereffects.effects.LamoishEffect;
import com.otaliastudios.cameraview.shadereffects.effects.NoEffect;
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

        BLACK_AND_WHITE_EFFECT,
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
            case BLACK_AND_WHITE_EFFECT:
                shaderEffect = new BlackAndWhiteEffect();
                break;

            case INVERT_COLOR_EFFECT:
                shaderEffect = new InvertColorsEffect();
                break;

            case LAMOISH_EFFECT:
                shaderEffect = new LamoishEffect(glSurfaceView);
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
                shaderEffect = new SharpnessEffect(glSurfaceView);
                break;

            case TEMPERATURE_EFFECT:
                shaderEffect = new TemperatureEffect();
                break;

            case TINT_EFFECT:
                shaderEffect = new TintEffect();
                break;

            case VIGNETTE_EFFECT:
                shaderEffect = new VignetteEffect(glSurfaceView);
                break;


            case NO_EFFECT:
            default:
                shaderEffect = new NoEffect();
        }

        return shaderEffect;
    }
}

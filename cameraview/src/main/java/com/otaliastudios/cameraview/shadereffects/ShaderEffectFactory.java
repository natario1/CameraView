package com.otaliastudios.cameraview.shadereffects;

import android.opengl.GLSurfaceView;

import com.otaliastudios.cameraview.shadereffects.effects.BlackAndWhiteEffect;
import com.otaliastudios.cameraview.shadereffects.effects.NoEffect;
import com.otaliastudios.cameraview.shadereffects.effects.SharpnessEffect;
import com.otaliastudios.cameraview.shadereffects.effects.TemperatureEffect;
import com.otaliastudios.cameraview.shadereffects.effects.TintEffect;
import com.otaliastudios.cameraview.shadereffects.effects.VignetteEffect;

public class ShaderEffectFactory {

    public enum ShaderEffects {
        BLACK_AND_WHITE_EFFECT,
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


            default:
                shaderEffect = new NoEffect();
        }

        return shaderEffect;
    }
}

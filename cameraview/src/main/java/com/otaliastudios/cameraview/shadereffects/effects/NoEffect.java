package com.otaliastudios.cameraview.shadereffects.effects;

import com.otaliastudios.cameraview.shadereffects.BaseShaderEffect;

/**
 * Displays the normal preview without any effect.
 */
public class NoEffect extends BaseShaderEffect {
    /**
     * Initialize
     */
    public NoEffect() {
    }

    @Override
    public String getFragmentShader() {

        String shader = "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "varying vec2 vTextureCoord;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "void main() {\n"
                + "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
                + "}\n";

        return shader;

    }
}
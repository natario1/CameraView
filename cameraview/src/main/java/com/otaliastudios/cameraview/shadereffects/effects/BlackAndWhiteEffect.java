package com.otaliastudios.cameraview.shadereffects.effects;

import com.otaliastudios.cameraview.shadereffects.BaseShaderEffect;

/**
 * Converts the preview into black and white colors
 */
public class BlackAndWhiteEffect extends BaseShaderEffect {


    /**
     * Initialize effect
     */
    public BlackAndWhiteEffect(){
    }

    @Override
    public String getFragmentShader() {

        String shader = "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "varying vec2 vTextureCoord;\n"
                + "uniform samplerExternalOES sTexture;\n" + "void main() {\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  float colorR = (color.r + color.g + color.b) / 3.0;\n"
                + "  float colorG = (color.r + color.g + color.b) / 3.0;\n"
                + "  float colorB = (color.r + color.g + color.b) / 3.0;\n"
                + "  gl_FragColor = vec4(colorR, colorG, colorB, color.a);\n"
                + "}\n";

        return shader;

    }
}

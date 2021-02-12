package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Inverts the input colors. This is also known as negative effect.
 */
public class InvertColorsFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  float colorR = (1.0 - color.r) / 1.0;\n"
            + "  float colorG = (1.0 - color.g) / 1.0;\n"
            + "  float colorB = (1.0 - color.b) / 1.0;\n"
            + "  gl_FragColor = vec4(colorR, colorG, colorB, color.a);\n"
            + "}\n";

    public InvertColorsFilter() { }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }
}

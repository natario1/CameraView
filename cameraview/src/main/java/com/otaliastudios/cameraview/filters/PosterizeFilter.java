package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Applies a posterization effect to the input frames.
 */
public class PosterizeFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n" + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  vec3 pcolor;\n"
            + "  pcolor.r = (color.r >= 0.5) ? 0.75 : 0.25;\n"
            + "  pcolor.g = (color.g >= 0.5) ? 0.75 : 0.25;\n"
            + "  pcolor.b = (color.b >= 0.5) ? 0.75 : 0.25;\n"
            + "  gl_FragColor = vec4(pcolor, color.a);\n"
            + "}\n";

    public PosterizeFilter() { }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }
}

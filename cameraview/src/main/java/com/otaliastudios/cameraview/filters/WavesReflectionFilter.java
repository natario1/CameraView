package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

public class WavesReflectionFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "\n" +
            "uniform vec3                iResolution;\n" +
            "uniform samplerExternalOES  iChannel0;\n" +
            "varying vec2                "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n" +
            "\n" +
            "float waterLevel = 0.5;\n" +
            "float waveAmplitude = 0.01;\n" +
            "\n" +
            "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
            "  {\n" +
            "     if(fragCoord.y >= waterLevel){\n" +
            "        fragColor = texture2D(iChannel0, fragCoord);\n" +
            "     }else{\n" +
            "        fragColor = texture2D(iChannel0,vec2(fragCoord.x + fract(sin(dot(fragCoord.xy ,vec2(12.9898,78.233))) * 43758.5453) * waveAmplitude,\n" +
            "       \t\t\t\t2.0 * waterLevel - fragCoord.y));\n" +
            "     }\n" +
            "   }\n" +
            "\n" +
            "void main() {\n" +
            " \tmainImage(gl_FragColor, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n" +
            " }";

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }
}

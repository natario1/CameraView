package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;

public class ColorspaceFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +

                    "varying vec2 vTextureCoord;" +
                    "uniform samplerExternalOES sTexture;" +

                    "void main() {" +
                    "highp vec2 sampleDivisor = vec2(1.0 / 200.0, 1.0 / 320.0);" +

                    "highp vec2 samplePos = vTextureCoord - mod(vTextureCoord, sampleDivisor);" +
                    "highp vec4 color = texture2D(sTexture, samplePos);" +

                    "mediump vec4 colorCyan = vec4(85.0 / 255.0, 1.0, 1.0, 1.0);" +
                    "mediump vec4 colorMagenta = vec4(1.0, 85.0 / 255.0, 1.0, 1.0);" +
                    "mediump vec4 colorWhite = vec4(1.0, 1.0, 1.0, 1.0);" +
                    "mediump vec4 colorBlack = vec4(0.0, 0.0, 0.0, 1.0);" +

                    "mediump vec4 endColor;" +
                    "highp float blackDistance = distance(color, colorBlack);" +
                    "highp float whiteDistance = distance(color, colorWhite);" +
                    "highp float magentaDistance = distance(color, colorMagenta);" +
                    "highp float cyanDistance = distance(color, colorCyan);" +

                    "mediump vec4 finalColor;" +

                    "highp float colorDistance = min(magentaDistance, cyanDistance);" +
                    "colorDistance = min(colorDistance, whiteDistance);" +
                    "colorDistance = min(colorDistance, blackDistance);" +

                    "if (colorDistance == blackDistance) {" +
                    "finalColor = colorBlack;" +
                    "} else if (colorDistance == whiteDistance) {" +
                    "finalColor = colorWhite;" +
                    "} else if (colorDistance == cyanDistance) {" +
                    "finalColor = colorCyan;" +
                    "} else {" +
                    "finalColor = colorMagenta;" +
                    "}" +

                    "gl_FragColor = finalColor;" +
                    "}";

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }
}

package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Applies a cross process effect, in which the red and green channels
 * are enhanced while the blue channel is restricted.
 */
public class CrossProcessFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  vec3 ncolor = vec3(0.0, 0.0, 0.0);\n"
            + "  float value;\n"
            + "  if (color.r < 0.5) {\n"
            + "    value = color.r;\n"
            + "  } else {\n"
            + "    value = 1.0 - color.r;\n"
            + "  }\n"
            + "  float red = 4.0 * value * value * value;\n"
            + "  if (color.r < 0.5) {\n"
            + "    ncolor.r = red;\n"
            + "  } else {\n"
            + "    ncolor.r = 1.0 - red;\n"
            + "  }\n"
            + "  if (color.g < 0.5) {\n"
            + "    value = color.g;\n"
            + "  } else {\n"
            + "    value = 1.0 - color.g;\n"
            + "  }\n"
            + "  float green = 2.0 * value * value;\n"
            + "  if (color.g < 0.5) {\n"
            + "    ncolor.g = green;\n"
            + "  } else {\n"
            + "    ncolor.g = 1.0 - green;\n"
            + "  }\n"
            + "  ncolor.b = color.b * 0.5 + 0.25;\n"
            + "  gl_FragColor = vec4(ncolor.rgb, color.a);\n"
            + "}\n";

    public CrossProcessFilter() { }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }
}

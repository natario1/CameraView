package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Converts frames to sepia tone.
 */
public class SepiaFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "mat3 matrix;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "void main() {\n"
            + "  matrix[0][0]=" + 805.0f / 2048.0f + ";\n"
            + "  matrix[0][1]=" + 715.0f / 2048.0f + ";\n"
            + "  matrix[0][2]=" + 557.0f / 2048.0f + ";\n"
            + "  matrix[1][0]=" + 1575.0f / 2048.0f + ";\n"
            + "  matrix[1][1]=" + 1405.0f / 2048.0f + ";\n"
            + "  matrix[1][2]=" + 1097.0f / 2048.0f + ";\n"
            + "  matrix[2][0]=" + 387.0f / 2048.0f + ";\n"
            + "  matrix[2][1]=" + 344.0f / 2048.0f + ";\n"
            + "  matrix[2][2]=" + 268.0f / 2048.0f + ";\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  vec3 new_color = min(matrix * color.rgb, 1.0);\n"
            + "  gl_FragColor = vec4(new_color.rgb, color.a);\n"
            + "}\n";

    public SepiaFilter() { }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }
}

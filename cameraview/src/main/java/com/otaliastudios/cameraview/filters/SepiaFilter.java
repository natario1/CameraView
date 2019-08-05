package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

/**
 * Converts preview to Sepia tone.
 */
public class SepiaFilter extends BaseFilter {

    public SepiaFilter() { }

    @NonNull
    @Override
    public String getFragmentShader() {
        float[] weights = {805.0f / 2048.0f, 715.0f / 2048.0f,
                557.0f / 2048.0f, 1575.0f / 2048.0f, 1405.0f / 2048.0f,
                1097.0f / 2048.0f, 387.0f / 2048.0f, 344.0f / 2048.0f,
                268.0f / 2048.0f};
        String[] matrixString = new String[9];

        matrixString[0] = "  matrix[0][0]=" + weights[0] + ";\n";
        matrixString[1] = "  matrix[0][1]=" + weights[1] + ";\n";
        matrixString[2] = "  matrix[0][2]=" + weights[2] + ";\n";
        matrixString[3] = "  matrix[1][0]=" + weights[3] + ";\n";
        matrixString[4] = "  matrix[1][1]=" + weights[4] + ";\n";
        matrixString[5] = "  matrix[1][2]=" + weights[5] + ";\n";
        matrixString[6] = "  matrix[2][0]=" + weights[6] + ";\n";
        matrixString[7] = "  matrix[2][1]=" + weights[7] + ";\n";
        matrixString[8] = "  matrix[2][2]=" + weights[8] + ";\n";

        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "mat3 matrix;\n"
                + "varying vec2 vTextureCoord;\n"
                + "void main() {\n"
                + matrixString[0] + matrixString[1] + matrixString[2]
                + matrixString[3] + matrixString[4] + matrixString[5]
                + matrixString[6] + matrixString[7] + matrixString[8]
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  vec3 new_color = min(matrix * color.rgb, 1.0);\n"
                + "  gl_FragColor = vec4(new_color.rgb, color.a);\n"
                + "}\n";

    }
}

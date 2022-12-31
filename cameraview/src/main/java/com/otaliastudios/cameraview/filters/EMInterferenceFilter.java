package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.opengl.core.Egloo;

import java.nio.FloatBuffer;

public class EMInterferenceFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "uniform float               iGlobalTime;\n" +
                    "uniform samplerExternalOES  iChannel0;\n" +
                    "varying vec2                " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ";\n" +
                    "\n" +
                    "float rng2(vec2 seed)\n" +
                    "{\n" +
                    "    return fract(sin(dot(seed * floor(iGlobalTime * 12.), vec2(127.1,311.7))) * 43758.5453123);\n" +
                    "}\n" +
                    "\n" +
                    "float rng(float seed)\n" +
                    "{\n" +
                    "    return rng2(vec2(seed, 1.0));\n" +
                    "}\n" +
                    "\n" +
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "{\n" +
                    "\tvec2 uv = fragCoord.xy;\n" +
                    "    vec2 blockS = floor(uv * vec2(24., 9.));\n" +
                    "    vec2 blockL = floor(uv * vec2(8., 4.));\n" +
                    "\n" +
                    "    float r = rng2(uv);\n" +
                    "    vec3 noise = (vec3(r, 1. - r, r / 2. + 0.5) * 1.0 - 2.0) * 0.08;\n" +
                    "\n" +
                    "    float lineNoise = pow(rng2(blockS), 8.0) * pow(rng2(blockL), 3.0) - pow(rng(7.2341), 17.0) * 2.;\n" +
                    "\n" +
                    "    vec4 col1 = texture2D(iChannel0, uv);\n" +
                    "    vec4 col2 = texture2D(iChannel0, uv + vec2(lineNoise * 0.05 * rng(5.0), 0));\n" +
                    "    vec4 col3 = texture2D(iChannel0, uv - vec2(lineNoise * 0.05 * rng(31.0), 0));\n" +
                    "\n" +
                    "\tfragColor = vec4(vec3(col1.x, col2.y, col3.z) + noise, 1.0);\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "\tmainImage(gl_FragColor, " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ");\n" +
                    "}";

    long START_TIME = System.currentTimeMillis();
    private int iGlobalTimeLocation = -1;

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        iGlobalTimeLocation = -1;
        START_TIME = System.currentTimeMillis();
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        iGlobalTimeLocation = GLES20.glGetUniformLocation(programHandle, "iGlobalTime");
        Egloo.checkGlProgramLocation(iGlobalTimeLocation, "iGlobalTime");
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        float time = ((float) (System.currentTimeMillis() - START_TIME)) / 1000.0f;
        GLES20.glUniform1f(iGlobalTimeLocation, time);
        Egloo.checkGlError("glUniform1f");
    }
}

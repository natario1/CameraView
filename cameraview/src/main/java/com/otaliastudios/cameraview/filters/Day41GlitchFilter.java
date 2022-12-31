package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.opengl.core.Egloo;

import java.nio.FloatBuffer;

public class Day41GlitchFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "uniform vec3                iResolution;\n" +
                    "uniform float               iTime;\n" +
                    "uniform samplerExternalOES           iChannel0;\n" +
                    "varying vec2                " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ";\n" +
                   "float rand(vec2 p)\n" +
                    "{\n" +
                    "    float t = floor(iTime * 20.) / 10.;\n" +
                    "    return fract(sin(dot(p, vec2(t * 12.9898, t * 78.233))) * 43758.5453);\n" +
                    "}\n" +
                    "\n" +
                    "float noise(vec2 uv, float blockiness)\n" +
                    "{   \n" +
                    "    vec2 lv = fract(uv);\n" +
                    "    vec2 id = floor(uv);\n" +
                    "    \n" +
                    "    float n1 = rand(id);\n" +
                    "    float n2 = rand(id+vec2(1,0));\n" +
                    "    float n3 = rand(id+vec2(0,1));\n" +
                    "    float n4 = rand(id+vec2(1,1));\n" +
                    "    \n" +
                    "    vec2 u = smoothstep(0.0, 1.0 + blockiness, lv);\n" +
                    "\n" +
                    "    return mix(mix(n1, n2, u.x), mix(n3, n4, u.x), u.y);\n" +
                    "}\n" +
                    "\n" +
                    "float fbm(vec2 uv, int count, float blockiness, float complexity)\n" +
                    "{\n" +
                    "    float val = 0.0;\n" +
                    "    float amp = 0.5;\n" +
                    "    \n" +
                    "    while(count != 0)\n" +
                    "    {\n" +
                    "    \tval += amp * noise(uv, blockiness);\n" +
                    "        amp *= 0.5;\n" +
                    "        uv *= complexity;    \n" +
                    "        count--;\n" +
                    "    }\n" +
                    "    \n" +
                    "    return val;\n" +
                    "}\n" +
                    "\n" +
                    "const float glitchAmplitude = 0.2; // increase this\n" +
                    "const float glitchNarrowness = 4.0;\n" +
                    "const float glitchBlockiness = 2.0;\n" +
                    "const float glitchMinimizer = 8.0; // decrease this\n" +
                    "\n" +
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "{\n" +
                    "    \n" +
                    "    vec2 uv = fragCoord/iResolution.xy;\n" +
                    "    float aspect = iResolution.x / iResolution.y;\n" +
                    "    vec2 a = vec2(uv.x * aspect , uv.y);\n" +
                    "    vec2 uv2 = vec2(a.x / iResolution.x, exp(a.y));\n" +
                    "\n" +
                    "    \n" +
                    "    float shift = glitchAmplitude * pow(fbm(uv2, 4, glitchBlockiness, glitchNarrowness), glitchMinimizer);\n" +
                    "    \n" +
                    "    float colR = texture2D(iChannel0, vec2(uv.x + shift, uv.y)).r * (1. - shift);\n" +
                    "    float colG = texture2D(iChannel0, vec2(uv.x - shift, uv.y)).g * (1. - shift);\n" +
                    "    float colB = texture2D(iChannel0, vec2(uv.x - shift, uv.y)).b * (1. - shift);\n" +
                    "    \n" +
                    "    // Mix with the scanline effect\n" +
                    "    vec3 f = vec3(colR, colG, colB);\n" +
                    "\n" +
                    "    \n" +
                    "    fragColor = vec4(f, 1.);\n" +
                    "}" +
                    "\nvoid main() {\n" +
                    "\tmainImage(gl_FragColor, " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + " * iResolution.xy);\n" +
                    "}";

    long START_TIME = System.currentTimeMillis();
    private int iGlobalTimeLocation = -1;
    private int iResolutionLocation = -1;

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        iResolutionLocation = -1;
        iGlobalTimeLocation = -1;
        START_TIME = System.currentTimeMillis();
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        iResolutionLocation = GLES20.glGetUniformLocation(programHandle, "iResolution");
        Egloo.checkGlProgramLocation(iResolutionLocation, "iResolution");


        iGlobalTimeLocation = GLES20.glGetUniformLocation(programHandle, "iTime");
        Egloo.checkGlProgramLocation(iGlobalTimeLocation, "iTime");
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);

        Size size = getSize();
        if (size != null) {
            GLES20.glUniform3fv(iResolutionLocation, 1,
                    FloatBuffer.wrap(new float[]{(float) size.getWidth(), (float) size.getHeight(), 1.0f}));
            Egloo.checkGlError("glUniform3fv");
        }


        float time = (((float) (System.currentTimeMillis() - START_TIME)) / 1000.0f) + 1;
        GLES20.glUniform1f(iGlobalTimeLocation, time);
        Egloo.checkGlError("glUniform1f");
    }
}

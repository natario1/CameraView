package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.opengl.core.Egloo;

public class ChromaticAberrationFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "\n" +
                    "uniform vec3                iResolution;\n" +
                    "uniform float               iGlobalTime;\n" +
                    "uniform samplerExternalOES           iChannel0;\n" +
                    "varying vec2                "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n" +
                    "\n" +
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "{\n" +
                    "    vec2 uv = fragCoord.xy;\n" +
                    "\n" +
                    "\tfloat amount = 0.0;\n" +
                    "\t\n" +
                    "\tamount = (1.0 + sin(iGlobalTime*6.0)) * 0.5;\n" +
                    "\tamount *= 1.0 + sin(iGlobalTime*16.0) * 0.5;\n" +
                    "\tamount *= 1.0 + sin(iGlobalTime*19.0) * 0.5;\n" +
                    "\tamount *= 1.0 + sin(iGlobalTime*27.0) * 0.5;\n" +
                    "\tamount = pow(amount, 3.0);\n" +
                    "\n" +
                    "\tamount *= 0.05;\n" +
                    "\t\n" +
                    "    vec3 col;\n" +
                    "    col.r = texture2D( iChannel0, vec2(uv.x+amount,uv.y) ).r;\n" +
                    "    col.g = texture2D( iChannel0, uv ).g;\n" +
                    "    col.b = texture2D( iChannel0, vec2(uv.x-amount,uv.y) ).b;\n" +
                    "\n" +
                    "\tcol *= (1.0 - amount * 0.5);\n" +
                    "\t\n" +
                    "    fragColor = vec4(col,1.0);\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "void main() {\n" +
                    " \tmainImage(gl_FragColor, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n" +
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

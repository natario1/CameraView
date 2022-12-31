package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

public class MirrorFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
                    "\n" +
                    "uniform samplerExternalOES           iChannel0;\n" +
                    "varying vec2                " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ";\n" +
                    "\n" +
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "  {\n" +
                    "    vec2 flipCoord = vec2(1.0-fragCoord.x, fragCoord.y);\n" +
                    "    if(flipCoord.x >= 0.5){\n" +
                    "    \tfragColor = texture2D(iChannel0, vec2( flipCoord.x - 0.5, flipCoord.y ));\n" +
                    "    } else {\n" +
                    "    \tfragColor = texture2D(iChannel0, vec2(  0.5 - flipCoord.x,flipCoord.y ));\n" +
                    "    }\n" +
                    "   }\n" +
                    "\n" +
                    "void main() {\n" +
                    " \tmainImage(gl_FragColor, " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ");\n" +
                    " }";

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
    }
}

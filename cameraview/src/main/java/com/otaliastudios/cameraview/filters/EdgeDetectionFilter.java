package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

public class EdgeDetectionFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "#extension GL_OES_standard_derivatives : enable\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "uniform samplerExternalOES           iChannel0;\n" +
                    "varying vec2                " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ";\n" +
                    "\n" +
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "{\n" +
                    "    vec2 uv = fragCoord.xy;\n" +
                    "    vec4 color =  texture2D(iChannel0, fragCoord);\n" +
                    "    float gray = length(color.rgb);\n" +
                    "    fragColor = vec4(vec3(step(0.06, length(vec2(dFdx(gray), dFdy(gray))))), 1.0);\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    mainImage(gl_FragColor, " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ");\n" +
                    "}";

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

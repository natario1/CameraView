package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.opengl.core.Egloo;

import java.nio.FloatBuffer;

public class RainbowFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "uniform vec3                iResolution;\n" +
                    "uniform samplerExternalOES           iChannel0;\n" +
                    "uniform float               iTime;\n" +
                    "varying vec2                " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ";\n" +
                    "\n" +
                    "vec2 size = vec2(50.0, 50.0);\n" +
                    "vec2 distortion = vec2(20.0, 20.0);\n" +
                    "float speed = 0.75;" +
                    "\n" +
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "{\n" +
                    "   vec2 transformed = vec2(\n" +
                    "       fragCoord.x + sin(fragCoord.y / size.x + iTime * speed) * distortion.x,\n" +
                    "       fragCoord.y + cos(fragCoord.x / size.y + iTime * speed) * distortion.y\n" +
                    "   );\n" +
                    "   vec2 relCoord = fragCoord.xy / iResolution.xy;\n" +
                    "   fragColor = texture2D(iChannel0, fragCoord/iResolution.xy) + vec4(\n" +
                        "        (cos(relCoord.x + iTime * speed * 4.0) + 1.0) / 2.0,\n" +
                        "        (relCoord.x + relCoord.y) / 2.0,\n" +
                        "        (sin(relCoord.y + iTime * speed) + 1.0) / 2.0,\n" +
                        "        0\n" +
                    "           );\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    mainImage(gl_FragColor, " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ");\n" +
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

        float time = ((float) (System.currentTimeMillis() - START_TIME)) / 1000.0f;
        GLES20.glUniform1f(iGlobalTimeLocation, time);
        Egloo.checkGlError("glUniform1f");
    }
}

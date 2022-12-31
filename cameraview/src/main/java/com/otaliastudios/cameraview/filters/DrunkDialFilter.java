package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.opengl.core.Egloo;

import java.nio.FloatBuffer;

public class DrunkDialFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "uniform vec3                iResolution;\n" +
                    "uniform float               iTime;\n" +
                    "uniform samplerExternalOES           iChannel0;\n" +
                    "varying vec2                " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ";\n" +
                 "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "{\n" +
                    "\t// Just playing around with shaders for my first time.\n" +
                    "\t// Pardon the unoptimized mess.\n" +
                    "\t// -Dan\n" +
                    "\tfloat drunk = sin(iTime*2.0)*6.0;\n" +
                    "\tfloat unitDrunk1 = (sin(iTime*1.2)+1.0)/2.0;\n" +
                    "\tfloat unitDrunk2 = (sin(iTime*1.8)+1.0)/2.0;\n" +
                    "\n" +
                    "\tvec2 normalizedCoord = mod((fragCoord.xy + vec2(0, drunk)) / iResolution.xy, 1.0);\n" +
                    "\tnormalizedCoord.x = pow(normalizedCoord.x, mix(1.25, 0.85, unitDrunk1));\n" +
                    "\tnormalizedCoord.y = pow(normalizedCoord.y, mix(0.85, 1.25, unitDrunk2));\n" +
                    "\n" +
                    "\tvec2 normalizedCoord2 = mod((fragCoord.xy + vec2(drunk, 0)) / iResolution.xy, 1.0);\t\n" +
                    "\tnormalizedCoord2.x = pow(normalizedCoord2.x, mix(0.95, 1.1, unitDrunk2));\n" +
                    "\tnormalizedCoord2.y = pow(normalizedCoord2.y, mix(1.1, 0.95, unitDrunk1));\n" +
                    "\n" +
                    "\tvec2 normalizedCoord3 = fragCoord.xy/iResolution.xy;\n" +
                    "\t\n" +
                    "\tvec4 color = texture2D(iChannel0, normalizedCoord);\t\n" +
                    "\tvec4 color2 = texture2D(iChannel0, normalizedCoord2);\n" +
                    "\tvec4 color3 = texture2D(iChannel0, normalizedCoord3);\n" +
                    "\n" +
                    "\t// Mess with colors and test swizzling\n" +
                    "\tcolor.x = sqrt(color2.x);\n" +
                    "\tcolor2.x = sqrt(color2.x);\n" +
                    "\t\n" +
                    "\tvec4 finalColor = mix( mix(color, color2, mix(0.4, 0.6, unitDrunk1)), color3, 0.4);\n" +
                    "\t\n" +
                    "\t// \n" +
                    "\tif (length(finalColor) > 1.4)\n" +
                    "\t\tfinalColor.xy = mix(finalColor.xy, normalizedCoord3, 0.5);\n" +
                    "\telse if (length(finalColor) < 0.4)\n" +
                    "\t\tfinalColor.yz = mix(finalColor.yz, normalizedCoord3, 0.5);\n" +
                    "\t\t\n" +
                    "\tfragColor = finalColor;\t\t\n" +
                    "}\n"+
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

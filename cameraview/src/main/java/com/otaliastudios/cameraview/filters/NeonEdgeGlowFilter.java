package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.opengl.core.Egloo;

import java.nio.FloatBuffer;

public class NeonEdgeGlowFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "uniform vec3                iResolution;\n" +
                    "uniform float               iTime;\n" +
                    "uniform samplerExternalOES           iChannel0;\n" +
                    "varying vec2                " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ";\n" +
                    "\n" +
                    "/* Returns rgb vec from input 0-1 */\n" +
                    "vec3 getRainbowColor(in float val) {\n" +
                    "    /*convert to rainbow RGB*/\n" +
                    "    float a = (1.0 - val) * 6.0;\n" +
                    "    int X = int(floor(a));\n" +
                    "    float Y = a - float(X);\n" +
                    "    float r = 0.;\n" +
                    "    float g = 0.;\n" +
                    "    float b = 0.;\n" +
                    "    if (X == 0) {\n" +
                    "        r = 1.; g = Y; b = 0.;\n" +
                    "    } else if (X == 1) {\n" +
                    "        r = 1. - Y; g = 1.; b = 0.;\n" +
                    "    } else if (X == 2) {\n" +
                    "        r = 0.; g = 1.; b = Y;\n" +
                    "    } else if (X == 3) {\n" +
                    "        r = 0.; g = 1. - Y; b = 1.;\n" +
                    "    } else if (X == 4) {\n" +
                    "        r = Y; g = 0.; b = 1.;\n" +
                    "    } else if (X == 5) {\n" +
                    "        r = 1.; g = 0.; b = 1. - Y;\n" +
                    "    } else {\n" +
                    "        r = 0.; g = 0.; b = 0.;\n" +
                    "    }\n" +
                    "    return vec3(57,255,20);\n" +
                    "}\n" +
                    "\n" +
                    "float d;\n" +
                    "\n" +
                    "float lookup(vec2 p, float dx, float dy, float edgeIntensity)\n" +
                    "{\n" +
                    "    vec2 uv = (p.xy + vec2(dx * edgeIntensity, dy * edgeIntensity)) / iResolution.xy;\n" +
                    "    vec4 c = texture2D(iChannel0, uv.xy);\n" +
                    "\t\n" +
                    "\t// return as luma\n" +
                    "    return 0.2126*c.r + 0.7152*c.g + 0.0722*c.b;\n" +
                    "}\n" +
                    "\n" +
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "{\n" +
                    "    float timeNorm = mod(iTime, 5.) / 5.;\n" +
                    "    vec3 glowCol = getRainbowColor(timeNorm);\n" +
                    "    float edgeIntensity = 1.;\n" +
                    "    if (timeNorm < .5) { edgeIntensity += (4. * timeNorm);}\n" +
                    "    else { edgeIntensity += -4. * (timeNorm - 1.); }\n" +
                    "    vec2 p = fragCoord.xy;\n" +
                    "    \n" +
                    "\t// simple sobel edge detection\n" +
                    "    float gx = 0.0;\n" +
                    "    gx += -1.0 * lookup(p, -1.0, -1.0, edgeIntensity);\n" +
                    "    gx += -2.0 * lookup(p, -1.0,  0.0, edgeIntensity);\n" +
                    "    gx += -1.0 * lookup(p, -1.0,  1.0, edgeIntensity);\n" +
                    "    gx +=  1.0 * lookup(p,  1.0, -1.0, edgeIntensity);\n" +
                    "    gx +=  2.0 * lookup(p,  1.0,  0.0, edgeIntensity);\n" +
                    "    gx +=  1.0 * lookup(p,  1.0,  1.0, edgeIntensity);\n" +
                    "    \n" +
                    "    float gy = 0.0;\n" +
                    "    gy += -1.0 * lookup(p, -1.0, -1.0, edgeIntensity);\n" +
                    "    gy += -2.0 * lookup(p,  0.0, -1.0, edgeIntensity);\n" +
                    "    gy += -1.0 * lookup(p,  1.0, -1.0, edgeIntensity);\n" +
                    "    gy +=  1.0 * lookup(p, -1.0,  1.0, edgeIntensity);\n" +
                    "    gy +=  2.0 * lookup(p,  0.0,  1.0, edgeIntensity);\n" +
                    "    gy +=  1.0 * lookup(p,  1.0,  1.0, edgeIntensity);\n" +
                    "    \n" +
                    "\t// hack: use g^2 to conceal noise in the video\n" +
                    "    float g = gx*gx + gy*gy;\n" +
                    "    \n" +
                    "    vec4 col = texture2D(iChannel0, p / iResolution.xy);\n" +
                    "    col += vec4(g * glowCol, 1.0);\n" +
                    "    \n" +
                    "    fragColor = col;\n" +
                    "}" +
                    "\n" +
                    "void main() {\n" +
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

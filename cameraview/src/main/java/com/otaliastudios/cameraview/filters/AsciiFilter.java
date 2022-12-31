package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.opengl.core.Egloo;

import java.nio.FloatBuffer;

public class AsciiFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "uniform vec3                iResolution;\n" +
                    "uniform samplerExternalOES           iChannel0;\n" +
                    "varying vec2                " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ";\n" +
                    "float character(int n, vec2 p)\n" +
                    "{\n" +
                    "\tp = floor(p*vec2(4.0, -4.0) + 2.5);\n" +
                    "    if (clamp(p.x, 0.0, 4.0) == p.x)\n" +
                    "\t{\n" +
                    "        if (clamp(p.y, 0.0, 4.0) == p.y)\t\n" +
                    "\t\t{\n" +
                    "        \tint a = int(round(p.x) + 5.0 * round(p.y));\n" +
                    "\t\t\tif (((n >> a) & 1) == 1) return 1.0;\n" +
                    "\t\t}\t\n" +
                    "    }\n" +
                    "\treturn 0.0;\n" +
                    "}\n" +
                    "\n" +
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "{\n" +
                    "\tvec2 pix = fragCoord.xy;\n" +
                    "\tvec3 col = texture2D(iChannel0, floor(pix/8.0)*8.0/iResolution.xy).rgb;\t\n" +
                    "\t\n" +
                    "\tfloat gray = 0.3 * col.r + 0.59 * col.g + 0.11 * col.b;\n" +
                    "\t\n" +
                    "\tint n =  4096;                // .\n" +
                    "\tif (gray > 0.2) n = 65600;    // :\n" +
                    "\tif (gray > 0.3) n = 332772;   // *\n" +
                    "\tif (gray > 0.4) n = 15255086; // o \n" +
                    "\tif (gray > 0.5) n = 23385164; // &\n" +
                    "\tif (gray > 0.6) n = 15252014; // 8\n" +
                    "\tif (gray > 0.7) n = 13199452; // @\n" +
                    "\tif (gray > 0.8) n = 11512810; // #\n" +
                    "\t\n" +
                    "\tvec2 p = mod(pix/4.0, 2.0) - vec2(1.0);\n" +
                    "    \n" +
                    "\tif (iMouse.z > 0.5)\tcol = gray*vec3(character(n, p));\n" +
                    "\telse col = col*character(n, p);\n" +
                    "\t\n" +
                    "\tfragColor = vec4(col, 1.0);\n" +
                    "}" +
                    "\nvoid main() {\n" +
                    "\tmainImage(gl_FragColor, " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + " * iResolution.xy);\n" +
                    "}";
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
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        iResolutionLocation = GLES20.glGetUniformLocation(programHandle, "iResolution");
        Egloo.checkGlProgramLocation(iResolutionLocation, "iResolution");
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
    }
}

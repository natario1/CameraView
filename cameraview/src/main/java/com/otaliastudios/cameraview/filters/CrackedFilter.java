package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.opengl.core.Egloo;

import java.nio.FloatBuffer;

public class CrackedFilter extends BaseFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "#extension GL_OES_standard_derivatives : enable\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "uniform vec3                iResolution;\n" +
//                    "uniform float               iGlobalTime;\n" +
                    "uniform samplerExternalOES           iChannel0;\n" +
                    "varying vec2                " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ";\n" +
                    "\n" +
                    "float rnd(vec2 s)\n" +
                    "{\n" +
                    "    return 1.-2.*fract(sin(s.x*253.13+s.y*341.41)*589.19);\n" +
                    "}\n" +
                    "\n" +
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
                    "{\n" +
                    "\tvec2 p=(fragCoord.xy*2.-iResolution.xy)/iResolution.x;\n" +
                    "\n" +
                    "    vec2 v=vec2(1E3);\n" +
                    "    vec2 v2=vec2(1E4);\n" +
                    "    vec2 center=vec2(.1,-.5);\n" +
                    "    for(int c=0;c<30;c++)\n" +
                    "    {\n" +
                    "        float angle=floor(rnd(vec2(float(c),387.44))*16.)*3.1415*.4-.5;\n" +
                    "        float dist=pow(rnd(vec2(float(c),78.21)),2.)*.5;\n" +
                    "        vec2 vc=vec2(center.x+cos(angle)*dist+rnd(vec2(float(c),349.3))*7E-3,\n" +
                    "                     center.y+sin(angle)*dist+rnd(vec2(float(c),912.7))*7E-3);\n" +
                    "        if(length(vc-p)<length(v-p))\n" +
                    "        {\n" +
                    "\t        v2=v;\n" +
                    "\t        v=vc;\n" +
                    "        }\n" +
                    "        else if(length(vc-p)<length(v2-p))\n" +
                    "        {\n" +
                    "            v2=vc;\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    float col=abs(length(dot(p-v,normalize(v-v2)))-length(dot(p-v2,normalize(v-v2))))+.002*length(p-center);\n" +
                    "    col=7E-4/col;\n" +
                    "    if(length(v-v2)<4E-3)col=0.;\n" +
                    "//    if(length(v-p)<4E-3)col=1E-6;\n" +
                    "    if(col<.3)col=0.;\n" +
                    "    vec4 tex=texture2D(iChannel0,(fragCoord.xy)/iResolution.xy+rnd(v)*.02);\n" +
                    "    fragColor=col*vec4(vec3(1.-tex.xyz),1.)+(1.-col)*tex;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "\tmainImage(gl_FragColor, " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + " * iResolution.xy);\n" +
                    "}";


    long START_TIME = System.currentTimeMillis();
//    private int iGlobalTimeLocation = -1;
    private int iResolutionLocation = -1;

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        iGlobalTimeLocation = -1;
//        START_TIME = System.currentTimeMillis();
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        iResolutionLocation = GLES20.glGetUniformLocation(programHandle, "iResolution");
        Egloo.checkGlProgramLocation(iResolutionLocation, "iResolution");

//        iGlobalTimeLocation = GLES20.glGetUniformLocation(programHandle, "iGlobalTime");
//        Egloo.checkGlProgramLocation(iGlobalTimeLocation, "iGlobalTime");
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
//        GLES20.glUniform1f(iGlobalTimeLocation, time);
        Egloo.checkGlError("glUniform1f");
    }
}

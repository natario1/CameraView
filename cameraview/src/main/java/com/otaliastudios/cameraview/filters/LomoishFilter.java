package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.opengl.core.Egloo;

import java.util.Random;

/**
 * Applies a lomo-camera style effect to the input frames.
 */
public class LomoishFilter extends BaseFilter {

    private final static Random RANDOM = new Random();
    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float stepsizeX;\n"
            + "uniform float stepsizeY;\n"
            + "uniform vec2 scale;\n"
            + "uniform float inv_max_dist;\n"
            + "vec2 seed;\n"
            + "float stepsize;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "float rand(vec2 loc) {\n"
            + "  float theta1 = dot(loc, vec2(0.9898, 0.233));\n"
            + "  float theta2 = dot(loc, vec2(12.0, 78.0));\n"
            + "  float value = cos(theta1) * sin(theta2) + sin(theta1) * cos(theta2);\n"
            // keep value of part1 in range: (2^-14 to 2^14).
            + "  float temp = mod(197.0 * value, 1.0) + value;\n"
            + "  float part1 = mod(220.0 * temp, 1.0) + temp;\n"
            + "  float part2 = value * 0.5453;\n"
            + "  float part3 = cos(theta1 + theta2) * 0.43758;\n"
            + "  return fract(part1 + part2 + part3);\n"
            + "}\n"
            + "void main() {\n"
            + "  seed[0] = " + RANDOM.nextFloat() + ";\n"
            + "  seed[1] = " + RANDOM.nextFloat() + ";\n"
            + "  stepsize = " + 1.0f / 255.0f + ";\n"
            // sharpen
            + "  vec3 nbr_color = vec3(0.0, 0.0, 0.0);\n"
            + "  vec2 coord;\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  coord.x = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".x - 0.5 * stepsizeX;\n"
            + "  coord.y = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".y - stepsizeY;\n"
            + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
            + "  coord.x = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".x - stepsizeX;\n"
            + "  coord.y = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".y + 0.5 * stepsizeY;\n"
            + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
            + "  coord.x = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".x + stepsizeX;\n"
            + "  coord.y = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".y - 0.5 * stepsizeY;\n"
            + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
            + "  coord.x = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".x + stepsizeX;\n"
            + "  coord.y = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+".y + 0.5 * stepsizeY;\n"
            + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
            + "  vec3 s_color = vec3(color.rgb + 0.3 * nbr_color);\n"
            // cross process
            + "  vec3 c_color = vec3(0.0, 0.0, 0.0);\n"
            + "  float value;\n"
            + "  if (s_color.r < 0.5) {\n"
            + "    value = s_color.r;\n"
            + "  } else {\n"
            + "    value = 1.0 - s_color.r;\n"
            + "  }\n"
            + "  float red = 4.0 * value * value * value;\n"
            + "  if (s_color.r < 0.5) {\n"
            + "    c_color.r = red;\n"
            + "  } else {\n"
            + "    c_color.r = 1.0 - red;\n"
            + "  }\n"
            + "  if (s_color.g < 0.5) {\n"
            + "    value = s_color.g;\n"
            + "  } else {\n"
            + "    value = 1.0 - s_color.g;\n"
            + "  }\n"
            + "  float green = 2.0 * value * value;\n"
            + "  if (s_color.g < 0.5) {\n"
            + "    c_color.g = green;\n"
            + "  } else {\n"
            + "    c_color.g = 1.0 - green;\n"
            + "  }\n"
            + "  c_color.b = s_color.b * 0.5 + 0.25;\n"
            // blackwhite
            + "  float dither = rand("+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+" + seed);\n"
            + "  vec3 xform = clamp((c_color.rgb - 0.15) * 1.53846, 0.0, 1.0);\n"
            + "  vec3 temp = clamp((color.rgb + stepsize - 0.15) * 1.53846, 0.0, 1.0);\n"
            + "  vec3 bw_color = clamp(xform + (temp - xform) * (dither - 0.5), 0.0, 1.0);\n"
            // vignette
            + "  coord = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+" - vec2(0.5, 0.5);\n"
            + "  float dist = length(coord * scale);\n"
            + "  float lumen = 0.85 / (1.0 + exp((dist * inv_max_dist - 0.73) * 20.0)) + 0.15;\n"
            + "  gl_FragColor = vec4(bw_color * lumen, color.a);\n"
            + "}\n";

    private int width = 1;
    private int height = 1;

    private int scaleLocation = -1;
    private int maxDistLocation = -1;
    private int stepSizeXLocation = -1;
    private int stepSizeYLocation = -1;

    public LomoishFilter() { }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.width = width;
        this.height = height;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        scaleLocation = GLES20.glGetUniformLocation(programHandle, "scale");
        Egloo.checkGlProgramLocation(scaleLocation, "scale");
        maxDistLocation = GLES20.glGetUniformLocation(programHandle, "inv_max_dist");
        Egloo.checkGlProgramLocation(maxDistLocation, "inv_max_dist");
        stepSizeXLocation = GLES20.glGetUniformLocation(programHandle, "stepsizeX");
        Egloo.checkGlProgramLocation(stepSizeXLocation, "stepsizeX");
        stepSizeYLocation = GLES20.glGetUniformLocation(programHandle, "stepsizeY");
        Egloo.checkGlProgramLocation(stepSizeYLocation, "stepsizeY");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scaleLocation = -1;
        maxDistLocation = -1;
        stepSizeXLocation = -1;
        stepSizeYLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        float[] scale = new float[2];
        if (width > height) {
            scale[0] = 1f;
            scale[1] = ((float) height) / width;
        } else {
            scale[0] = ((float) width) / height;
            scale[1] = 1f;
        }
        float maxDist = ((float) Math.sqrt(scale[0] * scale[0] + scale[1] * scale[1])) * 0.5f;
        GLES20.glUniform2fv(scaleLocation, 1, scale, 0);
        Egloo.checkGlError("glUniform2fv");
        GLES20.glUniform1f(maxDistLocation, 1.0F / maxDist);
        Egloo.checkGlError("glUniform1f");
        GLES20.glUniform1f(stepSizeXLocation, 1.0F / width);
        Egloo.checkGlError("glUniform1f");
        GLES20.glUniform1f(stepSizeYLocation, 1.0F / height);
        Egloo.checkGlError("glUniform1f");
    }
}

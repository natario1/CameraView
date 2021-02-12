package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.opengl.core.Egloo;

import java.util.Random;

/**
 * Applies black and white documentary style effect.
 */
public class DocumentaryFilter extends BaseFilter {

    private final static Random RANDOM = new Random();
    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "vec2 seed;\n"
            + "float stepsize;\n"
            + "uniform float inv_max_dist;\n"
            + "uniform vec2 scale;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "float rand(vec2 loc) {\n"
            + "  float theta1 = dot(loc, vec2(0.9898, 0.233));\n"
            + "  float theta2 = dot(loc, vec2(12.0, 78.0));\n"
            + "  float value = cos(theta1) * sin(theta2) + sin(theta1) * cos(theta2);\n"
            +
            // keep value of part1 in range: (2^-14 to 2^14).
            "  float temp = mod(197.0 * value, 1.0) + value;\n"
            + "  float part1 = mod(220.0 * temp, 1.0) + temp;\n"
            + "  float part2 = value * 0.5453;\n"
            + "  float part3 = cos(theta1 + theta2) * 0.43758;\n"
            + "  return fract(part1 + part2 + part3);\n"
            + "}\n"
            + "void main() {\n"
            + "  seed[0] = " + RANDOM.nextFloat() + ";\n"
            + "  seed[1] = " + RANDOM.nextFloat() + ";\n"
            + "  stepsize = " + 1.0f / 255.0f + ";\n"

            // black white
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  float dither = rand("+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+" + seed);\n"
            + "  vec3 xform = clamp(2.0 * color.rgb, 0.0, 1.0);\n"
            + "  vec3 temp = clamp(2.0 * (color.rgb + stepsize), 0.0, 1.0);\n"
            + "  vec3 new_color = clamp(xform + (temp - xform) * (dither - 0.5), 0.0, 1.0);\n"
            // grayscale
            + "  float gray = dot(new_color, vec3(0.299, 0.587, 0.114));\n"
            + "  new_color = vec3(gray, gray, gray);\n"
            // vignette
            + "  vec2 coord = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+" - vec2(0.5, 0.5);\n"
            + "  float dist = length(coord * scale);\n"
            + "  float lumen = 0.85 / (1.0 + exp((dist * inv_max_dist - 0.83) * 20.0)) + 0.15;\n"
            + "  gl_FragColor = vec4(new_color * lumen, color.a);\n"
            + "}\n";

    private int mWidth = 1;
    private int mHeight = 1;
    private int mScaleLocation = -1;
    private int mMaxDistLocation = -1;

    public DocumentaryFilter() { }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        mWidth = width;
        mHeight = height;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        mScaleLocation = GLES20.glGetUniformLocation(programHandle, "scale");
        Egloo.checkGlProgramLocation(mScaleLocation, "scale");
        mMaxDistLocation = GLES20.glGetUniformLocation(programHandle, "inv_max_dist");
        Egloo.checkGlProgramLocation(mMaxDistLocation, "inv_max_dist");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScaleLocation = -1;
        mMaxDistLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        float[] scale = new float[2];
        if (mWidth > mHeight) {
            scale[0] = 1f;
            scale[1] = ((float) mHeight) / mWidth;
        } else {
            scale[0] = ((float) mWidth) / mHeight;
            scale[1] = 1f;
        }
        GLES20.glUniform2fv(mScaleLocation, 1, scale, 0);
        Egloo.checkGlError("glUniform2fv");

        float maxDist = ((float) Math.sqrt(scale[0] * scale[0] + scale[1] * scale[1])) * 0.5f;
        float invMaxDist = 1F / maxDist;
        GLES20.glUniform1f(mMaxDistLocation, invMaxDist);
        Egloo.checkGlError("glUniform1f");

    }
}

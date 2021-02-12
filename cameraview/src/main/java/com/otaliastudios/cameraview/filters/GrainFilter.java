package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

import java.util.Random;

/**
 * Applies film grain effect to the frames.
 */
public class GrainFilter extends BaseFilter implements OneParameterFilter {

    private final static Random RANDOM = new Random();
    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "vec2 seed;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "uniform samplerExternalOES tex_sampler_0;\n"
            + "uniform samplerExternalOES tex_sampler_1;\n"
            + "uniform float scale;\n"
            + "uniform float stepX;\n"
            + "uniform float stepY;\n"
            + "float rand(vec2 loc) {\n"
            + "  float theta1 = dot(loc, vec2(0.9898, 0.233));\n"
            + "  float theta2 = dot(loc, vec2(12.0, 78.0));\n"
            + "  float value = cos(theta1) * sin(theta2) + sin(theta1) * cos(theta2);\n"
            // keep value of part1 in range: (2^-14 to 2^14).
            + "  float temp = mod(197.0 * value, 1.0) + value;\n"
            + "  float part1 = mod(220.0 * temp, 1.0) + temp;\n"
            + "  float part2 = value * 0.5453;\n"
            + "  float part3 = cos(theta1 + theta2) * 0.43758;\n"
            + "  float sum = (part1 + part2 + part3);\n"
            + "  return fract(sum)*scale;\n"
            + "}\n"
            + "void main() {\n"
            + "  seed[0] = " + RANDOM.nextFloat() + ";\n"
            + "  seed[1] = " + RANDOM.nextFloat() + ";\n"
            + "  float noise = texture2D(tex_sampler_1, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME
            + " + vec2(-stepX, -stepY)).r * 0.224;\n"
            + "  noise += texture2D(tex_sampler_1, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME
            + " + vec2(-stepX, stepY)).r * 0.224;\n"
            + "  noise += texture2D(tex_sampler_1, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME
            + " + vec2(stepX, -stepY)).r * 0.224;\n"
            + "  noise += texture2D(tex_sampler_1, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME
            + " + vec2(stepX, stepY)).r * 0.224;\n"
            + "  noise += 0.4448;\n"
            + "  noise *= scale;\n"
            + "  vec4 color = texture2D(tex_sampler_0, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME
            + ");\n"
            + "  float energy = 0.33333 * color.r + 0.33333 * color.g + 0.33333 * color.b;\n"
            + "  float mask = (1.0 - sqrt(energy));\n"
            + "  float weight = 1.0 - 1.333 * mask * noise;\n"
            + "  gl_FragColor = vec4(color.rgb * weight, color.a);\n"
            + "  gl_FragColor = gl_FragColor+vec4(rand("+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME
            + " + seed), rand("+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+" + seed),rand("
            + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+" + seed),1);\n"
            + "}\n";

    private float strength = 0.5f;
    private int width = 1;
    private int height = 1;
    private int strengthLocation = -1;
    private int stepXLocation = -1;
    private int stepYLocation = -1;

    public GrainFilter() { }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.width = width;
        this.height = height;
    }

    /**
     * Sets the current distortion strength.
     * 0.0: no distortion.
     * 1.0: maximum distortion.
     *
     * @param strength strength
     */
    @SuppressWarnings("WeakerAccess")
    public void setStrength(float strength) {
        if (strength < 0.0f) strength = 0.0f;
        if (strength > 1.0f) strength = 1.0f;
        this.strength = strength;
    }

    /**
     * Returns the current strength.
     *
     * @see #setStrength(float)
     * @return strength
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public float getStrength() {
        return strength;
    }

    @Override
    public void setParameter1(float value) {
        setStrength(value);
    }

    @Override
    public float getParameter1() {
        return getStrength();
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        strengthLocation = GLES20.glGetUniformLocation(programHandle, "scale");
        Egloo.checkGlProgramLocation(strengthLocation, "scale");
        stepXLocation = GLES20.glGetUniformLocation(programHandle, "stepX");
        Egloo.checkGlProgramLocation(stepXLocation, "stepX");
        stepYLocation = GLES20.glGetUniformLocation(programHandle, "stepY");
        Egloo.checkGlProgramLocation(stepYLocation, "stepY");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        strengthLocation = -1;
        stepXLocation = -1;
        stepYLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(strengthLocation, strength);
        Egloo.checkGlError("glUniform1f");
        GLES20.glUniform1f(stepXLocation, 0.5f / width);
        Egloo.checkGlError("glUniform1f");
        GLES20.glUniform1f(stepYLocation, 0.5f / height);
        Egloo.checkGlError("glUniform1f");
    }
}

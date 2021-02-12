package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * Attempts to auto-fix the frames based on histogram equalization.
 */
public class AutoFixFilter extends BaseFilter implements OneParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES tex_sampler_0;\n"
            + "uniform samplerExternalOES tex_sampler_1;\n"
            + "uniform samplerExternalOES tex_sampler_2;\n"
            + "uniform float scale;\n"
            + "float shift_scale;\n"
            + "float hist_offset;\n"
            + "float hist_scale;\n"
            + "float density_offset;\n"
            + "float density_scale;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "void main() {\n"
            + "  shift_scale = " + (1.0f / 256f) + ";\n"
            + "  hist_offset = " + (0.5f / 766f) + ";\n"
            + "  hist_scale = " + (765f / 766f) + ";\n"
            + "  density_offset = " + (0.5f / 1024f) + ";\n"
            + "  density_scale = " + (1023f / 1024f) + ";\n"
            + "  const vec3 weights = vec3(0.33333, 0.33333, 0.33333);\n"
            + "  vec4 color = texture2D(tex_sampler_0, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME
            + ");\n"
            + "  float energy = dot(color.rgb, weights);\n"
            + "  float mask_value = energy - 0.5;\n"
            + "  float alpha;\n"
            + "  if (mask_value > 0.0) {\n"
            + "    alpha = (pow(2.0 * mask_value, 1.5) - 1.0) * scale + 1.0;\n"
            + "  } else { \n"
            + "    alpha = (pow(2.0 * mask_value, 2.0) - 1.0) * scale + 1.0;\n"
            + "  }\n"
            + "  float index = energy * hist_scale + hist_offset;\n"
            + "  vec4 temp = texture2D(tex_sampler_1, vec2(index, 0.5));\n"
            + "  float value = temp.g + temp.r * shift_scale;\n"
            + "  index = value * density_scale + density_offset;\n"
            + "  temp = texture2D(tex_sampler_2, vec2(index, 0.5));\n"
            + "  value = temp.g + temp.r * shift_scale;\n"
            + "  float dst_energy = energy * alpha + value * (1.0 - alpha);\n"
            + "  float max_energy = energy / max(color.r, max(color.g, color.b));\n"
            + "  if (dst_energy > max_energy) {\n"
            + "    dst_energy = max_energy;\n"
            + "  }\n"
            + "  if (energy == 0.0) {\n"
            + "    gl_FragColor = color;\n"
            + "  } else {\n"
            + "    gl_FragColor = vec4(color.rgb * dst_energy / energy, color.a);\n"
            + "  }\n"
            + "}\n";

    private float scale = 1.0f;
    private int scaleLocation = -1;

    public AutoFixFilter() { }

    /**
     * A parameter between 0 and 1. Zero means no adjustment, while 1 indicates
     * the maximum amount of adjustment.
     *
     * @param scale scale
     */
    public void setScale(float scale) {
        if (scale < 0.0f) scale = 0.0f;
        if (scale > 1.0f) scale = 1.0f;
        this.scale = scale;
    }

    /**
     * Returns the current scale.
     *
     * @see #setScale(float)
     * @return current scale
     */
    public float getScale() {
        return scale;
    }

    @Override
    public void setParameter1(float value) {
        setScale(value);
    }

    @Override
    public float getParameter1() {
        return getScale();
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scaleLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(scaleLocation, scale);
        Egloo.checkGlError("glUniform1f");
    }
}

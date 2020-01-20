package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.TwoParameterFilter;
import com.otaliastudios.opengl.core.Egloo;


/**
 * Applies a vignette effect to input frames.
 */
public class VignetteFilter extends BaseFilter implements TwoParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float range;\n"
            + "uniform float inv_max_dist;\n"
            + "uniform float shade;\n"
            + "uniform vec2 scale;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "void main() {\n"
            + "  const float slope = 20.0;\n"
            + "  vec2 coord = "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+" - vec2(0.5, 0.5);\n"
            + "  float dist = length(coord * scale);\n"
            + "  float lumen = shade / (1.0 + exp((dist * inv_max_dist - range) * slope)) "
            + "+ (1.0 - shade);\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  gl_FragColor = vec4(color.rgb * lumen, color.a);\n"
            + "}\n";

    private float mScale = 0.85f; // 0...1
    private float mShade = 0.5f; // 0...1
    private int mWidth = 1;
    private int mHeight = 1;

    private int mRangeLocation = -1;
    private int mMaxDistLocation = -1;
    private int mShadeLocation = -1;
    private int mScaleLocation = -1;

    public VignetteFilter() { }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        mWidth = width;
        mHeight = height;
    }

    /**
     * Sets the vignette effect scale (0.0 - 1.0).
     * @param scale new scale
     */
    @SuppressWarnings("WeakerAccess")
    public void setVignetteScale(float scale) {
        if (scale < 0.0f) scale = 0.0f;
        if (scale > 1.0f) scale = 1.0f;
        mScale = scale;
    }

    /**
     * Sets the vignette effect shade (0.0 - 1.0).
     * @param shade new shade
     */
    @SuppressWarnings("WeakerAccess")
    public void setVignetteShade(float shade) {
        if (shade < 0.0f) shade = 0.0f;
        if (shade > 1.0f) shade = 1.0f;
        this.mShade = shade;
    }

    /**
     * Gets the current vignette scale.
     *
     * @see #setVignetteScale(float)
     * @return scale
     */
    @SuppressWarnings("WeakerAccess")
    public float getVignetteScale() {
        return mScale;
    }

    /**
     * Gets the current vignette shade.
     *
     * @see #setVignetteShade(float)
     * @return shade
     */
    @SuppressWarnings("WeakerAccess")
    public float getVignetteShade() {
        return mShade;
    }


    @Override
    public void setParameter1(float value) {
        setVignetteScale(value);
    }

    @Override
    public float getParameter1() {
        return getVignetteScale();
    }

    @Override
    public void setParameter2(float value) {
        setVignetteShade(value);
    }

    @Override
    public float getParameter2() {
        return getVignetteShade();
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        mRangeLocation = GLES20.glGetUniformLocation(programHandle, "range");
        Egloo.checkGlProgramLocation(mRangeLocation, "range");
        mMaxDistLocation = GLES20.glGetUniformLocation(programHandle, "inv_max_dist");
        Egloo.checkGlProgramLocation(mMaxDistLocation, "inv_max_dist");
        mShadeLocation = GLES20.glGetUniformLocation(programHandle, "shade");
        Egloo.checkGlProgramLocation(mShadeLocation, "shade");
        mScaleLocation = GLES20.glGetUniformLocation(programHandle, "scale");
        Egloo.checkGlProgramLocation(mScaleLocation, "scale");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRangeLocation = -1;
        mMaxDistLocation = -1;
        mShadeLocation = -1;
        mScaleLocation = -1;
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
        GLES20.glUniform1f(mMaxDistLocation, 1F / maxDist);
        Egloo.checkGlError("glUniform1f");

        GLES20.glUniform1f(mShadeLocation, mShade);
        Egloo.checkGlError("glUniform1f");

        // The 'range' is between 1.3 to 0.6. When scale is zero then range is 1.3
        // which means no vignette at all because the luminousity difference is
        // less than 1/256 and will cause nothing.
        float range = (1.30f - (float) Math.sqrt(mScale) * 0.7f);
        GLES20.glUniform1f(mRangeLocation, range);
        Egloo.checkGlError("glUniform1f");
    }
}

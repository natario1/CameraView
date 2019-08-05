package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;


/**
 * Applies a vignette effect to input frames.
 */
public class VignetteFilter extends BaseFilter {

    private float mScale = 0.85f;
    private float mShade = 0.5f;
    private int mOutputWidth = 1;
    private int mOutputHeight = 1;

    @SuppressWarnings("WeakerAccess")
    public VignetteFilter() { }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        mOutputWidth = width;
        mOutputHeight = height;
    }

    /**
     * Sets the vignette effect scale (0.0 - 1.0).
     * @param scale new scale
     */
    @SuppressWarnings("WeakerAccess")
    public void setVignetteScale(float scale) {
        if (scale < 0.0f)
            scale = 0.0f;
        else if (scale > 1.0f)
            scale = 1.0f;
        this.mScale = scale;
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
    protected BaseFilter onCopy() {
        VignetteFilter filter = new VignetteFilter();
        filter.setVignetteScale(getVignetteScale());
        filter.setVignetteShade(getVignetteShade());
        return filter;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        float[] scale = new float[2];
        if (mOutputWidth > mOutputHeight) {
            scale[0] = 1f;
            scale[1] = ((float) mOutputHeight) / mOutputWidth;
        } else {
            scale[0] = ((float) mOutputWidth) / mOutputHeight;
            scale[1] = 1f;
        }
        float max_dist = ((float) Math.sqrt(scale[0] * scale[0] + scale[1]
                * scale[1])) * 0.5f;

        String[] scaleString = new String[2];
        scaleString[0] = "scale[0] = " + scale[0] + ";\n";
        scaleString[1] = "scale[1] = " + scale[1] + ";\n";
        String inv_max_distString = "inv_max_dist = " + 1.0f / max_dist + ";\n";
        String shadeString = "shade = " + mShade + ";\n";

        // The 'range' is between 1.3 to 0.6. When scale is zero then range is 1.3
        // which means no vignette at all because the luminousity difference is
        // less than 1/256 and will cause nothing.
        String rangeString = "range = "
                + (1.30f - (float) Math.sqrt(mScale) * 0.7f) + ";\n";

        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + " float range;\n"
                + " float inv_max_dist;\n"
                + " float shade;\n"
                + " vec2 scale;\n"
                + "varying vec2 vTextureCoord;\n"
                + "void main() {\n"
                // Parameters that were created above
                + scaleString[0]
                + scaleString[1]
                + inv_max_distString
                + shadeString
                + rangeString
                + "  const float slope = 20.0;\n"
                + "  vec2 coord = vTextureCoord - vec2(0.5, 0.5);\n"
                + "  float dist = length(coord * scale);\n"
                + "  float lumen = shade / (1.0 + exp((dist * inv_max_dist - range) * slope)) + (1.0 - shade);\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  gl_FragColor = vec4(color.rgb * lumen, color.a);\n"
                + "}\n";
    }
}

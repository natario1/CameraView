package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Sharpens the input frames.
 */
public class SharpnessFilter extends BaseFilter {

    private float scale = 0.5f;
    private int mOutputWidth = 1;
    private int mOutputHeight = 1;

    @SuppressWarnings("WeakerAccess")
    public SharpnessFilter() { }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        mOutputWidth = width;
        mOutputHeight = height;
    }

    /**
     * Sets the current sharpness value:
     * 0.0: no change.
     * 1.0: maximum sharpness.
     *
     * @param value new sharpness
     */
    @SuppressWarnings("WeakerAccess")
    public void setSharpness(float value) {
        if (value < 0.0f) value = 0.0f;
        if (value > 1.0f) value = 1.0f;
        this.scale = value;
    }

    /**
     * Returns the current sharpness.
     *
     * @see #setSharpness(float)
     * @return sharpness
     */
    @SuppressWarnings("WeakerAccess")
    public float getSharpness() {
        return scale;
    }

    @Override
    protected BaseFilter onCopy() {
        SharpnessFilter filter = new SharpnessFilter();
        filter.setSharpness(getSharpness());
        return filter;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        String stepsizeXString = "stepsizeX = " + 1.0f / mOutputWidth + ";\n";
        String stepsizeYString = "stepsizeY = " + 1.0f / mOutputHeight + ";\n";
        String scaleString = "scale = " + scale + ";\n";

        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + " float scale;\n"
                + " float stepsizeX;\n"
                + " float stepsizeY;\n"
                + "varying vec2 vTextureCoord;\n"
                + "void main() {\n"
                // Parameters that were created above
                + stepsizeXString
                + stepsizeYString
                + scaleString
                + "  vec3 nbr_color = vec3(0.0, 0.0, 0.0);\n"
                + "  vec2 coord;\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  coord.x = vTextureCoord.x - 0.5 * stepsizeX;\n"
                + "  coord.y = vTextureCoord.y - stepsizeY;\n"
                + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
                + "  coord.x = vTextureCoord.x - stepsizeX;\n"
                + "  coord.y = vTextureCoord.y + 0.5 * stepsizeY;\n"
                + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
                + "  coord.x = vTextureCoord.x + stepsizeX;\n"
                + "  coord.y = vTextureCoord.y - 0.5 * stepsizeY;\n"
                + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
                + "  coord.x = vTextureCoord.x + stepsizeX;\n"
                + "  coord.y = vTextureCoord.y + 0.5 * stepsizeY;\n"
                + "  nbr_color += texture2D(sTexture, coord).rgb - color.rgb;\n"
                + "  gl_FragColor = vec4(color.rgb - 2.0 * scale * nbr_color, color.a);\n"
                + "}\n";

    }

}

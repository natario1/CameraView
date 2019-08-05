package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Applies back-light filling to the frames.
 */
public class FillLightFilter extends BaseFilter {

    private float strength = 0.5f;

    @SuppressWarnings("WeakerAccess")
    public FillLightFilter() { }

    /**
     * Sets the current strength.
     * 0.0: no change.
     * 1.0: max strength.
     *
     * @param strength strength
     */
    @SuppressWarnings("WeakerAccess")
    public void setStrength(float strength) {
        if (strength < 0.0f) strength = 0f;
        if (strength > 1.0f) strength = 1f;
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
    protected BaseFilter onCopy() {
        FillLightFilter filter = new FillLightFilter();
        filter.setStrength(getStrength());
        return filter;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        float fade_gamma = 0.3f;
        float amt = 1.0f - strength;
        float mult = 1.0f / (amt * 0.7f + 0.3f);
        float faded = fade_gamma + (1.0f - fade_gamma) * mult;
        float igamma = 1.0f / faded;

        String multString = "mult = " + mult + ";\n";
        String igammaString = "igamma = " + igamma + ";\n";

        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + " float mult;\n"
                + " float igamma;\n"
                + "varying vec2 vTextureCoord;\n"
                + "void main() {\n"
                // Parameters that were created above
                + multString
                + igammaString
                + "  const vec3 color_weights = vec3(0.25, 0.5, 0.25);\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  float lightmask = dot(color.rgb, color_weights);\n"
                + "  float backmask = (1.0 - lightmask);\n"
                + "  vec3 ones = vec3(1.0, 1.0, 1.0);\n"
                + "  vec3 diff = pow(mult * color.rgb, igamma * ones) - color.rgb;\n"
                + "  diff = min(diff, 1.0);\n"
                + "  vec3 new_color = min(color.rgb + diff * backmask, 1.0);\n"
                + "  gl_FragColor = vec4(new_color, color.a);\n"
                + "}\n";
    }

}

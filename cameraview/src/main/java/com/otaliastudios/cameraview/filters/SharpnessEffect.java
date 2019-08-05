package com.otaliastudios.cameraview.filters;

import com.otaliastudios.cameraview.filters.Filter;

/**
 * Sharpens the preview.
 */
public class SharpnessEffect extends Filter {
    private float scale = 0.5f;

    /**
     * Initialize Effect
     */
    public SharpnessEffect() { }

    /**
     * @param value Float, between 0 and 1. 0 means no change.
     */
    public void setSharpnessValue(float value){
        if (value < 0.0f)
            value = 0.0f;
        else if (value > 1.0f)
            value = 1.0f;

        this.scale = value;
    }

    public float getSharpnessValue() {
        return scale;
    }

    @Override
    public String getFragmentShader() {

        String stepsizeXString = "stepsizeX = " + 1.0f / mPreviewingViewWidth + ";\n";
        String stepsizeYString = "stepsizeY = " + 1.0f / mPreviewingViewHeight + ";\n";
        String scaleString = "scale = " + scale + ";\n";

        String shader = "#extension GL_OES_EGL_image_external : require\n"
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

        return shader;

    }

}

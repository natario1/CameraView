package com.otaliastudios.cameraview.filters;

import androidx.annotation.NonNull;

/**
 * Adjusts the contrast of the preview.
 */
public class ContrastFilter extends Filter {
    private float contrast = 2.0f;

    /**
     * Initialize Effect
     */
    public ContrastFilter() {
    }

    /**
     * setContrast
     *
     * @param contrast Range should be between 0.0- 1.0 with 0.0 being normal.
     */
    public void setContrast(float contrast) {
        if (contrast < 0.0f)
            contrast = 0.0f;
        else if (contrast > 1.0f)
            contrast = 1.0f;

        //since the shader excepts a range of 1.0 - 2.0
        //will add the 1.0 to every value
        this.contrast = contrast + 1.0f;
    }

    public float getContrast() {
        //since the shader excepts a range of 1.0 - 2.0
        //to keep it between 0.0f - 1.0f range, will subtract the 1.0 to every value
        return contrast - 1.0f;
    }

    @NonNull
    @Override
    public String getFragmentShader() {

        String shader = "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + " float contrast;\n" + "varying vec2 vTextureCoord;\n"
                + "void main() {\n" + "  contrast =" + contrast + ";\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  color -= 0.5;\n" + "  color *= contrast;\n"
                + "  color += 0.5;\n" + "  gl_FragColor = color;\n" + "}\n";
        return shader;

    }

}

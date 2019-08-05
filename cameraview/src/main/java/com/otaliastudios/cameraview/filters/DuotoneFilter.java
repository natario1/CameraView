package com.otaliastudios.cameraview.filters;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Representation of input frames using only two color tones.
 */
public class DuotoneFilter extends BaseFilter {

    // Default values
    private int mFirstColor = Color.MAGENTA;
    private int mSecondColor = Color.YELLOW;

    @SuppressWarnings("WeakerAccess")
    public DuotoneFilter() { }

    /**
     * Sets the two duotone ARGB colors.
     * @param firstColor first
     * @param secondColor second
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setColors(@ColorInt int firstColor, @ColorInt int secondColor) {
        setFirstColor(firstColor);
        setSecondColor(secondColor);
    }

    /**
     * Sets the first of the duotone ARGB colors.
     * Defaults to {@link Color#MAGENTA}.
     *
     * @param color first color
     */
    @SuppressWarnings("WeakerAccess")
    public void setFirstColor(@ColorInt int color) {
        mFirstColor = color;
    }

    /**
     * Sets the second of the duotone ARGB colors.
     * Defaults to {@link Color#YELLOW}.
     *
     * @param color second color
     */
    @SuppressWarnings("WeakerAccess")
    public void setSecondColor(@ColorInt int color) {
        mSecondColor = color;
    }

    /**
     * Returns the first color.
     *
     * @see #setFirstColor(int)
     * @return first
     */
    @SuppressWarnings("unused")
    @ColorInt
    public int getFirstColor() {
        return mFirstColor;
    }

    /**
     * Returns the second color.
     *
     * @see #setSecondColor(int)
     * @return second
     */
    @SuppressWarnings("unused")
    @ColorInt
    public int getSecondColor() {
        return mSecondColor;
    }

    @Override
    protected BaseFilter onCopy() {
        DuotoneFilter filter = new DuotoneFilter();
        filter.setColors(mFirstColor, mSecondColor);
        return filter;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        float[] first = {Color.red(mFirstColor) / 255f,
                Color.green(mFirstColor) / 255f, Color.blue(mFirstColor) / 255f};
        float[] second = {Color.red(mSecondColor) / 255f,
                Color.green(mSecondColor) / 255f,
                Color.blue(mSecondColor) / 255f};

        String[] firstColorString = new String[3];
        String[] secondColorString = new String[3];

        firstColorString[0] = "first[0] = " + first[0] + ";\n";
        firstColorString[1] = "first[1] = " + first[1] + ";\n";
        firstColorString[2] = "first[2] = " + first[2] + ";\n";

        secondColorString[0] = "second[0] = " + second[0] + ";\n";
        secondColorString[1] = "second[1] = " + second[1] + ";\n";
        secondColorString[2] = "second[2] = " + second[2] + ";\n";

        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "vec3 first;\n"
                + "vec3 second;\n"
                + "varying vec2 vTextureCoord;\n"
                + "void main() {\n"
                // Parameters that were created above
                + firstColorString[0]
                + firstColorString[1]
                + firstColorString[2]
                + secondColorString[0]
                + secondColorString[1]
                + secondColorString[2]
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  float energy = (color.r + color.g + color.b) * 0.3333;\n"
                + "  vec3 new_color = (1.0 - energy) * first + energy * second;\n"
                + "  gl_FragColor = vec4(new_color.rgb, color.a);\n"
                + "}\n";
    }
}

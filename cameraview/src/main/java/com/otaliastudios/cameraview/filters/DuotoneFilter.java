package com.otaliastudios.cameraview.filters;

import android.graphics.Color;
import android.opengl.GLES20;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.internal.GlUtils;

/**
 * Representation of input frames using only two color tones.
 */
public class DuotoneFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform vec3 first;\n"
            + "uniform vec3 second;\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
            + "  float energy = (color.r + color.g + color.b) * 0.3333;\n"
            + "  vec3 new_color = (1.0 - energy) * first + energy * second;\n"
            + "  gl_FragColor = vec4(new_color.rgb, color.a);\n"
            + "}\n";

    // Default values
    private int mFirstColor = Color.MAGENTA;
    private int mSecondColor = Color.YELLOW;
    private int mFirstColorLocation = -1;
    private int mSecondColorLocation = -1;

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
    @SuppressWarnings({"unused", "WeakerAccess"})
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
    @SuppressWarnings({"unused", "WeakerAccess"})
    @ColorInt
    public int getSecondColor() {
        return mSecondColor;
    }


    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        mFirstColorLocation = GLES20.glGetUniformLocation(programHandle, "first");
        GlUtils.checkLocation(mFirstColorLocation, "first");
        mSecondColorLocation = GLES20.glGetUniformLocation(programHandle, "second");
        GlUtils.checkLocation(mSecondColorLocation, "second");
    }

    @Override
    protected void onPreDraw(float[] transformMatrix) {
        super.onPreDraw(transformMatrix);
        float[] first = new float[]{
                Color.red(mFirstColor) / 255f,
                Color.green(mFirstColor) / 255f,
                Color.blue(mFirstColor) / 255f
        };
        float[] second = new float[]{
                Color.red(mSecondColor) / 255f,
                Color.green(mSecondColor) / 255f,
                Color.blue(mSecondColor) / 255f
        };
        GLES20.glUniform3fv(mFirstColorLocation, 1, first, 0);
        GlUtils.checkError("glUniform3fv");
        GLES20.glUniform3fv(mSecondColorLocation, 1, second, 0);
        GlUtils.checkError("glUniform3fv");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFirstColorLocation = -1;
        mSecondColorLocation = -1;
    }

    @Override
    protected BaseFilter onCopy() {
        DuotoneFilter filter = new DuotoneFilter();
        filter.setColors(getFirstColor(), getSecondColor());
        return filter;
    }
}

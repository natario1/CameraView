package com.otaliastudios.cameraview.filters;

import android.graphics.Color;
import android.opengl.GLES20;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;


/**
 * Tints the frames with specified color.
 */
public class TintFilter extends BaseFilter implements OneParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform vec3 tint;\n"
            + "vec3 color_ratio;\n"
            + "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n"
            + "void main() {\n"
            + "  color_ratio[0] = " + 0.21f + ";\n"
            + "  color_ratio[1] = " + 0.71f + ";\n"
            + "  color_ratio[2] = " + 0.07f + ";\n"
            + "  vec4 color = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n"
            + "  float avg_color = dot(color_ratio, color.rgb);\n"
            + "  vec3 new_color = min(0.8 * avg_color + 0.2 * tint, 1.0);\n"
            + "  gl_FragColor = vec4(new_color.rgb, color.a);\n" + "}\n";

    private int tint = Color.RED;
    private int tintLocation = -1;

    public TintFilter() { }

    /**
     * Sets the current tint.
     * @param color current tint
     */
    @SuppressWarnings("WeakerAccess")
    public void setTint(@ColorInt int color) {
        // Remove any alpha.
        this.tint = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Returns the current tint.
     *
     * @see #setTint(int)
     * @return tint
     */
    @SuppressWarnings("WeakerAccess")
    @ColorInt
    public int getTint() {
        return tint;
    }

    @Override
    public void setParameter1(float value) {
        // no easy way to transform 0...1 into a color.
        setTint((int) (value * 0xFFFFFF));
    }

    @Override
    public float getParameter1() {
        int color = getTint();
        color = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color));
        return (float) color / 0xFFFFFF;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        tintLocation = GLES20.glGetUniformLocation(programHandle, "tint");
        Egloo.checkGlProgramLocation(tintLocation, "tint");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tintLocation = -1;
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        float[] channels = new float[]{
                Color.red(tint) / 255f,
                Color.green(tint) / 255f,
                Color.blue(tint) / 255f
        };
        GLES20.glUniform3fv(tintLocation, 1, channels, 0);
        Egloo.checkGlError("glUniform3fv");
    }
}

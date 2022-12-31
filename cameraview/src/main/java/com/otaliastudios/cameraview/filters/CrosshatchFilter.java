package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.TwoParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

public class CrosshatchFilter extends BaseFilter implements TwoParameterFilter {
    private static final String CROSSHATCH_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;" +
            "varying vec2 vTextureCoord;\n" +
            " uniform samplerExternalOES sTexture;\n" +
            "uniform highp float crossHatchSpacing;\n" +
            "uniform highp float lineWidth;\n" +
            "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
            "void main()\n" +
            "{\n" +
            "highp float luminance = dot(texture2D(sTexture, vTextureCoord).rgb, W);\n" +
            "lowp vec4 colorToDisplay = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "if (luminance < 1.00)\n" +
            "{\n" +
            "if (mod(vTextureCoord.x + vTextureCoord.y, crossHatchSpacing) <= lineWidth)\n" +
            "{\n" +
            "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
            "}\n" +
            "}\n" +
            "if (luminance < 0.75)\n" +
            "{\n" +
            "if (mod(vTextureCoord.x - vTextureCoord.y, crossHatchSpacing) <= lineWidth)\n" +
            "{\n" +
            "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
            "}\n" +
            "}\n" +
            "if (luminance < 0.50)\n" +
            "{\n" +
            "if (mod(vTextureCoord.x + vTextureCoord.y - (crossHatchSpacing / 2.0), crossHatchSpacing) <= lineWidth)\n" +
            "{\n" +
            "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
            "}\n" +
            "}\n" +
            "if (luminance < 0.3)\n" +
            "{\n" +
            "if (mod(vTextureCoord.x - vTextureCoord.y - (crossHatchSpacing / 2.0), crossHatchSpacing) <= lineWidth)\n" +
            "{\n" +
            "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
            "}\n" +
            "}\n" +
            "gl_FragColor = colorToDisplay;\n" +
            "}\n";

    private float crossHatchSpacing = 0.03f;
    private int crossHatchSpacingLocation = -1;

    private float lineWidth = 0.003f;
    private int lineWidthLocation = -1;

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        crossHatchSpacingLocation = GLES20.glGetUniformLocation(programHandle, "crossHatchSpacing");
        Egloo.checkGlProgramLocation(crossHatchSpacingLocation, "crossHatchSpacing");

        lineWidthLocation = GLES20.glGetUniformLocation(programHandle, "lineWidth");
        Egloo.checkGlProgramLocation(lineWidthLocation, "lineWidth");
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(crossHatchSpacingLocation, crossHatchSpacing);
        GLES20.glUniform1f(lineWidthLocation, lineWidth);
        Egloo.checkGlError("glUniform1f");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        crossHatchSpacing = 0.03f;
        crossHatchSpacingLocation = -1;
        lineWidth = 0.003f;
        lineWidthLocation = -1;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return CROSSHATCH_FRAGMENT_SHADER;
    }

    @Override
    public void setParameter1(float value) {
        setCrossHatchSpacing(value);
    }

    @Override
    public float getParameter1() {
        return getCrossHatchSpacing();
    }

    @Override
    public void setParameter2(float value) {
        setLineWidth(value);
    }

    @Override
    public float getParameter2() {
        return getLineWidth();
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public float getCrossHatchSpacing() {
        return crossHatchSpacing;
    }

    public void setCrossHatchSpacing(float crossHatchSpacing) {
        this.crossHatchSpacing = crossHatchSpacing;
    }
}

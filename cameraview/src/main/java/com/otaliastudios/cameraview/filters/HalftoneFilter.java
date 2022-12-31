package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.cameraview.filter.TwoParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * exposure: The adjusted exposure (-10.0 - 10.0, with 0.0 as the default)
 */
public class HalftoneFilter extends BaseFilter implements TwoParameterFilter {
    private static final String EXPOSURE_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;" +
            "varying vec2 vTextureCoord;\n" +

            "uniform samplerExternalOES sTexture;\n" +

            "uniform highp float fractionalWidthOfPixel;\n" +
            "uniform highp float aspectRatio;\n" +

            "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
            "  highp vec2 sampleDivisor = vec2(fractionalWidthOfPixel, fractionalWidthOfPixel / aspectRatio);\n" +
            "  highp vec2 samplePos = vTextureCoord - mod(vTextureCoord, sampleDivisor) + 0.5 * sampleDivisor;\n" +
            "  highp vec2 textureCoordinateToUse = vec2(vTextureCoord.x, (vTextureCoord.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
            "  highp vec2 adjustedSamplePos = vec2(samplePos.x, (samplePos.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
            "  highp float distanceFromSamplePoint = distance(adjustedSamplePos, textureCoordinateToUse);\n" +
            "  lowp vec3 sampledColor = texture2D(sTexture, samplePos).rgb;\n" +
            "  highp float dotScaling = 1.0 - dot(sampledColor, W);\n" +
            "  lowp float checkForPresenceWithinDot = 1.0 - step(distanceFromSamplePoint, (fractionalWidthOfPixel * 0.5) * dotScaling);\n" +
            "  gl_FragColor = vec4(vec3(checkForPresenceWithinDot), 1.0);\n" +
            "}";

    private float fractionalWidthOfPixel = 0.01f;
    private int fractionalWidthOfPixelLocation = -1;
    private float aspectRatio = 1f;
    private int aspectRatioLocation = -1;

    @NonNull
    @Override
    public String getFragmentShader() {
        return EXPOSURE_FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);

        fractionalWidthOfPixelLocation = GLES20.glGetUniformLocation(programHandle, "fractionalWidthOfPixel");
        Egloo.checkGlProgramLocation(fractionalWidthOfPixelLocation, "fractionalWidthOfPixel");

        aspectRatioLocation = GLES20.glGetUniformLocation(programHandle, "aspectRatio");
        Egloo.checkGlProgramLocation(aspectRatioLocation, "aspectRatio");
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(fractionalWidthOfPixelLocation, fractionalWidthOfPixel);
        GLES20.glUniform1f(aspectRatioLocation, aspectRatio);
        Egloo.checkGlError("glUniform1f");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        fractionalWidthOfPixel = 0.01f;
        fractionalWidthOfPixelLocation = -1;
        aspectRatio = 1f;
        aspectRatioLocation = -1;
    }

    @Override
    public void setParameter1(float value) {
        setFractionalWidthOfPixel(value);
    }

    @Override
    public float getParameter1() {
        return getFractionalWidthOfPixel();
    }

    @Override
    public void setParameter2(float value) {
        setAspectRatio(value);
    }

    @Override
    public float getParameter2() {
        return getAspectRatio();
    }

    public float getFractionalWidthOfPixel() {
        return fractionalWidthOfPixel;
    }

    public void setFractionalWidthOfPixel(float fractionalWidthOfPixel) {
        this.fractionalWidthOfPixel = fractionalWidthOfPixel;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
}

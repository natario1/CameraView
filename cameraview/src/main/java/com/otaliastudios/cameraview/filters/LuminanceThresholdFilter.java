package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * exposure: The adjusted exposure (-10.0 - 10.0, with 0.0 as the default)
 */
public class LuminanceThresholdFilter extends BaseFilter implements OneParameterFilter {
    private static final String EXPOSURE_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;" +
            "varying highp vec2 vTextureCoord;\n" +
            "\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp float threshold;\n" +
            "\n" +
            "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    highp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
            "    highp float luminance = dot(textureColor.rgb, W);\n" +
            "    highp float thresholdResult = step(threshold, luminance);\n" +
            "    \n" +
            "    gl_FragColor = vec4(vec3(thresholdResult), textureColor.w);\n" +
            "}";


    private float threshold = 0.5f;
    private int thresholdLocation = -1;

    @NonNull
    @Override
    public String getFragmentShader() {
        return EXPOSURE_FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        thresholdLocation = GLES20.glGetUniformLocation(programHandle, "threshold");
        Egloo.checkGlProgramLocation(thresholdLocation, "threshold");
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(thresholdLocation, threshold);
        Egloo.checkGlError("glUniform1f");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        threshold = 0.5f;
        thresholdLocation = -1;
    }

    @Override
    public void setParameter1(float value) {
        setThreshold(value);
    }

    @Override
    public float getParameter1() {
        return getThreshold();
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
}

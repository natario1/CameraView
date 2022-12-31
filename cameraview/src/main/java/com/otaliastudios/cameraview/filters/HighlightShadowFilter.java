package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.TwoParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * between 0.0 to 1.0
 */
public class HighlightShadowFilter extends BaseFilter implements TwoParameterFilter {
    private static final String EXPOSURE_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;" +
            " uniform samplerExternalOES sTexture;\n" +
            " uniform float shadows;\n" +
            " uniform float highlights;\n" +
            " varying vec2 vTextureCoord;\n" +
            " \n" +
            " const mediump vec3 luminanceWeighting = vec3(0.3, 0.3, 0.3);\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            " 	vec4 source = texture2D(sTexture, vTextureCoord);\n" +
            " 	mediump float luminance = dot(source.rgb, luminanceWeighting);\n" +
            " \n" +
            " 	mediump float shadow = clamp((pow(luminance, 1.0/(shadows+1.0)) + (-0.76)*pow(luminance, 2.0/(shadows+1.0))) - luminance, 0.0, 1.0);\n" +
            " 	mediump float highlight = clamp((1.0 - (pow(1.0-luminance, 1.0/(2.0-highlights)) + (-0.8)*pow(1.0-luminance, 2.0/(2.0-highlights)))) - luminance, -1.0, 0.0);\n" +
            " 	lowp vec3 result = vec3(0.0, 0.0, 0.0) + ((luminance + shadow + highlight) - 0.0) * ((source.rgb - vec3(0.0, 0.0, 0.0))/(luminance - 0.0));\n" +
            " \n" +
            " 	gl_FragColor = vec4(result.rgb, source.a);\n" +
            " }";

    private float shadows = 1f;
    private int shadowsLocation = -1;

    private float highlights = 0f;
    private int highlightsLocation = -1;

    @NonNull
    @Override
    public String getFragmentShader() {
        return EXPOSURE_FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);

        shadowsLocation = GLES20.glGetUniformLocation(programHandle, "shadows");
        Egloo.checkGlProgramLocation(shadowsLocation, "shadows");

        highlightsLocation = GLES20.glGetUniformLocation(programHandle, "highlights");
        Egloo.checkGlProgramLocation(highlightsLocation, "highlights");
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(shadowsLocation, shadows);
        GLES20.glUniform1f(highlightsLocation, highlights);
        Egloo.checkGlError("glUniform1f");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        shadows = 1f;
        shadowsLocation = -1;
        highlights = 0f;
        highlightsLocation = -1;
    }

    @Override
    public void setParameter1(float value) {
        setShadows(value);
    }

    @Override
    public float getParameter1() {
        return getShadows();
    }

    @Override
    public void setParameter2(float value) {
        setHighlights(value);
    }

    @Override
    public float getParameter2() {
        return getHighlights();
    }

    public float getShadows() {
        return shadows;
    }

    public void setShadows(float shadows) {
        this.shadows = shadows;
    }

    public float getHighlights() {
        return highlights;
    }

    public void setHighlights(float highlights) {
        this.highlights = highlights;
    }
}

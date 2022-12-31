package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.cameraview.filter.TwoParameterFilter;
import com.otaliastudios.opengl.core.Egloo;

/**
 * exposure: The adjusted exposure (-1 - 0 - 1 as the default)
 */
public class ExposureFilter extends BaseFilter implements OneParameterFilter {
    private static final String EXPOSURE_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform float exposure;\n" +
            "varying vec2 "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+";\n" +
            "void main()\n" +
            "{\n" +
            "   vec4 textureColor = texture2D(sTexture, "+DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME+");\n" +
            "   gl_FragColor = vec4(textureColor.rgb * pow(2.0, exposure), textureColor.w);\n" +
            "}";

    private float exposure = 1f;
    private int exposureLocation = -1;

    @NonNull
    @Override
    public String getFragmentShader() {
        return EXPOSURE_FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        exposureLocation = GLES20.glGetUniformLocation(programHandle, "exposure");
        Egloo.checkGlProgramLocation(exposureLocation, "exposure");
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
        GLES20.glUniform1f(exposureLocation, exposure);
        Egloo.checkGlError("glUniform1f");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        exposure = 1f;
        exposureLocation = -1;
    }

    @Override
    public void setParameter1(float value) {
        setExposure(value);
    }

    @Override
    public float getParameter1() {
        return getExposure();
    }

    public float getExposure() {
        return exposure;
    }
    /**
     * exposure: The adjusted exposure (-10.0 - 10.0, with 0.0 as the default)
     */
    public void setExposure(float exposure) {
        this.exposure = exposure;
    }
}

package com.otaliastudios.cameraview.filters;

import android.content.Context;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.R;
import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.ContextParameterFilter;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.opengl.core.Egloo;

import java.nio.FloatBuffer;

public class EarlybirdFilter extends BaseFilter implements ContextParameterFilter {


    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float; \n" +
                    "uniform samplerExternalOES sTexture; \n" +
                    "varying vec2 " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + "; \n" +
                    "const vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
                    "\n" +
                    "vec3 BrightnessContrastSaturation(vec3 color, float brt, float con, float sat)\n" +
                    "{\n" +
                    "\tvec3 black = vec3(0., 0., 0.);\n" +
                    "\tvec3 middle = vec3(0.5, 0.5, 0.5);\n" +
                    "\tfloat luminance = dot(color, W);\n" +
                    "\tvec3 gray = vec3(luminance, luminance, luminance);\n" +
                    "\t\n" +
                    "\tvec3 brtColor = mix(black, color, brt);\n" +
                    "\tvec3 conColor = mix(middle, brtColor, con);\n" +
                    "\tvec3 satColor = mix(gray, conColor, sat);\n" +
                    "\treturn satColor;\n" +
                    "}\n" +
                    "\n" +
                    "vec3 ovelayBlender(vec3 Color, vec3 filter){\n" +
                    "\tvec3 filter_result;\n" +
                    "\tfloat luminance = dot(filter, W);\n" +
                    "\t\n" +
                    "\tif(luminance < 0.5)\n" +
                    "\t\tfilter_result = 2. * filter * Color;\n" +
                    "\telse\n" +
                    "\t\tfilter_result = 1. - (1. - (2. *(filter - 0.5)))*(1. - Color);\n" +
                    "\t\t\n" +
                    "\treturn filter_result;\n" +
                    "}\n" +
                    "\n" +
                    "vec3 multiplyBlender(vec3 Color, vec3 filter){\n" +
                    "\tvec3 filter_result;\n" +
                    "\tfloat luminance = dot(filter, W);\n" +
                    "\t\n" +
                    "\tif(luminance < 0.5)\n" +
                    "\t\tfilter_result = 2. * filter * Color;\n" +
                    "\telse\n" +
                    "\t\tfilter_result = Color;\n" +
                    "\t\t\t\n" +
                    "\treturn filter_result;\n" +
                    "}\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "\t //get the pixel\n" +
                    "     vec2 st = " + DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME + ".st;\n" +
                    "     vec3 irgb = texture2D(sTexture, st).rgb;\n" +
                    "     \n" +
                    "     //adjust the brightness/contrast/saturation\n" +
                    "     float T_bright = 1.2;\n" +
                    "     float T_contrast = 1.1;\n" +
                    "     float T_saturation = 1.2;\n" +
                    "     vec3 bcs_result = BrightnessContrastSaturation(irgb, T_bright, T_contrast, T_saturation);\n" +
                    "     \n" +
                    "     //more red, less blue\n" +
                    "     vec3 rb_result = vec3(bcs_result.r*1.1, bcs_result.g, bcs_result.b*0.9);\n" +
                    "     \n" +
                    "     gl_FragColor = vec4(rb_result, 1.);\n" +
                    "}";


    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
    }

    @Override
    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        super.onPreDraw(timestampUs, transformMatrix);
    }

    @Override
    public void setContext(Context value) {
    }
}

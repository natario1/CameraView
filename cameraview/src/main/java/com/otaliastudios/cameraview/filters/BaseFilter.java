package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.internal.GlUtils;
import com.otaliastudios.cameraview.size.Size;

import java.nio.FloatBuffer;

/**
 * A base implementation of {@link Filter} that just leaves the fragment shader to subclasses.
 * See {@link NoFilter} for a full implementation.
 *
 * This class offers a default vertex shader implementation which in most cases is not required
 * to be changed. Most effects can be rendered by simply changing the fragment shader, thus
 * by overriding {@link #getFragmentShader()}.
 *
 * This class expects variable to have a certain name:
 * - {@link #vertexPositionName}
 * - {@link #vertexTransformMatrixName}
 * - {@link #vertexModelViewProjectionMatrixName}
 * - {@link #vertexTextureCoordinateName}
 * - {@link #fragmentTextureCoordinateName}
 *
 * You can either change these variables, for example in your constructor, or change your
 * vertex and fragment shader code to use them.
 *
 * NOTE - All {@link BaseFilter}s should have a no-op public constructor.
 *
 * NOTE - the {@link android.graphics.SurfaceTexture} restrictions apply:
 * We only support the {@link android.opengl.GLES11Ext#GL_TEXTURE_EXTERNAL_OES} texture target
 * and it must be specified in the fragment shader as a samplerExternalOES texture.
 * You also have to explicitly require the extension: see {@link #createDefaultFragmentShader(String)}.
 */
public abstract class BaseFilter implements Filter {

    private final static String TAG = BaseFilter.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private final static String DEFAULT_VERTEX_POSITION_NAME = "aPosition";
    private final static String DEFAULT_VERTEX_TEXTURE_COORDINATE_NAME = "aTextureCoord";
    private final static String DEFAULT_VERTEX_MVP_MATRIX_NAME = "uMVPMatrix";
    private final static String DEFAULT_VERTEX_TRANSFORM_MATRIX_NAME = "uTexMatrix";
    private final static String DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME = "vTextureCoord";

    @NonNull
    private static String createDefaultVertexShader(@NonNull String vertexPositionName,
                                                    @NonNull String vertexTextureCoordinateName,
                                                    @NonNull String vertexModelViewProjectionMatrixName,
                                                    @NonNull String vertexTransformMatrixName,
                                                    @NonNull String fragmentTextureCoordinateName) {
        return "uniform mat4 "+vertexModelViewProjectionMatrixName+";\n" +
                "uniform mat4 "+vertexTransformMatrixName+";\n" +
                "attribute vec4 "+vertexPositionName+";\n" +
                "attribute vec4 "+vertexTextureCoordinateName+";\n" +
                "varying vec2 "+fragmentTextureCoordinateName+";\n" +
                "void main() {\n" +
                "    gl_Position = "+vertexModelViewProjectionMatrixName+" * "+vertexPositionName+";\n" +
                "    vTextureCoord = ("+vertexTransformMatrixName+" * "+vertexTextureCoordinateName+").xy;\n" +
                "}\n";
    }

    @NonNull
    private static String createDefaultFragmentShader(@NonNull String fragmentTextureCoordinateName) {
        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "varying vec2 "+fragmentTextureCoordinateName+";\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "void main() {\n"
                + "  gl_FragColor = texture2D(sTexture, "+fragmentTextureCoordinateName+");\n"
                + "}\n";
    }

    // When the model/view/projection matrix is identity, this will exactly cover the viewport.
    private FloatBuffer vertexPosition = GlUtils.floatBuffer(new float[]{
            -1.0f, -1.0f, // 0 bottom left
            1.0f, -1.0f, // 1 bottom right
            -1.0f, 1.0f, // 2 top left
            1.0f, 1.0f, // 3 top right
    });

    private FloatBuffer textureCoordinates = GlUtils.floatBuffer(new float[]{
            0.0f, 0.0f, // 0 bottom left
            1.0f, 0.0f, // 1 bottom right
            0.0f, 1.0f, // 2 top left
            1.0f, 1.0f  // 3 top right
    });

    private int vertexModelViewProjectionMatrixLocation = -1;
    private int vertexTranformMatrixLocation = -1;
    private int vertexPositionLocation = -1;
    private int vertexTextureCoordinateLocation = -1;
    private Size outputSize;

    @SuppressWarnings("WeakerAccess")
    protected String vertexPositionName = DEFAULT_VERTEX_POSITION_NAME;
    @SuppressWarnings("WeakerAccess")
    protected String vertexTextureCoordinateName = DEFAULT_VERTEX_TEXTURE_COORDINATE_NAME;
    @SuppressWarnings("WeakerAccess")
    protected String vertexModelViewProjectionMatrixName = DEFAULT_VERTEX_MVP_MATRIX_NAME;
    @SuppressWarnings("WeakerAccess")
    protected String vertexTransformMatrixName = DEFAULT_VERTEX_TRANSFORM_MATRIX_NAME;
    @SuppressWarnings({"unused", "WeakerAccess"})
    protected String fragmentTextureCoordinateName = DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected String createDefaultVertexShader() {
        return createDefaultVertexShader(vertexPositionName,
                vertexTextureCoordinateName,
                vertexModelViewProjectionMatrixName,
                vertexTransformMatrixName,
                fragmentTextureCoordinateName);
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected String createDefaultFragmentShader() {
        return createDefaultFragmentShader(fragmentTextureCoordinateName);
    }

    @Override
    public void onCreate(int programHandle) {
        vertexPositionLocation = GLES20.glGetAttribLocation(programHandle, vertexPositionName);
        GlUtils.checkLocation(vertexPositionLocation, vertexPositionName);
        vertexTextureCoordinateLocation = GLES20.glGetAttribLocation(programHandle, vertexTextureCoordinateName);
        GlUtils.checkLocation(vertexTextureCoordinateLocation, vertexTextureCoordinateName);
        vertexModelViewProjectionMatrixLocation = GLES20.glGetUniformLocation(programHandle, vertexModelViewProjectionMatrixName);
        GlUtils.checkLocation(vertexModelViewProjectionMatrixLocation, vertexModelViewProjectionMatrixName);
        vertexTranformMatrixLocation = GLES20.glGetUniformLocation(programHandle, vertexTransformMatrixName);
        GlUtils.checkLocation(vertexTranformMatrixLocation, vertexTransformMatrixName);
    }

    @Override
    public void onDestroy(int programHandle) {
        vertexPositionLocation = -1;
        vertexTextureCoordinateLocation = -1;
        vertexModelViewProjectionMatrixLocation = -1;
        vertexTranformMatrixLocation = -1;
    }

    @NonNull
    @Override
    public String getVertexShader() {
        return createDefaultVertexShader();
    }

    @Override
    public void setOutputSize(int width, int height) {
        outputSize = new Size(width, height);
    }

    @Override
    public void draw(float[] transformMatrix) {
        onPreDraw(transformMatrix);
        onDraw();
        onPostDraw();
    }

    @SuppressWarnings("WeakerAccess")
    protected void onPreDraw(float[] transformMatrix) {
        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(vertexModelViewProjectionMatrixLocation, 1, false, GlUtils.IDENTITY_MATRIX, 0);
        GlUtils.checkError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(vertexTranformMatrixLocation, 1, false, transformMatrix, 0);
        GlUtils.checkError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        // Connect vertexBuffer to "aPosition".
        GLES20.glEnableVertexAttribArray(vertexPositionLocation);
        GlUtils.checkError("glEnableVertexAttribArray: " + vertexPositionLocation);
        GLES20.glVertexAttribPointer(vertexPositionLocation, 2, GLES20.GL_FLOAT, false, 8, vertexPosition);
        GlUtils.checkError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        // Connect texBuffer to "aTextureCoord".
        GLES20.glEnableVertexAttribArray(vertexTextureCoordinateLocation);
        GlUtils.checkError("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(vertexTextureCoordinateLocation, 2, GLES20.GL_FLOAT, false, 8, textureCoordinates);
        GlUtils.checkError("glVertexAttribPointer");
    }

    @SuppressWarnings("WeakerAccess")
    protected void onDraw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GlUtils.checkError("glDrawArrays");
    }

    @SuppressWarnings("WeakerAccess")
    protected void onPostDraw() {
        GLES20.glDisableVertexAttribArray(vertexPositionLocation);
        GLES20.glDisableVertexAttribArray(vertexTextureCoordinateLocation);
    }

    @Override
    public BaseFilter copy() {
        BaseFilter copy = onCopy();
        copy.setOutputSize(outputSize.getWidth(), outputSize.getHeight());
        return copy;
    }

    @SuppressWarnings("WeakerAccess")
    protected BaseFilter onCopy() {
        try {
            return getClass().newInstance();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Filters should have a public no-op constructor.", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Filters should have a public no-op constructor.", e);
        }
    }
}

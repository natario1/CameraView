package com.otaliastudios.cameraview.filter;

import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.internal.GlUtils;
import com.otaliastudios.cameraview.size.Size;

import java.nio.FloatBuffer;

/**
 * A base implementation of {@link Filter} that just leaves the fragment shader to subclasses.
 * See {@link NoFilter} for a non-abstract implementation.
 *
 * This class offers a default vertex shader implementation which in most cases is not required
 * to be changed. Most effects can be rendered by simply changing the fragment shader, thus
 * by overriding {@link #getFragmentShader()}.
 *
 * All {@link BaseFilter}s should have a no-arguments public constructor.
 * This class will try to automatically implement {@link #copy()} thanks to this.
 * If your filter implements public parameters, please implement {@link OneParameterFilter}
 * and {@link TwoParameterFilter} to handle them and have them passed automatically to copies.
 *
 * NOTE - This class expects variable to have a certain name:
 * - {@link #vertexPositionName}
 * - {@link #vertexTransformMatrixName}
 * - {@link #vertexModelViewProjectionMatrixName}
 * - {@link #vertexTextureCoordinateName}
 * - {@link #fragmentTextureCoordinateName}
 * You can either change these variables, for example in your constructor, or change your
 * vertex and fragment shader code to use them.
 *
 * NOTE - the {@link android.graphics.SurfaceTexture} restrictions apply:
 * We only support the {@link android.opengl.GLES11Ext#GL_TEXTURE_EXTERNAL_OES} texture target
 * and it must be specified in the fragment shader as a samplerExternalOES texture.
 * You also have to explicitly require the extension: see
 * {@link #createDefaultFragmentShader(String)}.
 *
 */
public abstract class BaseFilter implements Filter {

    private final static String TAG = BaseFilter.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressWarnings("WeakerAccess")
    protected final static String DEFAULT_VERTEX_POSITION_NAME = "aPosition";

    @SuppressWarnings("WeakerAccess")
    protected final static String DEFAULT_VERTEX_TEXTURE_COORDINATE_NAME = "aTextureCoord";

    @SuppressWarnings("WeakerAccess")
    protected final static String DEFAULT_VERTEX_MVP_MATRIX_NAME = "uMVPMatrix";

    @SuppressWarnings("WeakerAccess")
    protected final static String DEFAULT_VERTEX_TRANSFORM_MATRIX_NAME = "uTexMatrix";
    protected final static String DEFAULT_FRAGMENT_TEXTURE_COORDINATE_NAME = "vTextureCoord";

    @NonNull
    private static String createDefaultVertexShader(
            @NonNull String vertexPositionName,
            @NonNull String vertexTextureCoordinateName,
            @NonNull String vertexModelViewProjectionMatrixName,
            @NonNull String vertexTransformMatrixName,
            @NonNull String fragmentTextureCoordinateName) {
        return "uniform mat4 "+vertexModelViewProjectionMatrixName+";\n"
                + "uniform mat4 "+vertexTransformMatrixName+";\n"
                + "attribute vec4 "+vertexPositionName+";\n"
                + "attribute vec4 "+vertexTextureCoordinateName+";\n"
                + "varying vec2 "+fragmentTextureCoordinateName+";\n"
                + "void main() {\n"
                + "    gl_Position = " +vertexModelViewProjectionMatrixName+" * "
                + vertexPositionName+";\n"
                + "    "+fragmentTextureCoordinateName+" = ("+vertexTransformMatrixName+" * "
                + vertexTextureCoordinateName+").xy;\n"
                + "}\n";
    }

    @NonNull
    private static String createDefaultFragmentShader(
            @NonNull String fragmentTextureCoordinateName) {
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
    private int vertexTransformMatrixLocation = -1;
    private int vertexPositionLocation = -1;
    private int vertexTextureCoordinateLocation = -1;
    @VisibleForTesting int programHandle = -1;
    @VisibleForTesting Size size;

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
        this.programHandle = programHandle;
        vertexPositionLocation = GLES20.glGetAttribLocation(programHandle, vertexPositionName);
        GlUtils.checkLocation(vertexPositionLocation, vertexPositionName);
        vertexTextureCoordinateLocation = GLES20.glGetAttribLocation(programHandle,
                vertexTextureCoordinateName);
        GlUtils.checkLocation(vertexTextureCoordinateLocation, vertexTextureCoordinateName);
        vertexModelViewProjectionMatrixLocation = GLES20.glGetUniformLocation(programHandle,
                vertexModelViewProjectionMatrixName);
        GlUtils.checkLocation(vertexModelViewProjectionMatrixLocation,
                vertexModelViewProjectionMatrixName);
        vertexTransformMatrixLocation = GLES20.glGetUniformLocation(programHandle,
                vertexTransformMatrixName);
        GlUtils.checkLocation(vertexTransformMatrixLocation, vertexTransformMatrixName);
    }

    @Override
    public void onDestroy() {
        programHandle = -1;
        vertexPositionLocation = -1;
        vertexTextureCoordinateLocation = -1;
        vertexModelViewProjectionMatrixLocation = -1;
        vertexTransformMatrixLocation = -1;
    }

    @NonNull
    @Override
    public String getVertexShader() {
        return createDefaultVertexShader();
    }

    @Override
    public void setSize(int width, int height) {
        size = new Size(width, height);
    }

    @Override
    public void draw(long timestampUs, float[] transformMatrix) {
        if (programHandle == -1) {
            LOG.w("Filter.draw() called after destroying the filter. " +
                    "This can happen rarely because of threading.");
        } else {
            onPreDraw(timestampUs, transformMatrix);
            onDraw(timestampUs);
            onPostDraw(timestampUs);
        }
    }

    protected void onPreDraw(long timestampUs, float[] transformMatrix) {
        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(vertexModelViewProjectionMatrixLocation, 1,
                false, GlUtils.IDENTITY_MATRIX, 0);
        GlUtils.checkError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(vertexTransformMatrixLocation, 1,
                false, transformMatrix, 0);
        GlUtils.checkError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        // Connect vertexBuffer to "aPosition".
        GLES20.glEnableVertexAttribArray(vertexPositionLocation);
        GlUtils.checkError("glEnableVertexAttribArray: " + vertexPositionLocation);
        GLES20.glVertexAttribPointer(vertexPositionLocation, 2, GLES20.GL_FLOAT,
                false, 8, vertexPosition);
        GlUtils.checkError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        // Connect texBuffer to "aTextureCoord".
        GLES20.glEnableVertexAttribArray(vertexTextureCoordinateLocation);
        GlUtils.checkError("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(vertexTextureCoordinateLocation, 2, GLES20.GL_FLOAT,
                false, 8, textureCoordinates);
        GlUtils.checkError("glVertexAttribPointer");
    }

    @SuppressWarnings("WeakerAccess")
    protected void onDraw(long timestampUs) {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GlUtils.checkError("glDrawArrays");
    }

    @SuppressWarnings("WeakerAccess")
    protected void onPostDraw(long timestampUs) {
        GLES20.glDisableVertexAttribArray(vertexPositionLocation);
        GLES20.glDisableVertexAttribArray(vertexTextureCoordinateLocation);
    }

    @NonNull
    @Override
    public final BaseFilter copy() {
        BaseFilter copy = onCopy();
        if (size != null) {
            copy.setSize(size.getWidth(), size.getHeight());
        }
        if (this instanceof OneParameterFilter) {
            ((OneParameterFilter) copy).setParameter1(((OneParameterFilter) this).getParameter1());
        }
        if (this instanceof TwoParameterFilter) {
            ((TwoParameterFilter) copy).setParameter2(((TwoParameterFilter) this).getParameter2());
        }
        return copy;
    }

    protected BaseFilter onCopy() {
        try {
            return getClass().newInstance();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Filters should have a public no-arguments constructor.", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Filters should have a public no-arguments constructor.", e);
        }
    }
}

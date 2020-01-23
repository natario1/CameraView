package com.otaliastudios.cameraview.filter;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.opengl.draw.GlDrawable;
import com.otaliastudios.opengl.draw.GlRect;
import com.otaliastudios.opengl.program.GlTextureProgram;

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

    @VisibleForTesting GlTextureProgram program = null;
    private GlDrawable programDrawable = null;
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
        program = new GlTextureProgram(programHandle,
                vertexPositionName,
                vertexModelViewProjectionMatrixName,
                vertexTextureCoordinateName,
                vertexTransformMatrixName);
        programDrawable = new GlRect();
    }

    @Override
    public void onDestroy() {
        // Since we used the handle constructor of GlTextureProgram, calling release here
        // will NOT destroy the GL program. This is important because Filters are not supposed
        // to have ownership of programs. Creation and deletion happen outside, and deleting twice
        // would cause an error.
        program.release();
        program = null;
        programDrawable = null;
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
    public void draw(long timestampUs, @NonNull float[] transformMatrix) {
        if (program == null) {
            LOG.w("Filter.draw() called after destroying the filter. " +
                    "This can happen rarely because of threading.");
        } else {
            onPreDraw(timestampUs, transformMatrix);
            onDraw(timestampUs);
            onPostDraw(timestampUs);
        }
    }

    protected void onPreDraw(long timestampUs, @NonNull float[] transformMatrix) {
        program.setTextureTransform(transformMatrix);
        program.onPreDraw(programDrawable, programDrawable.getModelMatrix());
    }

    @SuppressWarnings("WeakerAccess")
    protected void onDraw(@SuppressWarnings("unused") long timestampUs) {
        program.onDraw(programDrawable);
    }

    @SuppressWarnings("WeakerAccess")
    protected void onPostDraw(@SuppressWarnings("unused") long timestampUs) {
        program.onPostDraw(programDrawable);
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

    @NonNull
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

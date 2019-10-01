package com.otaliastudios.cameraview.filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.otaliastudios.cameraview.internal.GlUtils;
import com.otaliastudios.cameraview.size.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link MultiFilter} is a special {@link Filter} that can group one or more filters together.
 * When this happens, filters are applied in sequence:
 * - the first filter reads from input frames
 * - the second filters reads the output of the first
 * And so on, until the last filter which will read from the previous and write to the real output.
 *
 * New filters can be added at any time through {@link #addFilter(Filter)}, but currently they
 * can not be removed because we can not easily ensure that they would be correctly released.
 *
 * The {@link MultiFilter} does also implement {@link OneParameterFilter} and
 * {@link TwoParameterFilter}, dispatching all the parameter calls to child filters,
 * assuming they support it.
 *
 * There are some important technical caveats when using {@link MultiFilter}:
 * - each child filter requires the allocation of a GL framebuffer. Using a large number of filters
 *   will likely cause memory issues (e.g. https://stackoverflow.com/q/6354208/4288782).
 * - some of the children need to write into {@link GLES20#GL_TEXTURE_2D} instead of
 *   {@link GLES11Ext#GL_TEXTURE_EXTERNAL_OES}! To achieve this, we replace samplerExternalOES
 *   with sampler2D in your fragment shader code. This might cause issues for some shaders.
 */
@SuppressWarnings("unused")
public class MultiFilter implements Filter, OneParameterFilter, TwoParameterFilter {

    @VisibleForTesting
    static class State {
        @VisibleForTesting boolean isCreated = false;
        @VisibleForTesting boolean isFramebufferCreated = false;
        @VisibleForTesting Size size = null;

        private int programHandle = -1;
        private int framebufferId = -1;
        private int textureId = -1;
    }

    @VisibleForTesting final List<Filter> filters = new ArrayList<>();
    @VisibleForTesting final Map<Filter, State> states = new HashMap<>();
    private final Object lock = new Object();
    private Size size = null;
    private float parameter1 = 0F;
    private float parameter2 = 0F;

    /**
     * Creates a new group with the given filters.
     * @param filters children
     */
    @SuppressWarnings("WeakerAccess")
    public MultiFilter(@NonNull Filter... filters) {
        this(Arrays.asList(filters));
    }

    /**
     * Creates a new group with the given filters.
     * @param filters children
     */
    @SuppressWarnings("WeakerAccess")
    public MultiFilter(@NonNull Collection<Filter> filters) {
        for (Filter filter : filters) {
            addFilter(filter);
        }
    }

    /**
     * Adds a new filter. It will be used in the next frame.
     * If the filter is a {@link MultiFilter}, we'll use its children instead.
     *
     * @param filter a new filter
     */
    @SuppressWarnings("WeakerAccess")
    public void addFilter(@NonNull Filter filter) {
        if (filter instanceof MultiFilter) {
            MultiFilter multiFilter = (MultiFilter) filter;
            for (Filter multiChild : multiFilter.filters) {
                addFilter(multiChild);
            }
            return;
        }
        synchronized (lock) {
            if (!filters.contains(filter)) {
                filters.add(filter);
                states.put(filter, new State());
            }
        }
    }

    // We don't offer a removeFilter method since that would cause issues
    // with cleanup. Cleanup must happen on the GL thread so we'd have to wait
    // for new rendering call (which might not even happen).

    private void maybeCreate(@NonNull Filter filter, boolean isFirst) {
        State state = states.get(filter);
        //noinspection ConstantConditions
        if (!state.isCreated) {
            state.isCreated = true;
            String shader = filter.getFragmentShader();
            if (!isFirst) {
                // The first shader actually reads from a OES texture, but the others
                // will read from the 2d framebuffer texture. This is a dirty hack.
                shader = shader.replace("samplerExternalOES ", "sampler2D ");
            }
            state.programHandle = GlUtils.createProgram(filter.getVertexShader(), shader);
            filter.onCreate(state.programHandle);
        }
    }

    private void maybeDestroy(@NonNull Filter filter) {
        State state = states.get(filter);
        //noinspection ConstantConditions
        if (state.isCreated) {
            state.isCreated = false;
            filter.onDestroy();
            GLES20.glDeleteProgram(state.programHandle);
            state.programHandle = -1;
        }
    }

    private void maybeCreateFramebuffer(@NonNull Filter filter, boolean isLast) {
        if (isLast) return;
        State state = states.get(filter);
        //noinspection ConstantConditions
        if (!state.isFramebufferCreated) {
            state.isFramebufferCreated = true;

            int[] framebufferArray = new int[1];
            int[] textureArray = new int[1];
            GLES20.glGenFramebuffers(1, framebufferArray, 0);
            GLES20.glGenTextures(1, textureArray, 0);
            state.framebufferId = framebufferArray[0];
            state.textureId = textureArray[0];

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, state.textureId);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    state.size.getWidth(), state.size.getHeight(), 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, state.framebufferId);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                    GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D,
                    state.textureId,
                    0);

            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Invalid framebuffer generation. Error:" + status);
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    private void maybeDestroyFramebuffer(@NonNull Filter filter) {
        State state = states.get(filter);
        //noinspection ConstantConditions
        if (state.isFramebufferCreated) {
            state.isFramebufferCreated = false;
            GLES20.glDeleteFramebuffers(1, new int[]{state.framebufferId}, 0);
            state.framebufferId = -1;
            GLES20.glDeleteTextures(1, new int[]{state.textureId}, 0);
            state.textureId = -1;
        }
    }

    private void maybeSetSize(@NonNull Filter filter) {
        State state = states.get(filter);
        //noinspection ConstantConditions
        if (size != null && !size.equals(state.size)) {
            state.size = size;
            filter.setSize(size.getWidth(), size.getHeight());
        }
    }

    @NonNull
    @Override
    public String getVertexShader() {
        // Whatever, we won't be using this.
        return new NoFilter().getVertexShader();
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        // Whatever, we won't be using this.
        return new NoFilter().getFragmentShader();
    }

    @Override
    public void onCreate(int programHandle) {
        // We'll create children during the draw() op, since some of them
        // might have been added after this onCreate() is called.
    }

    @Override
    public void onDestroy() {
        synchronized (lock) {
            for (Filter filter : filters) {
                maybeDestroyFramebuffer(filter);
                maybeDestroy(filter);
            }
        }
    }

    @Override
    public void setSize(int width, int height) {
        size = new Size(width, height);
        synchronized (lock) {
            for (Filter filter : filters) {
                maybeSetSize(filter);
            }
        }
    }

    @Override
    public void draw(long timestampUs, float[] transformMatrix) {
        synchronized (lock) {
            for (int i = 0; i < filters.size(); i++) {
                boolean isFirst = i == 0;
                boolean isLast = i == filters.size() - 1;
                Filter filter = filters.get(i);
                State state = states.get(filter);
                maybeSetSize(filter);
                maybeCreate(filter, isFirst);
                maybeCreateFramebuffer(filter, isLast);

                //noinspection ConstantConditions
                GLES20.glUseProgram(state.programHandle);

                // Define the output framebuffer.
                // Each filter outputs into its own framebuffer object, except the
                // last filter, which outputs into the default framebuffer.
                if (!isLast) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, state.framebufferId);
                    GLES20.glClearColor(0, 0, 0, 0);
                } else {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                }

                // Perform the actual drawing.
                // The first filter should apply all the transformations. Then,
                // since they are applied, we should use a no-op matrix.
                if (isFirst) {
                    filter.draw(timestampUs, transformMatrix);
                } else {
                    filter.draw(timestampUs, GlUtils.IDENTITY_MATRIX);
                }

                // Set the input for the next cycle:
                // It is the framebuffer texture from this cycle. If this is the last
                // filter, reset this value just to cleanup.
                if (!isLast) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, state.textureId);
                } else {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                }

                GLES20.glUseProgram(0);
            }
        }
    }

    @NonNull
    @Override
    public Filter copy() {
        synchronized (lock) {
            MultiFilter copy = new MultiFilter();
            for (Filter filter : filters) {
                copy.addFilter(filter.copy());
            }
            return copy;
        }
    }

    @Override
    public void setParameter1(float parameter1) {
        this.parameter1 = parameter1;
        synchronized (lock) {
            for (Filter filter : filters) {
                if (filter instanceof OneParameterFilter) {
                    ((OneParameterFilter) filter).setParameter1(parameter1);
                }
            }
        }
    }

    @Override
    public void setParameter2(float parameter2) {
        this.parameter2 = parameter2;
        synchronized (lock) {
            for (Filter filter : filters) {
                if (filter instanceof TwoParameterFilter) {
                    ((TwoParameterFilter) filter).setParameter2(parameter2);
                }
            }
        }
    }

    @Override
    public float getParameter1() {
        return parameter1;
    }

    @Override
    public float getParameter2() {
        return parameter2;
    }
}

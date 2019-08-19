package com.otaliastudios.cameraview.filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.internal.GlUtils;
import com.otaliastudios.cameraview.size.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class MultiFilter implements Filter {

    private final static int TARGET = GLES20.GL_TEXTURE_2D;

    private static class State {
        boolean isCreated = false;
        int programHandle = -1;
        int framebufferId = -1;
        int textureId = -1;
        Size size = null;
    }

    private final List<Filter> filters = new ArrayList<>();
    private final Map<Filter, State> states = new HashMap<>();
    private final Object lock = new Object();
    private Size size = null;

    @SuppressWarnings("WeakerAccess")
    public MultiFilter(@NonNull Filter... filters) {
        this(Arrays.asList(filters));
    }

    @SuppressWarnings("WeakerAccess")
    public MultiFilter(@NonNull Collection<Filter> filters) {
        for (Filter filter : filters) {
            addFilter(filter);
        }
    }

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
        if (state.framebufferId == -1) {
            int[] framebufferArray = new int[1];
            int[] textureArray = new int[1];
            GLES20.glGenFramebuffers(1, framebufferArray, 0);
            GLES20.glGenTextures(1, textureArray, 0);
            state.framebufferId = framebufferArray[0];
            state.textureId = textureArray[0];

            GLES20.glBindTexture(TARGET, state.textureId);
            GLES20.glTexImage2D(TARGET, 0, GLES20.GL_RGBA, state.size.getWidth(), state.size.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(TARGET, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(TARGET, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(TARGET, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(TARGET, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, state.framebufferId);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                    GLES20.GL_COLOR_ATTACHMENT0,
                    TARGET,
                    state.textureId,
                    0);

            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Invalid framebuffer generation. Error:" + status);
            }

            GLES20.glBindTexture(TARGET, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    private void maybeDestroyFramebuffer(@NonNull Filter filter) {
        State state = states.get(filter);
        //noinspection ConstantConditions
        if (state.framebufferId != -1) {
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
        return new NoFilter().getVertexShader();
    }

    @NonNull
    @Override
    public String getFragmentShader() {
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
    public void draw(float[] transformMatrix) {
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
                filter.draw(transformMatrix);

                // Set the input for the next cycle:
                // It is the framebuffer texture from this cycle. If this is the last
                // filter, reset this value just to cleanup.
                if (!isLast) {
                    GLES20.glBindTexture(TARGET, state.textureId);
                } else {
                    GLES20.glBindTexture(TARGET, 0);
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
}

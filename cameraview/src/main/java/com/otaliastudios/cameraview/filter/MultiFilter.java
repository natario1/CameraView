package com.otaliastudios.cameraview.filter;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.internal.GlUtils;
import com.otaliastudios.cameraview.size.Size;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MultiFilter implements Filter {

    private static class State {
        boolean isCreated = false;
        int programHandle = -1;
        Size size = null;
    }

    private final Map<Filter, State> filters = new HashMap<>();
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
            for (Filter multiChild : multiFilter.filters.keySet()) {
                addFilter(multiChild);
            }
            return;
        }
        synchronized (lock) {
            if (!filters.keySet().contains(filter)) {
                filters.put(filter, new State());
            }
        }
    }

    // We don't offer a removeFilter method since that would cause issues
    // with cleanup. Cleanup must happen on the GL thread so we'd have to wait
    // for new rendering call (which might not even happen).

    private void maybeCreate(@NonNull Filter filter) {
        State state = filters.get(filter);
        //noinspection ConstantConditions
        if (!state.isCreated) {
            state.isCreated = true;
            state.programHandle = GlUtils.createProgram(filter.getVertexShader(), filter.getFragmentShader());
            filter.onCreate(state.programHandle);
            // TODO extra creation
        }
    }

    private void maybeDestroy(@NonNull Filter filter) {
        State state = filters.get(filter);
        //noinspection ConstantConditions
        if (state.isCreated) {
            state.isCreated = false;
            filter.onDestroy();
            GLES20.glDeleteProgram(state.programHandle);
            state.programHandle = -1;
            // TODO extra cleanup.
        }
    }

    private void maybeSetSize(@NonNull Filter filter) {
        State state = filters.get(filter);
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
            for (Filter filter : filters.keySet()) {
                maybeDestroy(filter);
            }
        }
    }

    @Override
    public void setSize(int width, int height) {
        size = new Size(width, height);
        synchronized (lock) {
            for (Filter filter : filters.keySet()) {
                maybeSetSize(filter);
            }
        }
    }

    @Override
    public void draw(float[] transformMatrix) {
        synchronized (lock) {
            for (Filter filter : filters.keySet()) {
                maybeCreate(filter);
                maybeSetSize(filter);
            }
        }
        // TODO
    }

    @NonNull
    @Override
    public Filter copy() {
        synchronized (lock) {
            MultiFilter copy = new MultiFilter();
            for (Filter filter : filters.keySet()) {
                copy.addFilter(filter.copy());
            }
            return copy;
        }
    }
}

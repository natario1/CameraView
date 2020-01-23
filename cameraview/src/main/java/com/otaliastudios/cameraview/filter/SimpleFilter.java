package com.otaliastudios.cameraview.filter;

import androidx.annotation.NonNull;

/**
 * The simplest possible filter that accepts a fragment shader in its constructor.
 * This can be used when your fragment shader is static and has no 'runtime' parameters
 * that influence its behavior.
 *
 * The given fragment shader should respect the default variable names, as listed
 * in the {@link BaseFilter} class.
 *
 * NOTE: SimpleFilter is not meant to be subclassed!
 * Subclassing it would require you to override {@link #onCopy()}, which would make
 * this class useless. Instead, you can extend {@link BaseFilter} directly.
 */
public final class SimpleFilter extends BaseFilter {

    private final String fragmentShader;

    /**
     * Creates a new filter with the given fragment shader.
     * @param fragmentShader a fragment shader
     */
    @SuppressWarnings("WeakerAccess")
    public SimpleFilter(@NonNull String fragmentShader) {
        this.fragmentShader = fragmentShader;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return fragmentShader;
    }

    @NonNull
    @Override
    protected BaseFilter onCopy() {
        return new SimpleFilter(fragmentShader);
    }
}

package com.otaliastudios.cameraview.filter;

import com.otaliastudios.cameraview.CameraView;

import androidx.annotation.NonNull;

import java.io.File;


/**
 * A Filter is a real time filter that operates onto the camera preview, plus any
 * snapshot media taken with {@link CameraView#takePictureSnapshot()} and
 * {@link CameraView#takeVideoSnapshot(File)}.
 *
 * You can apply filters to the camera engine using {@link CameraView#setFilter(Filter)}.
 * The default filter is called {@link NoFilter} and can be used to restore the normal preview.
 * A lof of other filters are collected in the {@link Filters} class.
 *
 * Advanced users can create custom filters using GLES.
 * It is recommended to extend {@link BaseFilter} instead of this class.
 *
 * All {@link Filter}s should have a no-arguments public constructor.
 * This ensures that you can pass the filter class to XML attribute {@code app:cameraFilter},
 * and also helps {@link BaseFilter} automatically make a copy of the filter.
 *
 * Parameterized filters can implement {@link OneParameterFilter} and {@link TwoParameterFilter}
 * to receive parameters in the 0F-1F range. This helps in making filter copies and also let us
 * map the filter parameter to gestures.
 */
public interface Filter {

    /**
     * Returns a String containing the vertex shader.
     * Together with {@link #getFragmentShader()}, this will be used to
     * create the OpenGL program.
     *
     * @return vertex shader
     */
    @NonNull
    String getVertexShader();

    /**
     * Returns a String containing the fragment shader.
     * Together with {@link #getVertexShader()}, this will be used to
     * create the OpenGL program.
     *
     * @return fragment shader
     */
    @NonNull
    String getFragmentShader();

    /**
     * The filter program was just created. We pass in a handle to the OpenGL
     * program that was created, so you can fetch pointers.
     *
     * @param programHandle handle
     */
    void onCreate(int programHandle);

    /**
     * The filter program is about to be destroyed.
     *
     */
    void onDestroy();

    /**
     * Called to render the actual texture. The given transformation matrix
     * should be applied.
     *
     * @param timestampUs timestamp in microseconds
     * @param transformMatrix matrix
     */
    void draw(long timestampUs, @NonNull float[] transformMatrix);

    /**
     * Called anytime the output size changes.
     *
     * @param width width
     * @param height height
     */
    void setSize(int width, int height);

    /**
     * Clones this filter creating a new instance of it.
     * If it has any important parameters, these should be passed
     * to the new instance.
     *
     * @return a clone
     */
    @NonNull
    Filter copy();
}

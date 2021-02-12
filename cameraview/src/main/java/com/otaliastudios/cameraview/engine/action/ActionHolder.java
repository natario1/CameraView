package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * The holder of {@link Action}.
 *
 * This class should keep a list or set of currently running actions, and offers
 * to them the base Camera2 objects that are needed to apply changes.
 *
 * This class, or an holder of it, should also forward the capture callbacks
 * to all {@link Action}s. See {@link com.otaliastudios.cameraview.engine.Camera2Engine} for
 * our implementation.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public interface ActionHolder {

    /**
     * Adds a new action
     * @param action action
     */
    void addAction(@NonNull Action action);

    /**
     * Removes a previously added action
     * @param action action
     */
    void removeAction(@NonNull Action action);

    /**
     * Returns the {@link CameraCharacteristics} of the current
     * camera device.
     * @param action action
     * @return characteristics
     */
    @NonNull
    CameraCharacteristics getCharacteristics(@NonNull Action action);

    /**
     * Returns the latest {@link TotalCaptureResult}. Can be used
     * by actions to start querying the state before receiving their
     * first frame.
     * @param action action
     * @return last result
     */
    @Nullable
    TotalCaptureResult getLastResult(@NonNull Action action);

    /**
     * Returns the current {@link CaptureRequest.Builder} so that
     * actions can apply changes to it and later submit them.
     * @param action action
     * @return the builder
     */
    @NonNull
    CaptureRequest.Builder getBuilder(@NonNull Action action);

    /**
     * Applies the current builder (as per {@link #getBuilder(Action)})
     * as a repeating request on the preview.
     * @param source action
     */
    void applyBuilder(@NonNull Action source);

    /**
     * Applies the given builder as a single capture request.
     * Callers can catch the exception and choose what to do.
     * @param source action
     * @param builder builder
     * @throws CameraAccessException camera exception
     */
    void applyBuilder(@NonNull Action source, @NonNull CaptureRequest.Builder builder)
            throws CameraAccessException;
}

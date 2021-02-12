package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * The Action class encapsulates logic for completing an action in a Camera2 environment.
 * In this case, we are often interested in constantly receiving the {@link CaptureResult}
 * and {@link CaptureRequest} callbacks, as well as applying changes to a
 * {@link CaptureRequest.Builder} and having them applied to the sensor.
 *
 * The Action class receives the given callbacks and can operate over the engine
 * through the {@link ActionHolder} object.
 *
 * Each Action operates on a given state in a given moment. This base class offers the
 * {@link #STATE_COMPLETED} state which is common to all actions.
 *
 * See {@link BaseAction} for a base implementation.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public interface Action {

    int STATE_COMPLETED = Integer.MAX_VALUE;

    /**
     * Returns the current state.
     * @return the state
     */
    int getState();

    /**
     * Starts this action.
     * @param holder the holder
     */
    void start(@NonNull ActionHolder holder);

    /**
     * Aborts this action.
     * @param holder the holder
     */
    void abort(@NonNull ActionHolder holder);

    /**
     * Adds an {@link ActionCallback} to receive state
     * change events.
     * @param callback a callback
     */
    void addCallback(@NonNull ActionCallback callback);

    /**
     * Removes a previously added callback.
     * @param callback a callback
     */
    void removeCallback(@NonNull ActionCallback callback);

    /**
     * Called from {@link CaptureCallback#onCaptureStarted(CameraCaptureSession, CaptureRequest,
     * long, long)}.
     * @param holder the holder
     * @param request the request
     */
    void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request);

    /**
     * Called from {@link CaptureCallback#onCaptureProgressed(CameraCaptureSession, CaptureRequest,
     * CaptureResult)}.
     * @param holder the holder
     * @param request the request
     * @param result the result
     */
    void onCaptureProgressed(@NonNull ActionHolder holder,
                             @NonNull CaptureRequest request,
                             @NonNull CaptureResult result);

    /**
     * Called from {@link CaptureCallback#onCaptureCompleted(CameraCaptureSession, CaptureRequest,
     * TotalCaptureResult)}.
     * @param holder the holder
     * @param request the request
     * @param result the result
     */
    void onCaptureCompleted(@NonNull ActionHolder holder,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result);
}

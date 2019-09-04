package com.otaliastudios.cameraview.engine.action;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Arrays;

/**
 * Utilities for creating {@link Action} sequences.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Actions {

    /**
     * Creates a {@link BaseAction} that executes all the child actions
     * together, at the same time, and completes once all of them are
     * completed.
     *
     * @param actions input actions
     * @return a new action
     */
    @NonNull
    public static BaseAction together(@NonNull BaseAction... actions) {
        return new TogetherAction(Arrays.asList(actions));
    }

    /**
     * Creates a {@link BaseAction} that executes all the child actions
     * in sequence, waiting for the first to complete, then going on with
     * the second and so on, finally completing when all are completed.
     *
     * @param actions input actions
     * @return a new action
     */
    @NonNull
    public static BaseAction sequence(@NonNull BaseAction... actions) {
        return new SequenceAction(Arrays.asList(actions));
    }

    /**
     * Creates a {@link BaseAction} that completes as normal, but is also
     * forced to complete if the given timeout is reached, by calling
     * {@link Action#abort(ActionHolder)}.
     *
     * @param timeoutMillis timeout in milliseconds
     * @param action action
     * @return a new action
     */
    @NonNull
    public static BaseAction timeout(long timeoutMillis, @NonNull BaseAction action) {
        return new TimeoutAction(timeoutMillis, action);
    }

}

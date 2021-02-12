package com.otaliastudios.cameraview.engine.action;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * A callback for {@link Action} state changes.
 * See the action class.
 *
 * See also {@link CompletionCallback}.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public interface ActionCallback {

    /**
     * Action state has just changed.
     * @param action action
     * @param state new state
     */
    void onActionStateChanged(@NonNull Action action, int state);
}

package com.otaliastudios.cameraview.engine.action;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class CompletionCallback implements ActionCallback {

    @Override
    public void onActionStateChanged(@NonNull Action action, int state) {
        if (state == Action.STATE_COMPLETED) {
            onActionCompleted(action);
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected abstract void onActionCompleted(@NonNull Action action);
}

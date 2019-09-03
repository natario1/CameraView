package com.otaliastudios.cameraview.engine.action;

import androidx.annotation.NonNull;

public interface ActionCallback {
    void onActionStateChanged(@NonNull Action action, int state);
}

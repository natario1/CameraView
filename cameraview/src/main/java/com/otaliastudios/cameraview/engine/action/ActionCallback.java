package com.otaliastudios.cameraview.engine.action;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public interface ActionCallback {
    void onActionStateChanged(@NonNull Action action, int state);
}

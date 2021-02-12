package com.otaliastudios.cameraview.engine.lock;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class BaseLock extends BaseAction {

    @Override
    protected final void onStart(@NonNull ActionHolder holder) {
        super.onStart(holder);
        boolean isSkipped = checkShouldSkip(holder);
        boolean isSupported = checkIsSupported(holder);
        if (isSupported && !isSkipped) {
            onStarted(holder);
        } else {
            setState(STATE_COMPLETED);
        }
    }

    protected abstract void onStarted(@NonNull ActionHolder holder);

    protected abstract boolean checkShouldSkip(@NonNull ActionHolder holder);

    protected abstract boolean checkIsSupported(@NonNull ActionHolder holder);
}

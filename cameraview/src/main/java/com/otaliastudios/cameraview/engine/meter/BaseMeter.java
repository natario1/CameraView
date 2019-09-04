package com.otaliastudios.cameraview.engine.meter;

import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class BaseMeter extends BaseAction {

    private final List<MeteringRectangle> areas;
    private boolean isSuccessful;
    private boolean skipIfPossible;

    @SuppressWarnings("WeakerAccess")
    protected BaseMeter(@NonNull List<MeteringRectangle> areas, boolean skipIfPossible) {
        this.areas = areas;
        this.skipIfPossible = skipIfPossible;
    }

    @Override
    protected final void onStart(@NonNull ActionHolder holder) {
        super.onStart(holder);
        boolean isSkipped = skipIfPossible && checkShouldSkip(holder);
        boolean isSupported = checkIsSupported(holder);
        if (isSupported && !isSkipped) {
            onStarted(holder, areas);
        } else {
            setSuccessful(true);
            setState(STATE_COMPLETED);
        }
    }

    protected abstract void onStarted(@NonNull ActionHolder holder,
                                      @NonNull List<MeteringRectangle> areas);

    protected abstract boolean checkShouldSkip(@NonNull ActionHolder holder);

    protected abstract boolean checkIsSupported(@NonNull ActionHolder holder);

    @SuppressWarnings("WeakerAccess")
    protected void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }
}

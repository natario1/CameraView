package com.otaliastudios.cameraview.engine.meter;

import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class BaseMeter extends BaseAction {

    private final static String TAG = BaseMeter.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

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
            LOG.i("onStart:", "supported and not skipped. Dispatching onStarted.");
            onStarted(holder, areas);
        } else {
            LOG.i("onStart:", "not supported or skipped. Dispatching COMPLETED state.");
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

    @SuppressWarnings("WeakerAccess")
    public boolean isSuccessful() {
        return isSuccessful;
    }
}

package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * An {@link Action} that wraps another, and forces the completion
 * after the given timeout in milliseconds is reached.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class TimeoutAction extends ActionWrapper {

    private long startMillis;
    private long timeoutMillis;
    private BaseAction action;

    TimeoutAction(long timeoutMillis, @NonNull BaseAction action) {
        this.timeoutMillis = timeoutMillis;
        this.action = action;
    }

    @NonNull
    @Override
    public BaseAction getAction() {
        return action;
    }

    @Override
    protected void onStart(@NonNull ActionHolder holder) {
        startMillis = System.currentTimeMillis();
        super.onStart(holder);
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
        if (!isCompleted()) {
            if (System.currentTimeMillis() > startMillis + timeoutMillis) {
                // This will set our state to COMPLETED and stop requests.
                getAction().abort(holder);
            }
        }
    }
}

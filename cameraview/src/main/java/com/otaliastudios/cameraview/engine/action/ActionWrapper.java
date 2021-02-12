package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * A simple wrapper around a {@link BaseAction}.
 * This can be used to add functionality around a base action.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class ActionWrapper extends BaseAction {

    /**
     * Should return the wrapped action.
     * @return the wrapped action
     */
    @NonNull
    public abstract BaseAction getAction();

    @Override
    protected void onStart(@NonNull ActionHolder holder) {
        super.onStart(holder);
        getAction().addCallback(new ActionCallback() {
            @Override
            public void onActionStateChanged(@NonNull Action action, int state) {
                setState(state);
                if (state == STATE_COMPLETED) {
                    action.removeCallback(this);
                }
            }
        });
        getAction().onStart(holder);
    }

    @Override
    protected void onAbort(@NonNull ActionHolder holder) {
        super.onAbort(holder);
        getAction().onAbort(holder);
    }

    @Override
    public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
        super.onCaptureStarted(holder, request);
        getAction().onCaptureStarted(holder, request);
    }

    @Override
    public void onCaptureProgressed(@NonNull ActionHolder holder,
                                    @NonNull CaptureRequest request,
                                    @NonNull CaptureResult result) {
        super.onCaptureProgressed(holder, request, result);
        getAction().onCaptureProgressed(holder, request, result);
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
        getAction().onCaptureCompleted(holder, request, result);
    }
}

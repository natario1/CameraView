package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs a list of actions together, completing
 * once all of them have completed.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class TogetherAction extends BaseAction {
    // Need to be BaseAction so we can call onStart() instead of start()
    private final List<BaseAction> actions;
    private final List<BaseAction> runningActions;

    TogetherAction(@NonNull final List<BaseAction> actions) {
        this.actions = new ArrayList<>(actions);
        this.runningActions = new ArrayList<>(actions);
        for (BaseAction action : actions) {
            action.addCallback(new ActionCallback() {
                @Override
                public void onActionStateChanged(@NonNull Action action, int state) {
                    if (state == STATE_COMPLETED) {
                        //noinspection SuspiciousMethodCalls
                        runningActions.remove(action);
                    }
                    if (runningActions.isEmpty()) {
                        setState(STATE_COMPLETED);
                    }
                }
            });
        }
    }

    @Override
    protected void onStart(@NonNull ActionHolder holder) {
        super.onStart(holder);
        for (BaseAction action : actions) {
            if (!action.isCompleted()) action.onStart(holder);
        }
    }

    @Override
    protected void onAbort(@NonNull ActionHolder holder) {
        super.onAbort(holder);
        for (BaseAction action : actions) {
            if (!action.isCompleted()) action.onAbort(holder);
        }
    }

    @Override
    public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
        super.onCaptureStarted(holder, request);
        for (BaseAction action : actions) {
            if (!action.isCompleted()) action.onCaptureStarted(holder, request);
        }
    }

    @Override
    public void onCaptureProgressed(@NonNull ActionHolder holder,
                                    @NonNull CaptureRequest request,
                                    @NonNull CaptureResult result) {
        super.onCaptureProgressed(holder, request, result);
        for (BaseAction action : actions) {
            if (!action.isCompleted()) action.onCaptureProgressed(holder, request, result);
        }
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
        for (BaseAction action : actions) {
            if (!action.isCompleted()) action.onCaptureCompleted(holder, request, result);
        }
    }
}

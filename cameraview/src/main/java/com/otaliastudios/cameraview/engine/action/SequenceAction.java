package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

/**
 * Executes a list of actions in sequence, completing once
 * the last of them has been completed.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SequenceAction extends BaseAction {
    // Need to be BaseAction so we can call onStart() instead of start()
    private final List<BaseAction> actions;
    private int runningAction = -1;

    SequenceAction(@NonNull List<BaseAction> actions) {
        this.actions = actions;
        increaseRunningAction();
    }

    private void increaseRunningAction() {
        boolean first = runningAction == -1;
        boolean last = runningAction == actions.size() - 1;
        if (last) {
            // This was the last action. We're done.
            setState(STATE_COMPLETED);
        } else {
            runningAction++;
            actions.get(runningAction).addCallback(new ActionCallback() {
                @Override
                public void onActionStateChanged(@NonNull Action action, int state) {
                    if (state == STATE_COMPLETED) {
                        action.removeCallback(this);
                        increaseRunningAction();
                    }
                }
            });
            if (!first) {
                actions.get(runningAction).onStart(getHolder());
            }
        }
    }

    @Override
    protected void onStart(@NonNull ActionHolder holder) {
        super.onStart(holder);
        if (runningAction >= 0) {
            actions.get(runningAction).onStart(holder);
        }
    }

    @Override
    protected void onAbort(@NonNull ActionHolder holder) {
        super.onAbort(holder);
        if (runningAction >= 0) {
            // Previous actions have been completed already.
            // Future actions will never start. So this is OK.
            actions.get(runningAction).onAbort(holder);
        }
    }

    @Override
    public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
        super.onCaptureStarted(holder, request);
        if (runningAction >= 0) {
            actions.get(runningAction).onCaptureStarted(holder, request);
        }
    }

    @Override
    public void onCaptureProgressed(@NonNull ActionHolder holder,
                                    @NonNull CaptureRequest request,
                                    @NonNull CaptureResult result) {
        super.onCaptureProgressed(holder, request, result);
        if (runningAction >= 0) {
            actions.get(runningAction).onCaptureProgressed(holder, request, result);
        }
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
        if (runningAction >= 0) {
            actions.get(runningAction).onCaptureCompleted(holder, request, result);
        }
    }
}

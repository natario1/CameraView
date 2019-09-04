package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilities for creating {@link Action} sequences.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Actions {

    /**
     * Creates a {@link BaseAction} that executes all the child actions
     * together, at the same time, and completes once all of them are
     * completed.
     * @param actions input actions
     * @return a new action
     */
    @NonNull
    public static BaseAction together(@NonNull BaseAction... actions) {
        return new TogetherAction(Arrays.asList(actions));
    }

    /**
     * Creates a {@link BaseAction} that executes all the child actions
     * in sequence, waiting for the first to complete, then going on with
     * the second and so on, finally completing when all are completed.
     * @param actions input actions
     * @return a new action
     */
    @NonNull
    public static BaseAction sequence(@NonNull BaseAction... actions) {
        return new SequenceAction(Arrays.asList(actions));
    }

    private static class TogetherAction extends BaseAction {
        // Need to be BaseAction so we can call onStart() instead of start()
        private final List<BaseAction> actions;
        private final List<BaseAction> runningActions;

        private TogetherAction(@NonNull final List<BaseAction> actions) {
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
        public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
            super.onCaptureStarted(holder, request);
            for (BaseAction action : actions) {
                if (!action.isCompleted()) action.onCaptureStarted(holder, request);
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull ActionHolder holder, @NonNull CaptureRequest request,
                                        @NonNull CaptureResult result) {
            super.onCaptureProgressed(holder, request, result);
            for (BaseAction action : actions) {
                if (!action.isCompleted()) action.onCaptureProgressed(holder, request, result);
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull ActionHolder holder, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(holder, request, result);
            for (BaseAction action : actions) {
                if (!action.isCompleted()) action.onCaptureCompleted(holder, request, result);
            }
        }
    }

    private static class SequenceAction extends BaseAction {
        // Need to be BaseAction so we can call onStart() instead of start()
        private final List<BaseAction> actions;
        private int runningAction = -1;

        private SequenceAction(@NonNull List<BaseAction> actions) {
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
        public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
            super.onCaptureStarted(holder, request);
            if (runningAction >= 0) {
                actions.get(runningAction).onCaptureStarted(holder, request);
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull ActionHolder holder, @NonNull CaptureRequest request,
                                        @NonNull CaptureResult result) {
            super.onCaptureProgressed(holder, request, result);
            if (runningAction >= 0) {
                actions.get(runningAction).onCaptureProgressed(holder, request, result);
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull ActionHolder holder, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(holder, request, result);
            if (runningAction >= 0) {
                actions.get(runningAction).onCaptureCompleted(holder, request, result);
            }
        }
    }
}

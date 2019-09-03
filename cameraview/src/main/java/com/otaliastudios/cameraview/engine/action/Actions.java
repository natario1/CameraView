package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class Actions {

    @NonNull
    public static Action together(@NonNull BaseAction... actions) {
        return new TogetherAction(Arrays.asList(actions));
    }

    @NonNull
    public static Action sequence(@NonNull BaseAction... actions) {
        return new SequenceAction(Arrays.asList(actions));
    }

    private static class TogetherAction extends BaseAction {
        // Need to be BaseAction so we can call onStart() instead of start()
        private final List<BaseAction> actions;

        private TogetherAction(@NonNull final List<BaseAction> actions) {
            this.actions = actions;
            for (BaseAction action : actions) {
                action.addCallback(new ActionCallback() {
                    @Override
                    public void onActionStateChanged(@NonNull Action action, int state) {
                        if (state == STATE_COMPLETED) {
                            //noinspection SuspiciousMethodCalls
                            actions.remove(action);
                        }
                        if (actions.isEmpty()) {
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
                action.onStart(holder);
            }
        }

        @Override
        public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
            super.onCaptureStarted(holder, request);
            for (BaseAction action : actions) {
                action.onCaptureStarted(holder, request);
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull ActionHolder holder, @NonNull CaptureRequest request,
                                        @NonNull CaptureResult result) {
            super.onCaptureProgressed(holder, request, result);
            for (BaseAction action : actions) {
                action.onCaptureProgressed(holder, request, result);
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull ActionHolder holder, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(holder, request, result);
            for (BaseAction action : actions) {
                action.onCaptureCompleted(holder, request, result);
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
            if (runningAction == actions.size() - 1) {
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

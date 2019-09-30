package com.otaliastudios.cameraview.engine;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.CameraLogger;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Represents one of the steps in the {@link CameraEngine} setup: for example, the engine step,
 * the bind-to-surface step, and the preview step.
 *
 * A step is something that can be setup (started) or torn down (stopped), and
 * steps can of course depend onto each other.
 *
 * The purpose of this class is to manage the step state (stopping, stopped, starting or started)
 * and, more importantly, to perform START and STOP operations in such a way that they do not
 * overlap. For example, if we're stopping, we're wait for stop to finish before starting again.
 *
 * This is an important condition for simplifying the engine code.
 * Since Camera1, the only requirement was basically to use a single thread.
 * Since Camera2, which has an asynchronous API, further care must be used.
 *
 * For this reason, we use Google's {@link Task} abstraction and only start new operations
 * once the previous one has ended.
 *
 * <strong>This class is NOT thread safe!</string>
 */
class Step {

    private static final String TAG = Step.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    interface Callback {
        @NonNull
        Executor getExecutor();
        void handleException(@NonNull Exception exception);
    }

    static final int STATE_STOPPING = -1;
    static final int STATE_STOPPED = 0;
    static final int STATE_STARTING = 1;
    static final int STATE_STARTED = 2;

    private int state = STATE_STOPPED;

    // To avoid dirty scenarios (e.g. calling stopXXX while XXX is starting),
    // and since every operation can be asynchronous, we use some tasks for each step.
    private Task<Void> task = Tasks.forResult(null);

    private final String name;
    private final Callback callback;

    Step(@NonNull String name, @NonNull Callback callback) {
        this.name = name.toUpperCase();
        this.callback = callback;
    }

    int getState() {
        return state;
    }

    @VisibleForTesting void setState(int newState) {
        state = newState;
    }

    @NonNull
    String getStateName() {
        switch (state) {
            case STATE_STOPPING: return name + "_STATE_STOPPING";
            case STATE_STOPPED: return name + "_STATE_STOPPED";
            case STATE_STARTING: return name + "_STATE_STARTING";
            case STATE_STARTED: return name + "_STATE_STARTED";
        }
        return "null";
    }

    boolean isStoppingOrStopped() {
        return state == STATE_STOPPING || state == STATE_STOPPED;
    }

    boolean isStartedOrStarting() {
        return state == STATE_STARTING || state == STATE_STARTED;
    }

    boolean isStarted() {
        return state == STATE_STARTED;
    }

    @NonNull
    Task<Void> getTask() {
        return task;
    }

    @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
    Task<Void> doStart(final boolean swallowExceptions, final @NonNull Callable<Task<Void>> op) {
        return doStart(swallowExceptions, op, null);
    }

    Task<Void> doStart(final boolean swallowExceptions,
                       final @NonNull Callable<Task<Void>> op,
                       final @Nullable Runnable onStarted) {
        LOG.i(name, "doStart", "Called. Enqueuing.");
        task = task.continueWithTask(callback.getExecutor(), new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                LOG.i(name, "doStart", "About to start. Setting state to STARTING");
                setState(STATE_STARTING);
                return op.call().addOnFailureListener(callback.getExecutor(),
                        new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        LOG.w(name, "doStart", "Failed with error", e,
                                "Setting state to STOPPED");
                        setState(STATE_STOPPED);
                        if (!swallowExceptions) callback.handleException(e);
                    }
                });
            }
        }).onSuccessTask(callback.getExecutor(), new SuccessContinuation<Void, Void>() {
            @NonNull
            @Override
            public Task<Void> then(@Nullable Void aVoid) {
                LOG.i(name, "doStart", "Succeeded! Setting state to STARTED");
                setState(STATE_STARTED);
                if (onStarted != null) onStarted.run();
                return Tasks.forResult(null);
            }
        });
        return task;
    }

    @SuppressWarnings("UnusedReturnValue")
    Task<Void> doStop(final boolean swallowExceptions, final @NonNull Callable<Task<Void>> op) {
        return doStop(swallowExceptions, op, null);
    }

    Task<Void> doStop(final boolean swallowExceptions,
                      final @NonNull Callable<Task<Void>> op,
                      final @Nullable Runnable onStopped) {
        LOG.i(name, "doStop", "Called. Enqueuing.");
        task = task.continueWithTask(callback.getExecutor(), new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                LOG.i(name, "doStop", "About to stop. Setting state to STOPPING");
                state = STATE_STOPPING;
                return op.call().addOnFailureListener(callback.getExecutor(),
                        new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        LOG.w(name, "doStop", "Failed with error", e,
                                "Setting state to STOPPED");
                        state = STATE_STOPPED;
                        if (!swallowExceptions) callback.handleException(e);
                    }
                });
            }
        }).onSuccessTask(callback.getExecutor(), new SuccessContinuation<Void, Void>() {
            @NonNull
            @Override
            public Task<Void> then(@Nullable Void aVoid) {
                LOG.i(name, "doStop", "Succeeded! Setting state to STOPPED");
                state = STATE_STOPPED;
                if (onStopped != null) onStopped.run();
                return Tasks.forResult(null);
            }
        });
        return task;
    }
}

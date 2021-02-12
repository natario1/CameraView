package com.otaliastudios.cameraview.engine.orchestrator;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * A special {@link CameraOrchestrator} with special methods that deal with the
 * {@link CameraState}.
 */
public class CameraStateOrchestrator extends CameraOrchestrator {

    private CameraState mCurrentState = CameraState.OFF;
    private CameraState mTargetState = CameraState.OFF;
    private int mStateChangeCount = 0;

    public CameraStateOrchestrator(@NonNull Callback callback) {
        super(callback);
    }

    @NonNull
    public CameraState getCurrentState() {
        return mCurrentState;
    }

    @NonNull
    public CameraState getTargetState() {
        return mTargetState;
    }

    public boolean hasPendingStateChange() {
        synchronized (mJobsLock) {
            for (Job<?> job : mJobs) {
                if ((job.name.contains(" >> ") || job.name.contains(" << "))
                        && !job.source.getTask().isComplete()) {
                    return true;
                }
            }
            return false;
        }
    }

    @NonNull
    public <T> Task<T> scheduleStateChange(@NonNull final CameraState fromState,
                                           @NonNull final CameraState toState,
                                           boolean dispatchExceptions,
                                           @NonNull final Callable<Task<T>> stateChange) {
        final int changeCount = ++mStateChangeCount;
        mTargetState = toState;

        final boolean isTearDown = !toState.isAtLeast(fromState);
        final String name = isTearDown ? fromState.name() + " << " + toState.name()
                : fromState.name() + " >> " + toState.name();
        return schedule(name, dispatchExceptions, new Callable<Task<T>>() {
            @Override
            public Task<T> call() throws Exception {
                if (getCurrentState() != fromState) {
                    LOG.w(name.toUpperCase(), "- State mismatch, aborting. current:",
                            getCurrentState(), "from:", fromState, "to:", toState);
                    return Tasks.forCanceled();
                } else {
                    Executor executor = mCallback.getJobWorker(name).getExecutor();
                    return stateChange.call().continueWithTask(executor,
                            new Continuation<T, Task<T>>() {
                        @Override
                        public Task<T> then(@NonNull Task<T> task) {
                            if (task.isSuccessful() || isTearDown) {
                                mCurrentState = toState;
                            }
                            return task;
                        }
                    });
                }
            }
        }).addOnCompleteListener(new OnCompleteListener<T>() {
            @Override
            public void onComplete(@NonNull Task<T> task) {
                if (changeCount == mStateChangeCount) {
                    mTargetState = mCurrentState;
                }
            }
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public Task<Void> scheduleStateful(@NonNull String name,
                                       @NonNull final CameraState atLeast,
                                       @NonNull final Runnable job) {
        return schedule(name, true, new Runnable() {
            @Override
            public void run() {
                if (getCurrentState().isAtLeast(atLeast)) {
                    job.run();
                }
            }
        });
    }

    public void scheduleStatefulDelayed(@NonNull String name,
                                        @NonNull final CameraState atLeast,
                                        long delay,
                                        @NonNull final Runnable job) {
        scheduleDelayed(name, true, delay, new Runnable() {
            @Override
            public void run() {
                if (getCurrentState().isAtLeast(atLeast)) {
                    job.run();
                }
            }
        });
    }
}

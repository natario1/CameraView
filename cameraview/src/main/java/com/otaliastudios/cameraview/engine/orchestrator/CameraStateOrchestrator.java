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
        synchronized (mLock) {
            for (Token token : mJobs) {
                if (token.name.contains(" > ") && !token.task.isComplete()) {
                    return true;
                }
            }
            return false;
        }
    }

    @NonNull
    public Task<Void> scheduleStateChange(@NonNull final CameraState fromState,
                                          @NonNull final CameraState toState,
                                          boolean dispatchExceptions,
                                          @NonNull final Callable<Task<Void>> stateChange) {
        final int changeCount = ++mStateChangeCount;
        mTargetState = toState;

        final boolean isTearDown = !toState.isAtLeast(fromState);
        final String changeName = fromState.name() + " > " + toState.name();
        return schedule(changeName, dispatchExceptions, new Callable<Task<Void>>() {
            @Override
            public Task<Void> call() throws Exception {
                if (getCurrentState() != fromState) {
                    LOG.w(changeName.toUpperCase(), "- State mismatch, aborting. current:",
                            getCurrentState(), "from:", fromState, "to:", toState);
                    return Tasks.forCanceled();
                } else {
                    Executor executor = mCallback.getJobWorker(changeName).getExecutor();
                    return stateChange.call().continueWithTask(executor,
                            new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(@NonNull Task<Void> task) {
                            if (task.isSuccessful() || isTearDown) {
                                mCurrentState = toState;
                            }
                            return task;
                        }
                    });
                }
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
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
        scheduleDelayed(name, delay, new Runnable() {
            @Override
            public void run() {
                if (getCurrentState().isAtLeast(atLeast)) {
                    job.run();
                }
            }
        });
    }
}

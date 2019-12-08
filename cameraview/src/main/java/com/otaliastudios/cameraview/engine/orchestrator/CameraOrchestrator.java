package com.otaliastudios.cameraview.engine.orchestrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.internal.utils.WorkerHandler;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

/**
 * Schedules {@link com.otaliastudios.cameraview.engine.CameraEngine} actions,
 * so that they always run on the same thread.
 *
 * We need to be extra careful (not as easy as posting on a Handler) because the engine
 * has different states, and some actions will modify the engine state - turn it on or
 * tear it down. Other actions might need a specific state to be executed.
 * And most importantly, some actions will finish asynchronously, so subsequent actions
 * should wait for the previous to finish, but without blocking the thread.
 */
@SuppressWarnings("WeakerAccess")
public class CameraOrchestrator {

    protected static final String TAG = CameraOrchestrator.class.getSimpleName();
    protected static final CameraLogger LOG = CameraLogger.create(TAG);

    public interface Callback {
        @NonNull
        WorkerHandler getJobWorker(@NonNull String job);
        void handleJobException(@NonNull String job, @NonNull Exception exception);
    }

    protected static class Token {
        public final String name;
        public final Task<Void> task;

        private Token(@NonNull String name, @NonNull Task<Void> task) {
            this.name = name;
            this.task = task;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof Token && ((Token) obj).name.equals(name);
        }
    }

    protected final Callback mCallback;
    protected final ArrayDeque<Token> mJobs = new ArrayDeque<>();
    protected final Object mLock = new Object();
    private final Map<String, Runnable> mDelayedJobs = new HashMap<>();

    public CameraOrchestrator(@NonNull Callback callback) {
        mCallback = callback;
        ensureToken();
    }

    @NonNull
    public Task<Void> schedule(@NonNull String name,
                               boolean dispatchExceptions,
                               @NonNull final Runnable job) {
        return schedule(name, dispatchExceptions, new Callable<Task<Void>>() {
            @Override
            public Task<Void> call() {
                job.run();
                return Tasks.forResult(null);
            }
        });
    }

    @NonNull
    public Task<Void> schedule(@NonNull final String name,
                               final boolean dispatchExceptions,
                               @NonNull final Callable<Task<Void>> job) {
        LOG.i(name.toUpperCase(), "- Scheduling.");
        final TaskCompletionSource<Void> source = new TaskCompletionSource<>();
        final WorkerHandler handler = mCallback.getJobWorker(name);
        synchronized (mLock) {
            applyCompletionListener(mJobs.getLast().task, handler,
                    new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    synchronized (mLock) {
                        mJobs.removeFirst();
                        ensureToken();
                    }
                    try {
                        LOG.i(name.toUpperCase(), "- Executing.");
                        Task<Void> inner = job.call();
                        applyCompletionListener(inner, handler, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Exception e = task.getException();
                                LOG.i(name.toUpperCase(), "- Finished.", e);
                                if (e != null) {
                                    if (dispatchExceptions) {
                                        mCallback.handleJobException(name, e);
                                    }
                                    source.trySetException(e);
                                } else if (task.isCanceled()) {
                                    source.trySetException(new CancellationException());
                                } else {
                                    source.trySetResult(null);
                                }
                            }
                        });
                    } catch (Exception e) {
                        LOG.i(name.toUpperCase(), "- Finished.", e);
                        if (dispatchExceptions) mCallback.handleJobException(name, e);
                        source.trySetException(e);
                    }
                }
            });
            mJobs.addLast(new Token(name, source.getTask()));
        }
        return source.getTask();
    }

    public void scheduleDelayed(@NonNull final String name,
                                long minDelay,
                                @NonNull final Runnable runnable) {
        Runnable wrapper = new Runnable() {
            @Override
            public void run() {
                schedule(name, true, runnable);
                synchronized (mLock) {
                    if (mDelayedJobs.containsValue(this)) {
                        mDelayedJobs.remove(name);
                    }
                }
            }
        };
        synchronized (mLock) {
            mDelayedJobs.put(name, wrapper);
            mCallback.getJobWorker(name).post(minDelay, wrapper);
        }
    }

    public void remove(@NonNull String name) {
        synchronized (mLock) {
            if (mDelayedJobs.get(name) != null) {
                //noinspection ConstantConditions
                mCallback.getJobWorker(name).remove(mDelayedJobs.get(name));
                mDelayedJobs.remove(name);
            }
            Token token = new Token(name, Tasks.<Void>forResult(null));
            //noinspection StatementWithEmptyBody
            while (mJobs.remove(token)) { /* do nothing */ }
            ensureToken();
        }
    }

    private void ensureToken() {
        synchronized (mLock) {
            if (mJobs.isEmpty()) {
                mJobs.add(new Token("BASE", Tasks.<Void>forResult(null)));
            }
        }
    }

    private static void applyCompletionListener(@NonNull final Task<Void> task,
                                                @NonNull WorkerHandler handler,
                                                @NonNull final OnCompleteListener<Void> listener) {
        if (task.isComplete()) {
            handler.run(new Runnable() {
                @Override
                public void run() {
                    listener.onComplete(task);
                }
            });
        } else {
            task.addOnCompleteListener(handler.getExecutor(), listener);
        }
    }
}

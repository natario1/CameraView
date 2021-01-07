package com.otaliastudios.cameraview.engine.orchestrator;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.internal.WorkerHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    protected static class Job<T> {
        public final String name;
        public final TaskCompletionSource<T> source = new TaskCompletionSource<>();
        public final Callable<Task<T>> scheduler;
        public final boolean dispatchExceptions;
        public final long startTime;

        private Job(@NonNull String name, @NonNull Callable<Task<T>> scheduler, boolean dispatchExceptions, long startTime) {
            this.name = name;
            this.scheduler = scheduler;
            this.dispatchExceptions = dispatchExceptions;
            this.startTime = startTime;
        }
    }

    protected final Callback mCallback;
    protected final ArrayDeque<Job<?>> mJobs = new ArrayDeque<>();
    protected boolean mJobRunning = false;
    protected final Object mJobsLock = new Object();

    public CameraOrchestrator(@NonNull Callback callback) {
        mCallback = callback;
    }

    @NonNull
    public Task<Void> schedule(@NonNull String name,
                               boolean dispatchExceptions,
                               @NonNull Runnable job) {
        return scheduleDelayed(name, dispatchExceptions, 0L, job);
    }

    @NonNull
    public Task<Void> scheduleDelayed(@NonNull String name,
                                      boolean dispatchExceptions,
                                      long minDelay,
                                      @NonNull final Runnable job) {
        return scheduleInternal(name, dispatchExceptions, minDelay, new Callable<Task<Void>>() {
            @Override
            public Task<Void> call() {
                job.run();
                return Tasks.forResult(null);
            }
        });
    }

    @NonNull
    public <T> Task<T> schedule(@NonNull String name,
                                boolean dispatchExceptions,
                                @NonNull Callable<Task<T>> scheduler) {
        return scheduleInternal(name, dispatchExceptions, 0L, scheduler);
    }

    @NonNull
    private <T> Task<T> scheduleInternal(@NonNull String name,
                                         boolean dispatchExceptions,
                                         long minDelay,
                                         @NonNull Callable<Task<T>> scheduler) {
        LOG.i(name.toUpperCase(), "- Scheduling.");
        Job<T> job = new Job<>(name, scheduler, dispatchExceptions,
                System.currentTimeMillis() + minDelay);
        synchronized (mJobsLock) {
            mJobs.addLast(job);
            sync(minDelay);
        }
        return job.source.getTask();
    }

    @GuardedBy("mJobsLock")
    private void sync(long after) {
        // Jumping on the message handler even if after = 0L should avoid StackOverflow errors.
        mCallback.getJobWorker("_sync").post(after, new Runnable() {
            @SuppressWarnings("StatementWithEmptyBody")
            @Override
            public void run() {
                Job<?> job = null;
                synchronized (mJobsLock) {
                    if (mJobRunning) {
                        // Do nothing, job will be picked in executed().
                    } else {
                        long now = System.currentTimeMillis();
                        for (Job<?> candidate : mJobs) {
                            if (candidate.startTime <= now) {
                                job = candidate;
                                break;
                            }
                        }
                        if (job != null) {
                            mJobRunning = true;
                        }
                    }
                }
                // This must be out of mJobsLock! See comments in execute().
                if (job != null) execute(job);
            }
        });
    }

    // Since we use WorkerHandler.run(), the job can end up being executed on the current thread.
    // For this reason, it's important that this method is never guarded by mJobsLock! Because
    // all threads can be waiting on that, even the UI thread e.g. through scheduleInternal.
    private <T> void execute(@NonNull final Job<T> job) {
        final WorkerHandler worker = mCallback.getJobWorker(job.name);
        worker.run(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.i(job.name.toUpperCase(), "- Executing.");
                    Task<T> task = job.scheduler.call();
                    onComplete(task, worker, new OnCompleteListener<T>() {
                        @Override
                        public void onComplete(@NonNull Task<T> task) {
                            Exception e = task.getException();
                            if (e != null) {
                                LOG.w(job.name.toUpperCase(), "- Finished with ERROR.", e);
                                if (job.dispatchExceptions) {
                                    mCallback.handleJobException(job.name, e);
                                }
                                job.source.trySetException(e);
                            } else if (task.isCanceled()) {
                                LOG.i(job.name.toUpperCase(), "- Finished because ABORTED.");
                                job.source.trySetException(new CancellationException());
                            } else {
                                LOG.i(job.name.toUpperCase(), "- Finished.");
                                job.source.trySetResult(task.getResult());
                            }
                            synchronized (mJobsLock) {
                                executed(job);
                            }
                        }
                    });
                } catch (Exception e) {
                    LOG.i(job.name.toUpperCase(), "- Finished with ERROR.", e);
                    if (job.dispatchExceptions) {
                        mCallback.handleJobException(job.name, e);
                    }
                    job.source.trySetException(e);
                    synchronized (mJobsLock) {
                        executed(job);
                    }
                }
            }
        });
    }

    @GuardedBy("mJobsLock")
    private <T> void executed(Job<T> job) {
        if (!mJobRunning) {
            throw new IllegalStateException("mJobRunning was not true after completing job=" + job.name);
        }
        mJobRunning = false;
        mJobs.remove(job);
        sync(0L);
    }

    public void remove(@NonNull String name) {
        trim(name, 0);
    }

    public void trim(@NonNull String name, int allowed) {
        synchronized (mJobsLock) {
            List<Job<?>> scheduled = new ArrayList<>();
            for (Job<?> job : mJobs) {
                if (job.name.equals(name)) {
                    scheduled.add(job);
                }
            }
            LOG.v("trim: name=", name, "scheduled=", scheduled.size(), "allowed=", allowed);
            int existing = Math.max(scheduled.size() - allowed, 0);
            if (existing > 0) {
                // To remove the oldest ones first, we must reverse the list.
                // Note that we will potentially remove a job that is being executed: we don't
                // have a mechanism to cancel the ongoing execution, but it shouldn't be a problem.
                Collections.reverse(scheduled);
                scheduled = scheduled.subList(0, existing);
                for (Job<?> job : scheduled) {
                    mJobs.remove(job);
                }
            }
        }
    }

    public void reset() {
        synchronized (mJobsLock) {
            Set<String> all = new HashSet<>();
            for (Job<?> job : mJobs) {
                all.add(job.name);
            }
            for (String job : all) {
                remove(job);
            }
        }
    }

    private static <T> void onComplete(@NonNull final Task<T> task,
                                       @NonNull WorkerHandler handler,
                                       @NonNull final OnCompleteListener<T> listener) {
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

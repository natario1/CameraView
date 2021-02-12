package com.otaliastudios.cameraview.internal;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.CameraLogger;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * Class holding a background handler.
 * We want them to survive configuration changes if there's still job to do.
 */
public class WorkerHandler {

    private final static CameraLogger LOG
            = CameraLogger.create(WorkerHandler.class.getSimpleName());
    private final static ConcurrentHashMap<String, WeakReference<WorkerHandler>> sCache
            = new ConcurrentHashMap<>(4);

    private final static String FALLBACK_NAME = "FallbackCameraThread";

    // Store a hard reference to the fallback handler. We never use this, only update it
    // anytime get() is called. This should ensure that this instance is not collected.
    @SuppressWarnings("FieldCanBeLocal")
    private static WorkerHandler sFallbackHandler;

    /**
     * Gets a possibly cached handler with the given name.
     * @param name the handler name
     * @return a handler
     */
    @NonNull
    public static WorkerHandler get(@NonNull String name) {
        if (sCache.containsKey(name)) {
            //noinspection ConstantConditions
            WorkerHandler cached = sCache.get(name).get();
            if (cached != null) {
                if (cached.getThread().isAlive() && !cached.getThread().isInterrupted()) {
                    LOG.w("get:", "Reusing cached worker handler.", name);
                    return cached;
                } else {
                    // Cleanup the old thread before creating a new one
                    cached.destroy();
                    LOG.w("get:", "Thread reference found, but not alive or interrupted.",
                            "Removing.", name);
                    sCache.remove(name);
                }
            } else {
                LOG.w("get:", "Thread reference died. Removing.", name);
                sCache.remove(name);
            }
        }

        LOG.i("get:", "Creating new handler.", name);
        WorkerHandler handler = new WorkerHandler(name);
        sCache.put(name, new WeakReference<>(handler));
        return handler;
    }

    /**
     * Returns a fallback WorkerHandler.
     * @return a fallback handler
     */
    @NonNull
    public static WorkerHandler get() {
        sFallbackHandler = get(FALLBACK_NAME);
        return sFallbackHandler;
    }

    /**
     * Handy utility to perform an action in a fallback thread.
     * Not to be used for long-running operations since they will block
     * the fallback thread.
     *
     * @param action the action
     */
    public static void execute(@NonNull Runnable action) {
        get().post(action);
    }

    private String mName;
    private HandlerThread mThread;
    private Handler mHandler;
    private Executor mExecutor;

    private WorkerHandler(@NonNull String name) {
        mName = name;
        mThread = new HandlerThread(name) {
            @NonNull
            @Override
            public String toString() {
                return super.toString() + "[" + getThreadId() + "]";
            }
        };
        mThread.setDaemon(true);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                WorkerHandler.this.run(command);
            }
        };

        // HandlerThreads/Handlers sometimes have a significant warmup time.
        // We want to spend this time here so when this object is built, it
        // is fully operational.
        final CountDownLatch latch = new CountDownLatch(1);
        post(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException ignore) {}
    }

    /**
     * Post an action on this handler.
     * @param runnable the action
     */
    public void run(@NonNull Runnable runnable) {
        if (Thread.currentThread() == getThread()) {
            runnable.run();
        } else {
            post(runnable);
        }
    }

    /**
     * Post an action on this handler.
     * @param callable the action
     */
    public <T> Task<T> run(@NonNull Callable<T> callable) {
        if (Thread.currentThread() == getThread()) {
            try {
                return Tasks.forResult(callable.call());
            } catch (Exception e) {
                return Tasks.forException(e);
            }
        } else {
            return post(callable);
        }
    }

    /**
     * Post an action on this handler.
     * @param runnable the action
     */
    public void post(@NonNull Runnable runnable) {
        mHandler.post(runnable);
    }

    /**
     * Post an action on this handler.
     * @param callable the action
     */
    public <T> Task<T> post(@NonNull final Callable<T> callable) {
        final TaskCompletionSource<T> source = new TaskCompletionSource<>();
        post(new Runnable() {
            @Override
            public void run() {
                try {
                    source.trySetResult(callable.call());
                } catch (Exception e) {
                    source.trySetException(e);
                }
            }
        });
        return source.getTask();
    }

    /**
     * Post an action on this handler.
     * @param delay the delay in millis
     * @param runnable the action
     */
    public void post(long delay, @NonNull Runnable runnable) {
        mHandler.postDelayed(runnable, delay);
    }

    /**
     * Removes a previously added action from this handler.
     * @param runnable the action
     */
    public void remove(@NonNull Runnable runnable) {
        mHandler.removeCallbacks(runnable);
    }

    /**
     * Returns the android backing {@link Handler}.
     * @return the handler
     */
    @NonNull
    public Handler getHandler() {
        return mHandler;
    }

    /**
     * Returns the android backing {@link HandlerThread}.
     * @return the thread
     */
    @NonNull
    public HandlerThread getThread() {
        return mThread;
    }

    /**
     * Returns the android backing {@link Looper}.
     * @return the looper
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Looper getLooper() {
        return mThread.getLooper();
    }

    /**
     * Returns an {@link Executor}.
     * @return the executor
     */
    @NonNull
    public Executor getExecutor() {
        return mExecutor;
    }

    /**
     * Destroys this handler and its thread. After this method returns, the handler
     * should be considered unusable.
     *
     * Internal note: this does not remove the thread from our cache, but it does
     * interrupt it, so the next {@link #get(String)} call will remove it.
     * In any case, we only store weak references.
     */
    public void destroy() {
        HandlerThread thread = getThread();
        if (thread.isAlive()) {
            thread.interrupt();
            thread.quit();
            // after quit(), the thread will die at some point in the future. Might take some ms.
            // try { handler.getThread().join(); } catch (InterruptedException ignore) {}
        }
        // This should not be needed, but just to be sure, let's remove it from cache.
        // For example, interrupt() won't interrupt the thread if it's blocked - it will throw
        // an exception instead.
        sCache.remove(mName);
    }

    /**
     * Destroys all handlers, interrupting their work and
     * removing them from our cache.
     */
    public static void destroyAll() {
        for (String key : sCache.keySet()) {
            WeakReference<WorkerHandler> ref = sCache.get(key);
            //noinspection ConstantConditions
            WorkerHandler handler = ref.get();
            if (handler != null) handler.destroy();
            ref.clear();
        }
        sCache.clear();
    }
}

package com.otaliastudios.cameraview.internal.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.otaliastudios.cameraview.CameraLogger;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class holding a background handler.
 * We want them to survive configuration changes if there's still job to do.
 */
public class WorkerHandler {

    private final static CameraLogger LOG = CameraLogger.create(WorkerHandler.class.getSimpleName());
    private final static ConcurrentHashMap<String, WeakReference<WorkerHandler>> sCache = new ConcurrentHashMap<>(4);

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
                HandlerThread thread = cached.mThread;
                if (thread.isAlive() && !thread.isInterrupted()) {
                    LOG.w("get:", "Reusing cached worker handler.", name);
                    return cached;
                }
            }
            LOG.w("get:", "Thread reference died, removing.", name);
            sCache.remove(name);
        }

        LOG.i("get:", "Creating new handler.", name);
        WorkerHandler handler = new WorkerHandler(name);
        sCache.put(name, new WeakReference<>(handler));
        return handler;
    }

    /**
     * Handy utility to perform an action in a fallback thread.
     * Not to be used for long-running operations since they will block
     * the fallback thread.
     *
     * @param action the action
     */
    public static void run(@NonNull Runnable action) {
        get("FallbackCameraThread").post(action);
    }

    private HandlerThread mThread;
    private Handler mHandler;

    private WorkerHandler(@NonNull String name) {
        mThread = new HandlerThread(name);
        mThread.setDaemon(true);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
    }

    /**
     * Post an action on this handler.
     * @param runnable the action
     */
    public void post(@NonNull Runnable runnable) {
        mHandler.post(runnable);
    }

    /**
     * Returns the android backing {@link Handler}.
     * @return the handler
     */
    public Handler get() {
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
    @NonNull
    public Looper getLooper() {
        return mThread.getLooper();
    }

    /**
     * Destroys all handlers, interrupting their work and
     * removing them from our cache.
     */
    public static void destroy() {
        for (String key : sCache.keySet()) {
            WeakReference<WorkerHandler> ref = sCache.get(key);
            //noinspection ConstantConditions
            WorkerHandler handler = ref.get();
            if (handler != null && handler.getThread().isAlive()) {
                handler.getThread().interrupt();
                // handler.getThread().quit();
            }
            ref.clear();
        }
        sCache.clear();
    }
}

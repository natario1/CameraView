package com.otaliastudios.cameraview;

import android.os.Handler;
import android.os.HandlerThread;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class holding a background handler.
 * We want them to survive configuration changes if there's still job to do.
 */
class WorkerHandler {

    private static ConcurrentHashMap<String, WeakReference<WorkerHandler>> sCache = new ConcurrentHashMap<>(4);

    public static WorkerHandler get(String name) {
        if (sCache.containsKey(name)) {
            WorkerHandler cached = sCache.get(name).get();
            if (cached != null) {
                HandlerThread thread = cached.mThread;
                if (thread.isAlive() && !thread.isInterrupted()) {
                    return cached;
                }
            }
            sCache.remove(name);
        }
        WorkerHandler handler = new WorkerHandler(name);
        sCache.put(name, new WeakReference<>(handler));
        return handler;
    }

    private HandlerThread mThread;
    private Handler mHandler;

    public WorkerHandler(String name) {
        mThread = new HandlerThread(name);
        mThread.setDaemon(true);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
    }

    public Handler get() {
        return mHandler;
    }

    public void post(Runnable runnable) {
        mHandler.post(runnable);
    }
}

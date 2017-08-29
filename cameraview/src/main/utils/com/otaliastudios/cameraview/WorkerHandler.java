package com.otaliastudios.cameraview;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Class holding a background handler.
 * Using setDaemon right now but a better approach would be to have
 * start() and stop() callbacks here. TODO
 */
class WorkerHandler {

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

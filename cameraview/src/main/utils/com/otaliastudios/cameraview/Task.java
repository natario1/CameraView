package com.otaliastudios.cameraview;

import androidx.annotation.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A naive implementation of {@link java.util.concurrent.CountDownLatch}
 * to help in testing.
 */
class Task<T> {

    private CountDownLatch mLatch;
    private T mResult;
    private int mCount;

    Task() {
    }

    Task(boolean startListening) {
        if (startListening) listen();
    }

    private boolean listening() {
        return mLatch != null;
    }

    void listen() {
        if (listening()) throw new RuntimeException("Should not happen.");
        mResult = null;
        mLatch = new CountDownLatch(1);
    }

    void start() {
        if (!listening()) mCount++;
    }

    void end(T result) {
        if (mCount > 0) {
            mCount--;
            return;
        }

        if (listening()) { // Should be always true.
            mResult = result;
            mLatch.countDown();
        }
    }

    T await(long millis) {
        return await(millis, TimeUnit.MILLISECONDS);
    }

    T await() {
        return await(1, TimeUnit.MINUTES);
    }

    private T await(long time, @NonNull TimeUnit unit) {
        try {
            mLatch.await(time, unit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        T result = mResult;
        mResult = null;
        mLatch = null;
        return result;
    }


}

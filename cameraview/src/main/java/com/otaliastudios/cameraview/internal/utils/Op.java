package com.otaliastudios.cameraview.internal.utils;

import androidx.annotation.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A naive implementation of {@link java.util.concurrent.CountDownLatch}
 * to help in testing.
 */
public class Op<T> {

    private CountDownLatch mLatch;
    private T mResult;
    private int mCount;

    /**
     * Creates an empty task.
     *
     * Listeners should:
     * - call {@link #listen()} to notify they are interested in the next action
     * - call {@link #await()} to know when the action is performed.
     *
     * Op owners should:
     * - call {@link #start()} when task started
     * - call {@link #end(Object)} when task ends
     */
    public Op() { }

    /**
     * Creates an empty task and starts listening.
     * @param startListening whether to call listen
     */
    public Op(boolean startListening) {
        if (startListening) listen();
    }

    private boolean isListening() {
        return mLatch != null;
    }

    /**
     * Op owner method: notifies the action started.
     */
    public void start() {
        if (!isListening()) mCount++;
    }

    /**
     * Op owner method: notifies the action ended.
     * @param result the action result
     */
    public void end(T result) {
        if (mCount > 0) {
            mCount--;
            return;
        }

        if (isListening()) { // Should be always true.
            mResult = result;
            mLatch.countDown();
        }
    }

    /**
     * Listener method: notifies we are interested in the next action.
     */
    public void listen() {
        if (isListening()) throw new RuntimeException("Should not happen.");
        mResult = null;
        mLatch = new CountDownLatch(1);
    }

    /**
     * Listener method: waits for next task action to end.
     * @param millis milliseconds
     * @return the action result
     */
    public T await(long millis) {
        return await(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Listener method: waits 1 minute for next task action to end.
     * @return the action result
     */
    public T await() {
        return await(1, TimeUnit.MINUTES);
    }

    /**
     * Listener method: waits for next task action to end.
     * @param time time
     * @param unit the time unit
     * @return the action result
     */
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

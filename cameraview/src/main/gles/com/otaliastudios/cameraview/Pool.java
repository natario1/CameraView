package com.otaliastudios.cameraview;

import java.util.concurrent.LinkedBlockingQueue;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class Pool<T> {

    private static final String TAG = Pool.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private int maxPoolSize;
    private int activeCount;
    private LinkedBlockingQueue<T> mQueue;
    private Factory<T> factory;

    interface Factory<T> {
        T create();
    }

    Pool(int maxPoolSize, Factory<T> factory) {
        this.maxPoolSize = maxPoolSize;
        this.mQueue = new LinkedBlockingQueue<>(maxPoolSize);
        this.factory = factory;
    }

    boolean canGet() {
        return count() < maxPoolSize;
    }

    @Nullable
    T get() {
        T buffer = mQueue.poll();
        if (buffer != null) {
            activeCount++; // poll decreases, this fixes
            LOG.v("GET: Reusing recycled item.", this);
            return buffer;
        }

        if (!canGet()) {
            LOG.v("GET: Returning null. Too much items requested.", this);
            return null;
        }

        activeCount++;
        LOG.v("GET: Creating a new item.", this);
        return factory.create();
    }


    void recycle(@NonNull T item) {
        LOG.v("RECYCLE: Recycling item.", this);
        if (--activeCount < 0) {
            throw new IllegalStateException("Trying to recycle an item which makes activeCount < 0." +
                    "This means that this or some previous items being recycled were not coming from " +
                    "this pool, or some item was recycled more than once. " + this);
        }
        if (!mQueue.offer(item)) {
            throw new IllegalStateException("Trying to recycle an item while the queue is full. " +
                    "This means that this or some previous items being recycled were not coming from " +
                    "this pool, or some item was recycled more than once. " + this);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + " -- count:" + count() + ", active:" + activeCount() + ", cached:" + cachedCount();
    }

    final int count() {
        return activeCount() + cachedCount();
    }

    final int activeCount() {
        return activeCount;
    }

    final int cachedCount() {
        return mQueue.size();
    }

    @CallSuper
    void clear() {
        mQueue.clear();
    }
}

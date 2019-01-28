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

    @Nullable
    T get() {
        if (count() >= maxPoolSize) {
            LOG.v("GET: Returning null. Too much items requested.");
            return null;
        }

        T buffer = mQueue.poll();
        if (buffer != null) {
            activeCount++;
            LOG.v("GET: Reusing recycled item. Count", count(), "Active", activeCount(), "Cached", cachedCount());
            return buffer;
        }

        activeCount++;
        LOG.v("GET: Creating a new item. Count", count(), "Active", activeCount(), "Cached", cachedCount());
        return factory.create();
    }


    void recycle(@NonNull T buffer) {
        LOG.v("RECYCLE: Recycling item. Count", count(), "Active", activeCount(), "Cached", cachedCount());
        if (--activeCount < 0) {
            throw new IllegalStateException("Trying to recycle an item which makes activeCount < 0." +
                    "This means that this or some previous items being recycled were not coming from " +
                    "this pool, or some item was recycled more than once.");
        }
        if (!mQueue.offer(buffer)) {
            throw new IllegalStateException("Trying to recycle an item while the queue is full. " +
                    "This means that this or some previous items being recycled were not coming from " +
                    "this pool, or some item was recycled more than once.");
        }
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

package com.otaliastudios.cameraview.internal;

import com.otaliastudios.cameraview.CameraLogger;

import java.util.concurrent.LinkedBlockingQueue;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Base class for thread-safe pools of recycleable objects.
 * @param <T> the object type
 */
public class Pool<T> {

    private static final String TAG = Pool.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private int maxPoolSize;
    private int activeCount;
    private LinkedBlockingQueue<T> queue;
    private Factory<T> factory;
    private final Object lock = new Object();

    /**
     * Used to create new instances of objects when needed.
     * @param <T> object type
     */
    public interface Factory<T> {
        T create();
    }

    /**
     * Creates a new pool with the given pool size and factory.
     * @param maxPoolSize the max pool size
     * @param factory the factory
     */
    public Pool(int maxPoolSize, @NonNull Factory<T> factory) {
        this.maxPoolSize = maxPoolSize;
        this.queue = new LinkedBlockingQueue<>(maxPoolSize);
        this.factory = factory;
    }

    /**
     * Whether the pool is empty. This means that {@link #get()} will return
     * a null item, because all objects were reclaimed and not recycled yet.
     *
     * @return whether the pool is empty
     */
    public boolean isEmpty() {
        synchronized (lock) {
            return count() >= maxPoolSize;
        }
    }

    /**
     * Returns a new item, from the recycled pool if possible (if there are recycled items),
     * or instantiating one through the factory (if we can respect the pool size).
     * If these conditions are not met, this returns null.
     *
     * @return an item or null
     */
    @Nullable
    public T get() {
        synchronized (lock) {
            T item = queue.poll();
            if (item != null) {
                activeCount++; // poll decreases, this fixes
                LOG.v("GET - Reusing recycled item.", this);
                return item;
            }

            if (isEmpty()) {
                LOG.v("GET - Returning null. Too much items requested.", this);
                return null;
            }

            activeCount++;
            LOG.v("GET - Creating a new item.", this);
            return factory.create();
        }
    }

    /**
     * Recycles an item after it has been used. The item should come from a previous
     * {@link #get()} call.
     *
     * @param item used item
     */
    public void recycle(@NonNull T item) {
        synchronized (lock) {
            LOG.v("RECYCLE - Recycling item.", this);
            if (--activeCount < 0) {
                throw new IllegalStateException("Trying to recycle an item which makes " +
                        "activeCount < 0. This means that this or some previous items being " +
                        "recycled were not coming from this pool, or some item was recycled " +
                        "more than once. " + this);
            }
            if (!queue.offer(item)) {
                throw new IllegalStateException("Trying to recycle an item while the queue " +
                        "is full. This means that this or some previous items being recycled " +
                        "were not coming from this pool, or some item was recycled " +
                        "more than once. " + this);
            }
        }
    }

    /**
     * Clears the pool of recycled items.
     */
    @CallSuper
    public void clear() {
        synchronized (lock) {
            queue.clear();
        }
    }

    /**
     * Returns the count of all items managed by this pool. Includes
     * - active items: currently being used
     * - recycled items: used and recycled, available for second use
     *
     * @return count
     */
    public final int count() {
        synchronized (lock) {
            return activeCount() + recycledCount();
        }
    }

    /**
     * Returns the active items managed by this pools, which means, items
     * currently being used.
     *
     * @return active count
     */
    @SuppressWarnings("WeakerAccess")
    public final int activeCount() {
        synchronized (lock) {
            return activeCount;
        }
    }

    /**
     * Returns the recycled items managed by this pool, which means, items
     * that were used and later recycled, and are currently available for
     * second use.
     *
     * @return recycled count
     */
    @SuppressWarnings("WeakerAccess")
    public final int recycledCount() {
        synchronized (lock) {
            return queue.size();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + " - count:" + count() + ", active:" + activeCount() + ", recycled:" + recycledCount();
    }
}

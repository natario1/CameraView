package com.otaliastudios.cameraview.internal;


import com.otaliastudios.cameraview.internal.Pool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

public class PoolTest {

    private final static int MAX_SIZE = 20;

    private class Item {}

    private Pool<Item> pool;
    private int instances = 0;

    @Before
    public void setUp() {
        pool = new Pool<>(MAX_SIZE, new Pool.Factory<Item>() {
            @Override
            public Item create() {
                instances++;
                return new Item();
            }
        });
    }

    @After
    public void tearDown() {
        instances = 0;
        pool = null;
    }

    @Test
    public void testInstances() {
        for (int i = 0; i < MAX_SIZE; i++) {
            assertEquals(instances, i);
            pool.get();
        }
    }

    @Test
    public void testIsEmtpy() {
        assertFalse(pool.isEmpty());

        // Get all items without recycling.
        Item item = null;
        for (int i = 0; i < MAX_SIZE; i++) {
            item = pool.get();
        }
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testClear() {
        // Take one and recycle it
        Item item = pool.get();
        assertNotNull(item);
        pool.recycle(item);

        // Ensure it is recycled.
        assertEquals(pool.recycledCount(), 1);
        assertEquals(pool.activeCount(), 0);
        assertEquals(pool.count(), 1);

        // Now clear and ensure pool is empty.
        pool.clear();
        assertEquals(pool.recycledCount(), 0);
        assertEquals(pool.activeCount(), 0);
        assertEquals(pool.count(), 0);
    }

    @Test
    public void testCounts() {
        assertEquals(pool.recycledCount(), 0);
        assertEquals(pool.activeCount(), 0);
        assertEquals(pool.count(), 0);

        // Take all
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < MAX_SIZE; i++) {
            items.add(pool.get());
            assertEquals(pool.recycledCount(), 0);
            assertEquals(pool.activeCount(), items.size());
            assertEquals(pool.count(), items.size());
        }

        // Recycle all
        int recycled = 0;
        for (Item item : items) {
            pool.recycle(item);
            recycled++;
            assertEquals(pool.recycledCount(), recycled);
            assertEquals(pool.activeCount(), MAX_SIZE - recycled);
            assertEquals(pool.count(), MAX_SIZE);
        }
    }

    @Test
    public void testToString() {
        String string = pool.toString();
        assertTrue(string.contains("count"));
        assertTrue(string.contains("active"));
        assertTrue(string.contains("recycled"));
        assertTrue(string.contains(Pool.class.getSimpleName()));
    }

    @Test(expected = IllegalStateException.class)
    public void testRecycle_notActive() {
        Item item = new Item();
        pool.recycle(item);
    }

    @Test(expected = IllegalStateException.class)
    public void testRecycle_twice() {
        Item item = pool.get();
        assertNotNull(item);
        pool.recycle(item);
        pool.recycle(item);
    }

    @Test(expected = IllegalStateException.class)
    public void testRecycle_whileFull() {
        // Take all and recycle all
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < MAX_SIZE; i++) {
            items.add(pool.get());
        }
        for (Item item : items) {
            pool.recycle(item);
        }
        // Take one and recycle again
        pool.recycle(items.get(0));
    }

    @Test
    public void testGet_fromFactory() {
        pool.get();
        assertEquals(1, instances);
    }

    @Test
    public void testGet_whenFull() {
        for (int i = 0; i < MAX_SIZE; i++) {
            pool.get();
        }
        assertNull(pool.get());
    }

    @Test
    public void testGet_recycled() {
        Item item = pool.get();
        assertNotNull(item);
        pool.recycle(item);
        Item newItem = pool.get();
        assertEquals(item, newItem);
        assertEquals(1, instances);
    }
}

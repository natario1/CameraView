package com.otaliastudios.cameraview.internal.utils;


import com.otaliastudios.cameraview.BaseTest;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WorkerHandlerTest extends BaseTest {

    @Test
    public void testCache() {
        WorkerHandler w1 = WorkerHandler.get("handler1");
        WorkerHandler w1a = WorkerHandler.get("handler1");
        WorkerHandler w2 = WorkerHandler.get("handler2");
        assertSame(w1, w1a);
        assertNotSame(w1, w2);
    }

    @Test
    public void testStaticRun() {
        final Op<Boolean> op = new Op<>(true);
        Runnable action = new Runnable() {
            @Override
            public void run() {
                op.end(true);
            }
        };
        WorkerHandler.run(action);
        Boolean result = op.await(500);
        assertNotNull(result);
        assertTrue(result);
    }
}

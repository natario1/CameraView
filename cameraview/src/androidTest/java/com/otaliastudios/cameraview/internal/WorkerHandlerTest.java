package com.otaliastudios.cameraview.internal;


import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.internal.WorkerHandler;
import com.otaliastudios.cameraview.tools.Op;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WorkerHandlerTest extends BaseTest {

    @Test
    public void testGetFromCache() {
        WorkerHandler first = WorkerHandler.get("first");
        WorkerHandler second = WorkerHandler.get("first");
        assertSame(first, second);
    }

    @Test
    public void testGetAnother() {
        WorkerHandler first = WorkerHandler.get("first");
        WorkerHandler second = WorkerHandler.get("second");
        assertNotSame(first, second);
    }

    @NonNull
    private Runnable getRunnableForOp(final @NonNull Op<Boolean> op) {
        return new Runnable() {
            @Override
            public void run() {
                op.controller().end(true);
            }
        };
    }

    @NonNull
    private Callable<Boolean> getCallableForOp(final @NonNull Op<Boolean> op) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() {
                op.controller().end(true);
                return true;
            }
        };
    }

    @NonNull
    private Callable<Void> getThrowCallable() {
        return new Callable<Void>() {
            @Override
            public Void call() {
                throw new RuntimeException("Fake error");
            }
        };
    }

    private void waitOp(@NonNull Op<Boolean> op) {
        Boolean result = op.await(500);
        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    public void testFallbackExecute() {
        final Op<Boolean> op = new Op<>();
        WorkerHandler.execute(getRunnableForOp(op));
        waitOp(op);
    }

    @Test
    public void testPostRunnable() {
        WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op = new Op<>();
        handler.post(getRunnableForOp(op));
        waitOp(op);
    }

    @Test
    public void testPostCallable() {
        WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op = new Op<>();
        handler.post(getCallableForOp(op));
        waitOp(op);
    }

    @Test
    public void testPostCallable_throws() {
        WorkerHandler handler = WorkerHandler.get("handler");
        Task<Void> task = handler.post(getThrowCallable());
        try { Tasks.await(task); } catch (ExecutionException | InterruptedException ignore) {}
        assertTrue(task.isComplete());
        assertFalse(task.isSuccessful());
    }

    @Test
    public void testRunRunnable_background() {
        WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op = new Op<>();
        handler.run(getRunnableForOp(op));
        waitOp(op);
    }

    @Test
    public void testRunRunnable_sameThread() {
        final WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op1 = new Op<>();
        final Op<Boolean> op2 = new Op<>();
        handler.post(new Runnable() {
            @Override
            public void run() {
                handler.run(getRunnableForOp(op2));
                assertTrue(op2.await(0)); // Do not wait.
                op1.controller().end(true);
            }
        });
        waitOp(op1);
    }

    @Test
    public void testRunCallable_background() {
        WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op = new Op<>();
        handler.run(getCallableForOp(op));
        waitOp(op);
    }

    @Test
    public void testRunCallable_sameThread() {
        final WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op1 = new Op<>();
        final Op<Boolean> op2 = new Op<>();
        handler.post(new Runnable() {
            @Override
            public void run() {
                handler.run(getCallableForOp(op2));
                assertTrue(op2.await(0)); // Do not wait.
                op1.controller().end(true);
            }
        });
        waitOp(op1);
    }

    @Test
    public void testRunCallable_sameThread_throws() {
        final WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op = new Op<>();
        handler.post(new Runnable() {
            @Override
            public void run() {
                Task<Void> task = handler.run(getThrowCallable());
                assertTrue(task.isComplete()); // Already complete
                assertFalse(task.isSuccessful());
                op.controller().end(true);
            }
        });
        waitOp(op);
    }

    @Test
    public void testPostDelayed_tooEarly() {
        final WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op = new Op<>();
        handler.post(1000, getRunnableForOp(op));
        assertNull(op.await(500));
    }

    @Test
    public void testPostDelayed() {
        final WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op = new Op<>();
        handler.post(1000, getRunnableForOp(op));
        assertNotNull(op.await(2000));
    }

    @Test
    public void testRemove() {
        final WorkerHandler handler = WorkerHandler.get("handler");
        final Op<Boolean> op = new Op<>();
        Runnable runnable = getRunnableForOp(op);
        handler.post(1000, runnable);
        handler.remove(runnable);
        assertNull(op.await(2000));
    }

    @Test
    public void testGetters() {
        final WorkerHandler handler = WorkerHandler.get("handler");
        assertNotNull(handler.getExecutor());
        assertNotNull(handler.getHandler());
        assertNotNull(handler.getLooper());
        assertNotNull(handler.getThread());
    }

    @Test
    public void testExecutor() {
        final WorkerHandler handler = WorkerHandler.get("handler");
        Executor executor = handler.getExecutor();
        final Op<Boolean> op = new Op<>();
        executor.execute(getRunnableForOp(op));
        waitOp(op);
    }

    @Test
    public void testDestroy() {
        final WorkerHandler handler = WorkerHandler.get("handler");
        assertTrue(handler.getThread().isAlive());
        handler.destroy();
        WorkerHandler newHandler = WorkerHandler.get("handler");
        assertNotSame(handler, newHandler);
        assertTrue(newHandler.getThread().isAlive());
        // Ensure old thread dies at some point.
        try { handler.getThread().join(500); } catch (InterruptedException ignore) {}
        assertFalse(handler.getThread().isAlive());
    }

    @Test
    public void testDestroyAll() {
        final WorkerHandler handler1 = WorkerHandler.get("handler1");
        final WorkerHandler handler2 = WorkerHandler.get("handler2");
        WorkerHandler.destroyAll();
        WorkerHandler newHandler1 = WorkerHandler.get("handler1");
        WorkerHandler newHandler2 = WorkerHandler.get("handler2");
        assertNotSame(handler1, newHandler1);
        assertNotSame(handler2, newHandler2);
    }
}

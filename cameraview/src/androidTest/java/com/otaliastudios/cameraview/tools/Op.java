package com.otaliastudios.cameraview.tools;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.otaliastudios.cameraview.controls.Control;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A naive implementation of {@link java.util.concurrent.CountDownLatch}
 * to help in testing.
 */
public class Op<T> {

    public class Controller {
        private int mToBeIgnored;

        private Controller() { }

        /** Op owner method: notifies the action started. */
        public void start() {
            if (!isListening()) mToBeIgnored++;
        }

        /** Op owner method: notifies the action ended. */
        public void end(T result) {
            if (mToBeIgnored > 0) {
                mToBeIgnored--;
                return;
            }

            if (isListening()) { // Should be always true.
                mResult = result;
                mLatch.countDown();
            }
        }

        public void from(@NonNull Task<T> task) {
            start();
            task.addOnSuccessListener(new OnSuccessListener<T>() {
                @Override
                public void onSuccess(T result) {
                    end(result);
                }
            });
        }

        @NonNull
        public Stubber from(final int invocationArgument) {
            return Mockito.doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) {
                    //noinspection unchecked
                    T o = (T) invocation.getArguments()[invocationArgument];
                    start();
                    end(o);
                    return null;
                }
            });
        }
    }

    private CountDownLatch mLatch;
    private Controller mController = new Controller();
    private T mResult;

    /**
     * Listeners should:
     * - call {@link #listen()} to notify they are interested in the next action
     * - call {@link #await()} to know when the action is performed.
     *
     * Op owners should:
     * - call {@link Controller#start()} when task started
     * - call {@link Controller#end(Object)} when task ends
     */
    public Op() {
        this(true);
    }

    public Op(boolean startListening) {
        if (startListening) listen();
    }

    public Op(@NonNull Task<T> task) {
        listen();
        controller().from(task);
    }

    private boolean isListening() {
        return mLatch != null;
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

    @NonNull
    public Controller controller() {
        return mController;
    }
}

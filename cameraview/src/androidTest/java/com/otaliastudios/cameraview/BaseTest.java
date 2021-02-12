package com.otaliastudios.cameraview;


import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.otaliastudios.cameraview.tools.Op;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.util.concurrent.CountDownLatch;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static org.mockito.Mockito.doAnswer;

public class BaseTest {

    private static KeyguardManager.KeyguardLock keyguardLock;
    private static PowerManager.WakeLock wakeLock;

    // https://github.com/linkedin/test-butler/blob/bc2bb4df13d0a554d2e2b0ea710795017717e710/test-butler-app/src/main/java/com/linkedin/android/testbutler/ButlerService.java#L121
    @BeforeClass
    public static void beforeClass_wakeUp() {
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);

        // Acquire a keyguard lock to prevent the lock screen from randomly appearing and breaking tests
        KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(KEYGUARD_SERVICE);
        keyguardLock = keyguardManager.newKeyguardLock("CameraViewLock");
        keyguardLock.disableKeyguard();

        // Acquire a wake lock to prevent the cpu from going to sleep and breaking tests
        PowerManager powerManager = (PowerManager) getContext().getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "CameraViewLock");
        wakeLock.acquire();
    }

    @AfterClass
    public static void afterClass_releaseWakeUp() {
        CameraLogger.setLogLevel(CameraLogger.LEVEL_ERROR);

        wakeLock.release();
        keyguardLock.reenableKeyguard();
    }

    /**
     * This will make mockito report the error when it should.
     * Mockito reports failure on the next mockito invocation, which is terrible
     * since it might be on the next test or even never happen.
     */
    @After
    public void after_checkMockito() {
        Object object = Mockito.mock(Object.class);
        //noinspection ResultOfMethodCallIgnored
        object.toString();
    }

    @NonNull
    protected static Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getContext();
    }

    protected static void uiSync(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    @SuppressWarnings("unused")
    protected static void uiAsync(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    @SuppressWarnings("unused")
    protected static void waitUiIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @NonNull
    protected static Stubber doCountDown(@NonNull final CountDownLatch latch) {
        return doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                latch.countDown();
                return null;
            }
        });
    }

    @NonNull
    protected static <T> Stubber doEndOp(final Op<T> op, final T response) {
        return doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                op.controller().end(response);
                return null;
            }
        });
    }

    @NonNull
    protected static <T> Stubber doEndOp(final Op<T> op, final int withReturnArgument) {
        return op.controller().from(withReturnArgument);
    }
}

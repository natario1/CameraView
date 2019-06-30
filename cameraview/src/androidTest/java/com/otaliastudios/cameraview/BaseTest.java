package com.otaliastudios.cameraview;


import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import androidx.test.platform.app.InstrumentationRegistry;

import android.util.Log;
import android.view.View;

import com.otaliastudios.cameraview.internal.utils.Op;

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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class BaseTest {

    public static CameraLogger LOG = CameraLogger.create("Test");

    private static KeyguardManager.KeyguardLock keyguardLock;
    private static PowerManager.WakeLock wakeLock;

    // https://github.com/linkedin/test-butler/blob/bc2bb4df13d0a554d2e2b0ea710795017717e710/test-butler-app/src/main/java/com/linkedin/android/testbutler/ButlerService.java#L121
    @BeforeClass
    @SuppressWarnings("MissingPermission")
    public static void wakeUp() {
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);

        // Acquire a keyguard lock to prevent the lock screen from randomly appearing and breaking tests
        KeyguardManager keyguardManager = (KeyguardManager) context().getSystemService(KEYGUARD_SERVICE);
        keyguardLock = keyguardManager.newKeyguardLock("CameraViewLock");
        keyguardLock.disableKeyguard();

        // Acquire a wake lock to prevent the cpu from going to sleep and breaking tests
        PowerManager powerManager = (PowerManager) context().getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "CameraViewLock");
        wakeLock.acquire();
    }

    @AfterClass
    @SuppressWarnings("MissingPermission")
    public static void releaseWakeUp() {
        wakeLock.release();
        keyguardLock.reenableKeyguard();
    }

    /**
     * This will make mockito report the error when it should.
     * Mockito reports failure on the next mockito invocation, which is terrible
     * since it might be on the next test or even never happen.
     *
     * Calling this
     */
    @After
    public void syncMockito() {
        Object object = Mockito.mock(Object.class);
        object.toString();
    }

    public static void ui(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    public static void uiAsync(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public static Context context() {
        return InstrumentationRegistry.getInstrumentation().getContext();
    }

    public static void uiRequestLayout(final View view) {
        ui(new Runnable() {
            @Override
            public void run() {
                view.requestLayout();
            }
        });
    }

    public static void idle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void grantPermissions() {
        grantPermission("android.permission.CAMERA");
        grantPermission("android.permission.RECORD_AUDIO");
        grantPermission("android.permission.WRITE_EXTERNAL_STORAGE");
    }

    public static void grantPermission(String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        String command = "pm grant " + context().getPackageName() + " " + permission;
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
    }

    public static Stubber doCountDown(final CountDownLatch latch) {
        return doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                latch.countDown();
                return null;
            }
        });
    }

    public static <T> Stubber doEndTask(final Op<T> op, final T response) {
        return doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                op.end(response);
                return null;
            }
        });
    }

    public static Stubber doEndTask(final Op op, final int withReturnArgument) {
        return doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object o = invocation.getArguments()[withReturnArgument];
                //noinspection unchecked
                op.end(o);
                return null;
            }
        });
    }
}

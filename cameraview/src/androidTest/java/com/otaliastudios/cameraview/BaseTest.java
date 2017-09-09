package com.otaliastudios.cameraview;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.core.internal.deps.guava.collect.ObjectArrays;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseTest {

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

    public static void waitUi() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }


    public static byte[] mockJpeg(int width, int height) {
        Bitmap source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        source.compress(Bitmap.CompressFormat.JPEG, 100, os);
        return os.toByteArray();
    }

    public static YuvImage mockYuv(int width, int height) {
        YuvImage y = mock(YuvImage.class);
        when(y.getWidth()).thenReturn(width);
        when(y.getHeight()).thenReturn(height);
        when(y.compressToJpeg(any(Rect.class), anyInt(), any(OutputStream.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                OutputStream stream = (OutputStream) invocation.getArguments()[2];
                stream.write(mockJpeg(rect.width(), rect.height()));
                return true;
            }
        });
        return y;
    }

    public static Stubber doCountDown(final CountDownLatch latch) {
        return doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        });
    }

    public static <T> Stubber doEndTask(final Task<T> task, final T response) {
        return doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                task.end(response);
                return null;
            }
        });
    }

    public static Stubber doEndTask(final Task task, final int withReturnArgument) {
        return doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object o = invocation.getArguments()[withReturnArgument];
                //noinspection unchecked
                task.end(o);
                return null;
            }
        });
    }
}

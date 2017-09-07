package com.otaliastudios.cameraview;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;

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
}

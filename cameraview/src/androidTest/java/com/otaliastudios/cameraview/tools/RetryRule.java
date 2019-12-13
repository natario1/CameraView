package com.otaliastudios.cameraview.tools;

import android.os.Build;

import com.otaliastudios.cameraview.CameraLogger;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryRule implements TestRule {

    private final static String TAG = RetryRule.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private AtomicInteger retries;

    public RetryRule(int retries) {
        this.retries = new AtomicInteger(retries);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Retry retry = description.getAnnotation(Retry.class);
                if (retry == null || retry.emulatorOnly() && !Emulator.isEmulator()) {
                    base.evaluate();
                } else {
                    Throwable caught = null;
                    while (retries.getAndDecrement() > 0) {
                        try {
                            base.evaluate();
                            return;
                        } catch (Throwable throwable) {
                            LOG.e("[RETRY] Test failed.", retries.get(),
                                    "retries available...");
                            LOG.e("*******************************************************");
                            caught = throwable;
                        }
                    }
                    if (caught != null) {
                        throw caught;
                    }
                }
            }
        };
    }
}
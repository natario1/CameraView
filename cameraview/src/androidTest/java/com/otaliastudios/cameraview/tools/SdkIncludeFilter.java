package com.otaliastudios.cameraview.tools;


import android.os.Build;

import androidx.annotation.Nullable;
import androidx.test.internal.runner.filters.ParentFilter;

import org.junit.runner.Description;

/**
 * Filter for {@link SdkInclude}, based on
 * {@link androidx.test.internal.runner.TestRequestBuilder}'s SdkSuppressFilter.
 */
public class SdkIncludeFilter extends ParentFilter {

    protected boolean evaluateTest(Description description) {
        SdkInclude annotation = getAnnotationForTest(description);
        if (annotation != null) {
            if ((!annotation.emulatorOnly() || Emulator.isEmulator())
                    && Build.VERSION.SDK_INT >= annotation.minSdkVersion()
                    && Build.VERSION.SDK_INT <= annotation.maxSdkVersion()) {
                return true; // run the test
            }
            return false; // drop the test
        }
        return true; // no annotation, run the test
    }

    @Nullable
    private SdkInclude getAnnotationForTest(Description description) {
        final SdkInclude s = description.getAnnotation(SdkInclude.class);
        if (s != null) {
            return s;
        }
        final Class<?> testClass = description.getTestClass();
        if (testClass != null) {
            return testClass.getAnnotation(SdkInclude.class);
        }
        return null;
    }

    @Override
    public String describe() {
        return "Skip tests annotated with SdkInclude";
    }
}

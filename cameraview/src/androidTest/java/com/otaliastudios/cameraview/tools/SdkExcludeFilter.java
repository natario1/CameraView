package com.otaliastudios.cameraview.tools;


import android.os.Build;

import androidx.annotation.Nullable;
import androidx.test.internal.runner.filters.ParentFilter;

import org.junit.runner.Description;

/**
 * Filter for {@link SdkExclude}, based on
 * {@link androidx.test.internal.runner.TestRequestBuilder}'s SdkSuppressFilter.
 */
public class SdkExcludeFilter extends ParentFilter {

    protected boolean evaluateTest(Description description) {
        final SdkExclude sdkSuppress = getAnnotationForTest(description);
        if (sdkSuppress != null) {
            if (Build.VERSION.SDK_INT >= sdkSuppress.minSdkVersion()
                    && Build.VERSION.SDK_INT <= sdkSuppress.maxSdkVersion()) {
                return false; // exclude the test
            }
            return true; // run the test
        }
        return true; // no annotation, run the test
    }

    @Nullable
    private SdkExclude getAnnotationForTest(Description description) {
        final SdkExclude s = description.getAnnotation(SdkExclude.class);
        if (s != null) {
            return s;
        }
        final Class<?> testClass = description.getTestClass();
        if (testClass != null) {
            return testClass.getAnnotation(SdkExclude.class);
        }
        return null;
    }

    @Override
    public String describe() {
        return "Skip tests annotated with SdkExclude";
    }
}

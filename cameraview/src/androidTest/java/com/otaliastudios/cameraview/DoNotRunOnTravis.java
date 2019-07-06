package com.otaliastudios.cameraview;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Thanks to a testInstrumentationRunnerArgument in our build file, we will not
 * execute these tests on Travis CI.
 * The {@link RetentionPolicy#RUNTIME} is needed!
 *
 * https://developer.android.com/reference/android/support/test/runner/AndroidJUnitRunner
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DoNotRunOnTravis {
    String because() default "";
}

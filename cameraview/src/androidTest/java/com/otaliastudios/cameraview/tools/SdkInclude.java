package com.otaliastudios.cameraview.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Like {@link androidx.test.filters.SdkSuppress}, but with emulatorOnly().
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SdkInclude {
    /** The minimum API level to run (inclusive) */
    int minSdkVersion() default 1;
    /** The maximum API level to run (inclusive) */
    int maxSdkVersion() default Integer.MAX_VALUE;
    /** Whether this filter only applies to emulators */
    boolean emulatorOnly() default false;
}
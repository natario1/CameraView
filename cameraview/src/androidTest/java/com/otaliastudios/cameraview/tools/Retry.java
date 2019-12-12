package com.otaliastudios.cameraview.tools;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
    boolean emulatorOnly() default false;
}

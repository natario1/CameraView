package com.otaliastudios.cameraview;

public @interface DoNotRunOnTravis {
    String because() default "";
}

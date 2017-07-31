package com.flurgle.camerakit;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.flurgle.camerakit.CameraKit.Constants.CAPTURE_METHOD_STANDARD;
import static com.flurgle.camerakit.CameraKit.Constants.CAPTURE_METHOD_FRAME;

@Deprecated
@Retention(RetentionPolicy.SOURCE)
@IntDef({CAPTURE_METHOD_STANDARD, CAPTURE_METHOD_FRAME})
public @interface Method {
}

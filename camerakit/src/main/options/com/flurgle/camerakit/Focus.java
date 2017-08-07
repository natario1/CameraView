package com.flurgle.camerakit;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.flurgle.camerakit.CameraConstants.FOCUS_TAP;
import static com.flurgle.camerakit.CameraConstants.FOCUS_FIXED;
import static com.flurgle.camerakit.CameraConstants.FOCUS_CONTINUOUS;
import static com.flurgle.camerakit.CameraConstants.FOCUS_TAP_WITH_MARKER;

@Retention(RetentionPolicy.SOURCE)
@IntDef({FOCUS_CONTINUOUS, FOCUS_TAP, FOCUS_FIXED, FOCUS_TAP_WITH_MARKER})
public @interface Focus {
}
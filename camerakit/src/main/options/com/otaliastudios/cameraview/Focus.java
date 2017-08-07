package com.otaliastudios.cameraview;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.otaliastudios.cameraview.CameraConstants.FOCUS_TAP;
import static com.otaliastudios.cameraview.CameraConstants.FOCUS_FIXED;
import static com.otaliastudios.cameraview.CameraConstants.FOCUS_CONTINUOUS;
import static com.otaliastudios.cameraview.CameraConstants.FOCUS_TAP_WITH_MARKER;

@Retention(RetentionPolicy.SOURCE)
@IntDef({FOCUS_CONTINUOUS, FOCUS_TAP, FOCUS_FIXED, FOCUS_TAP_WITH_MARKER})
public @interface Focus {
}
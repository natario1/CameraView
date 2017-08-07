package com.otaliastudios.cameraview;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.otaliastudios.cameraview.CameraConstants.FACING_BACK;
import static com.otaliastudios.cameraview.CameraConstants.FACING_FRONT;

@IntDef({FACING_BACK, FACING_FRONT})
@Retention(RetentionPolicy.SOURCE)
public @interface Facing {
}
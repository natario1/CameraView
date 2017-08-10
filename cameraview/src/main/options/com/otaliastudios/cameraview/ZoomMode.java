package com.otaliastudios.cameraview;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.otaliastudios.cameraview.CameraConstants.ZOOM_OFF;
import static com.otaliastudios.cameraview.CameraConstants.ZOOM_PINCH;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ZOOM_OFF, ZOOM_PINCH})
@Deprecated
public @interface ZoomMode {
}

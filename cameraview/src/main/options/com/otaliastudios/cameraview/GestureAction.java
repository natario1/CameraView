package com.otaliastudios.cameraview;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.otaliastudios.cameraview.CameraConstants.*;

@Retention(RetentionPolicy.SOURCE)
@IntDef({GESTURE_ACTION_NONE, GESTURE_ACTION_FOCUS, GESTURE_ACTION_FOCUS_WITH_MARKER,
        GESTURE_ACTION_AE_CORRECTION, GESTURE_ACTION_CAPTURE, GESTURE_ACTION_ZOOM})
public @interface GestureAction {
}

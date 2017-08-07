package com.otaliastudios.cameraview;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.otaliastudios.cameraview.CameraConstants.PERMISSIONS_PICTURE;
import static com.otaliastudios.cameraview.CameraConstants.PERMISSIONS_VIDEO;

@Deprecated
@Retention(RetentionPolicy.SOURCE)
@IntDef({PERMISSIONS_VIDEO, PERMISSIONS_PICTURE})
public @interface Permissions {
}

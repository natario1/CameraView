package com.flurgle.camerakit;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.flurgle.camerakit.CameraKit.Constants.PERMISSIONS_PICTURE;
import static com.flurgle.camerakit.CameraKit.Constants.PERMISSIONS_VIDEO;

@Deprecated
@Retention(RetentionPolicy.SOURCE)
@IntDef({PERMISSIONS_VIDEO, PERMISSIONS_PICTURE})
public @interface Permissions {
}

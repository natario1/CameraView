package com.flurgle.camerakit;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.flurgle.camerakit.CameraKit.Constants.GRID_OFF;
import static com.flurgle.camerakit.CameraKit.Constants.GRID_3X3;
import static com.flurgle.camerakit.CameraKit.Constants.GRID_4X4;
import static com.flurgle.camerakit.CameraKit.Constants.GRID_PHI;

@IntDef({GRID_OFF, GRID_3X3, GRID_4X4, GRID_PHI})
@Retention(RetentionPolicy.SOURCE)
public @interface Grid {
}
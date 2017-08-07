package com.otaliastudios.cameraview;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.otaliastudios.cameraview.CameraConstants.FLASH_AUTO;
import static com.otaliastudios.cameraview.CameraConstants.FLASH_OFF;
import static com.otaliastudios.cameraview.CameraConstants.FLASH_ON;
import static com.otaliastudios.cameraview.CameraConstants.FLASH_TORCH;

@Retention(RetentionPolicy.SOURCE)
@IntDef({FLASH_OFF, FLASH_ON, FLASH_AUTO, FLASH_TORCH})
public @interface Flash {
}
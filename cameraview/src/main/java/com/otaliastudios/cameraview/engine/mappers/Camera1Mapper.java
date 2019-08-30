package com.otaliastudios.cameraview.engine.mappers;

import android.hardware.Camera;
import android.os.Build;

import com.otaliastudios.cameraview.controls.Control;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.WhiteBalance;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A Mapper maps camera engine constants to CameraView constants.
 */
public class Camera1Mapper {

    private static Camera1Mapper sInstance;

    @NonNull
    public static Camera1Mapper get() {
        if (sInstance == null) {
            sInstance = new Camera1Mapper();
        }
        return sInstance;
    }

    private static final Map<Flash, String> FLASH = new HashMap<>();
    private static final Map<WhiteBalance, String> WB = new HashMap<>();
    private static final Map<Facing, Integer> FACING = new HashMap<>();
    private static final Map<Hdr, String> HDR = new HashMap<>();

    static {
        FLASH.put(Flash.OFF, Camera.Parameters.FLASH_MODE_OFF);
        FLASH.put(Flash.ON, Camera.Parameters.FLASH_MODE_ON);
        FLASH.put(Flash.AUTO, Camera.Parameters.FLASH_MODE_AUTO);
        FLASH.put(Flash.TORCH, Camera.Parameters.FLASH_MODE_TORCH);
        FACING.put(Facing.BACK, Camera.CameraInfo.CAMERA_FACING_BACK);
        FACING.put(Facing.FRONT, Camera.CameraInfo.CAMERA_FACING_FRONT);
        WB.put(WhiteBalance.AUTO, Camera.Parameters.WHITE_BALANCE_AUTO);
        WB.put(WhiteBalance.INCANDESCENT, Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
        WB.put(WhiteBalance.FLUORESCENT, Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
        WB.put(WhiteBalance.DAYLIGHT, Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
        WB.put(WhiteBalance.CLOUDY, Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
        HDR.put(Hdr.OFF, Camera.Parameters.SCENE_MODE_AUTO);
        if (Build.VERSION.SDK_INT >= 17) {
            HDR.put(Hdr.ON, Camera.Parameters.SCENE_MODE_HDR);
        } else {
            HDR.put(Hdr.ON, "hdr");
        }
    }

    private Camera1Mapper() {}

    @NonNull
    public String mapFlash(@NonNull Flash flash) {
        //noinspection ConstantConditions
        return FLASH.get(flash);
    }

    public int mapFacing(@NonNull Facing facing) {
        //noinspection ConstantConditions
        return FACING.get(facing);
    }

    @NonNull
    public String mapWhiteBalance(@NonNull WhiteBalance whiteBalance) {
        //noinspection ConstantConditions
        return WB.get(whiteBalance);
    }

    @NonNull
    public String mapHdr(@NonNull Hdr hdr) {
        //noinspection ConstantConditions
        return HDR.get(hdr);
    }

    @Nullable
    public Flash unmapFlash(@NonNull String cameraConstant) {
        return reverseLookup(FLASH, cameraConstant);
    }

    @Nullable
    public Facing unmapFacing(int cameraConstant) {
        return reverseLookup(FACING, cameraConstant);
    }

    @Nullable
    public WhiteBalance unmapWhiteBalance(@NonNull String cameraConstant) {
        return reverseLookup(WB, cameraConstant);
    }

    @Nullable
    public Hdr unmapHdr(@NonNull String cameraConstant) {
        return reverseLookup(HDR, cameraConstant);
    }

    @Nullable
    private <C extends Control, T> C reverseLookup(@NonNull Map<C, T> map, @NonNull T object) {
        for (C value : map.keySet()) {
            if (object.equals(map.get(value))) {
                return value;
            }
        }
        return null;
    }
}

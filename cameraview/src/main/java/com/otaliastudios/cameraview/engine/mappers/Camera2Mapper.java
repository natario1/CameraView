package com.otaliastudios.cameraview.engine.mappers;

import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.controls.Control;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.WhiteBalance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Mapper maps camera engine constants to CameraView constants.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Mapper {

    private static Camera2Mapper sInstance;

    public static Camera2Mapper get() {
        if (sInstance == null) {
            sInstance = new Camera2Mapper();
        }
        return sInstance;
    }

    private static final Map<Facing, Integer> FACING = new HashMap<>();
    private static final Map<WhiteBalance, Integer> WB = new HashMap<>();
    private static final Map<Hdr, Integer> HDR = new HashMap<>();

    static {
        FACING.put(Facing.BACK, CameraCharacteristics.LENS_FACING_BACK);
        FACING.put(Facing.FRONT, CameraCharacteristics.LENS_FACING_FRONT);
        WB.put(WhiteBalance.AUTO, CameraCharacteristics.CONTROL_AWB_MODE_AUTO);
        WB.put(WhiteBalance.CLOUDY, CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
        WB.put(WhiteBalance.DAYLIGHT, CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT);
        WB.put(WhiteBalance.FLUORESCENT, CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT);
        WB.put(WhiteBalance.INCANDESCENT, CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT);
        HDR.put(Hdr.OFF, CameraCharacteristics.CONTROL_SCENE_MODE_DISABLED);
        HDR.put(Hdr.ON, 18 /* CameraCharacteristics.CONTROL_SCENE_MODE_HDR */);
    }

    private Camera2Mapper() {}

    @NonNull
    public List<Pair<Integer, Integer>> mapFlash(@NonNull Flash flash) {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        switch (flash) {
            case ON: {
                result.add(new Pair<>(
                        CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                        CameraCharacteristics.FLASH_MODE_OFF));
                break;
            }
            case AUTO: {
                result.add(new Pair<>(
                        CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH,
                        CameraCharacteristics.FLASH_MODE_OFF));
                result.add(new Pair<>(
                        CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE,
                        CameraCharacteristics.FLASH_MODE_OFF));
                break;
            }
            case OFF: {
                result.add(new Pair<>(
                        CameraCharacteristics.CONTROL_AE_MODE_ON,
                        CameraCharacteristics.FLASH_MODE_OFF));
                result.add(new Pair<>(
                        CameraCharacteristics.CONTROL_AE_MODE_OFF,
                        CameraCharacteristics.FLASH_MODE_OFF));
                break;
            }
            case TORCH: {
                // When AE_MODE is ON or OFF, we can finally use the flash mode
                // low level control to either turn flash off or open the torch
                result.add(new Pair<>(
                        CameraCharacteristics.CONTROL_AE_MODE_ON,
                        CameraCharacteristics.FLASH_MODE_TORCH));
                result.add(new Pair<>(
                        CameraCharacteristics.CONTROL_AE_MODE_OFF,
                        CameraCharacteristics.FLASH_MODE_TORCH));
                break;
            }
        }
        return  result;
    }

    public int mapFacing(@NonNull Facing facing) {
        //noinspection ConstantConditions
        return FACING.get(facing);
    }

    public int mapWhiteBalance(@NonNull WhiteBalance whiteBalance) {
        //noinspection ConstantConditions
        return WB.get(whiteBalance);
    }

    public int mapHdr(@NonNull Hdr hdr) {
        //noinspection ConstantConditions
        return HDR.get(hdr);
    }

    @NonNull
    public Set<Flash> unmapFlash(int cameraConstant) {
        Set<Flash> result = new HashSet<>();
        switch (cameraConstant) {
            case CameraCharacteristics.CONTROL_AE_MODE_OFF:
            case CameraCharacteristics.CONTROL_AE_MODE_ON: {
                result.add(Flash.OFF);
                result.add(Flash.TORCH);
                break;
            }
            case CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH: {
                result.add(Flash.ON);
                break;
            }
            case CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH:
            case CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE: {
                result.add(Flash.AUTO);
                break;
            }
            case CameraCharacteristics.CONTROL_AE_MODE_ON_EXTERNAL_FLASH:
            default: break; // we don't support external flash
        }
        return result;
    }

    @Nullable
    public Facing unmapFacing(int cameraConstant) {
        return reverseLookup(FACING, cameraConstant);
    }

    @Nullable
    public WhiteBalance unmapWhiteBalance(int cameraConstant) {
        return reverseLookup(WB, cameraConstant);
    }

    @Nullable
    public Hdr unmapHdr(int cameraConstant) {
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

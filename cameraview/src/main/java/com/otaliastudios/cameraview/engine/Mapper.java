package com.otaliastudios.cameraview.engine;

import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import com.otaliastudios.cameraview.controls.Control;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.WhiteBalance;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * A Mapper maps camera engine constants to CameraView constants.
 */
public abstract class Mapper {

    private static Mapper CAMERA1;
    private static Mapper CAMERA2;

    public static Mapper get(@NonNull Engine engine) {
        if (engine == Engine.CAMERA1) {
            if (CAMERA1 == null) CAMERA1 = new Camera1Mapper();
            return CAMERA1;
        } else if (engine == Engine.CAMERA2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (CAMERA2 == null) CAMERA2 = new Camera2Mapper();
            return CAMERA2;
        } else {
            throw new IllegalArgumentException("Unknown engine or unsupported API level.");
        }
    }

    private Mapper() {}

    public abstract <T> T map(Flash flash);

    public abstract <T> T map(Facing facing);

    public abstract <T> T map(WhiteBalance whiteBalance);

    public abstract <T> T map(Hdr hdr);

    public abstract <T> Flash unmapFlash(T cameraConstant);

    public abstract <T> Facing unmapFacing(T cameraConstant);

    public abstract <T> WhiteBalance unmapWhiteBalance(T cameraConstant);

    public abstract <T> Hdr unmapHdr(T cameraConstant);

    @SuppressWarnings("WeakerAccess")
    protected <C extends Control, T> C reverseLookup(HashMap<C, T> map, T object) {
        for (C value : map.keySet()) {
            if (object.equals(map.get(value))) {
                return value;
            }
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    protected <C extends Control, T> C reverseListLookup(HashMap<C, List<T>> map, T object) {
        for (C value : map.keySet()) {
            List<T> list = map.get(value);
            if (list == null) continue;
            for (T candidate : list) {
                if (object.equals(candidate)) {
                    return value;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static class Camera1Mapper extends Mapper {

        private static final HashMap<Flash, String> FLASH = new HashMap<>();
        private static final HashMap<WhiteBalance, String> WB = new HashMap<>();
        private static final HashMap<Facing, Integer> FACING = new HashMap<>();
        private static final HashMap<Hdr, String> HDR = new HashMap<>();

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

        @Override
        public <T> T map(Flash flash) {
            return (T) FLASH.get(flash);
        }

        @Override
        public <T> T map(Facing facing) {
            return (T) FACING.get(facing);
        }

        @Override
        public <T> T map(WhiteBalance whiteBalance) {
            return (T) WB.get(whiteBalance);
        }

        @Override
        public <T> T map(Hdr hdr) {
            return (T) HDR.get(hdr);
        }

        @Override
        public <T> Flash unmapFlash(T cameraConstant) {
            return reverseLookup(FLASH, (String) cameraConstant);
        }

        @Override
        public <T> Facing unmapFacing(T cameraConstant) {
            return reverseLookup(FACING, (Integer) cameraConstant);
        }

        @Override
        public <T> WhiteBalance unmapWhiteBalance(T cameraConstant) {
            return reverseLookup(WB, (String) cameraConstant);
        }

        @Override
        public <T> Hdr unmapHdr(T cameraConstant) {
            return reverseLookup(HDR, (String) cameraConstant);
        }
    }

    @SuppressWarnings("unchecked")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static class Camera2Mapper extends Mapper {

        private static final HashMap<Flash, List<Integer>> FLASH = new HashMap<>();
        private static final HashMap<Facing, Integer> FACING = new HashMap<>();
        private static final HashMap<WhiteBalance, Integer> WB = new HashMap<>();
        private static final HashMap<Hdr, Integer> HDR = new HashMap<>();

        static {
            // OFF and TORCH have also a second condition - to CameraCharacteristics.CONTROL_FLASH_MODE - but that does not
            // fit into the Mapper interface. TODO review this
            FLASH.put(Flash.OFF, Arrays.asList(CameraCharacteristics.CONTROL_AE_MODE_ON, CameraCharacteristics.CONTROL_AE_MODE_OFF));
            FLASH.put(Flash.TORCH, Arrays.asList(CameraCharacteristics.CONTROL_AE_MODE_ON, CameraCharacteristics.CONTROL_AE_MODE_OFF));
            FLASH.put(Flash.AUTO, Arrays.asList(CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH, CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE));
            FLASH.put(Flash.ON, Collections.singletonList(CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH));
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

        @Override
        public <T> T map(Flash flash) {
            return (T) FLASH.get(flash);
        }

        @Override
        public <T> T map(Facing facing) {
            return (T) FACING.get(facing);
        }

        @Override
        public <T> T map(WhiteBalance whiteBalance) {
            return (T) WB.get(whiteBalance);
        }

        @Override
        public <T> T map(Hdr hdr) {
            return (T) HDR.get(hdr);
        }

        @Override
        public <T> Flash unmapFlash(T cameraConstant) {
            return reverseListLookup(FLASH, (Integer) cameraConstant);
        }

        @Override
        public <T> Facing unmapFacing(T cameraConstant) {
            return reverseLookup(FACING, (Integer) cameraConstant);
        }

        @Override
        public <T> WhiteBalance unmapWhiteBalance(T cameraConstant) {
            return reverseLookup(WB, (Integer) cameraConstant);
        }

        @Override
        public <T> Hdr unmapHdr(T cameraConstant) {
            return reverseLookup(HDR, (Integer) cameraConstant);
        }
    }
}

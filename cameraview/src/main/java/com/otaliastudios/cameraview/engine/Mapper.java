package com.otaliastudios.cameraview.engine;

import android.hardware.Camera;
import android.os.Build;

import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.WhiteBalance;

import java.util.HashMap;

/**
 * A Mapper maps camera engine constants to CameraView constants.
 */
public abstract class Mapper {

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
    protected <T> T reverseLookup(HashMap<T, ?> map, Object object) {
        for (T value : map.keySet()) {
            if (object.equals(map.get(value))) {
                return value;
            }
        }
        return null;
    }

    private static Mapper CAMERA1;

    public static Mapper get() {
        if (CAMERA1 == null) {
            CAMERA1 = new Camera1Mapper();
        }
        return CAMERA1;
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
            return reverseLookup(FLASH, cameraConstant);
        }

        @Override
        public <T> Facing unmapFacing(T cameraConstant) {
            return reverseLookup(FACING, cameraConstant);
        }

        @Override
        public <T> WhiteBalance unmapWhiteBalance(T cameraConstant) {
            return reverseLookup(WB, cameraConstant);
        }

        @Override
        public <T> Hdr unmapHdr(T cameraConstant) {
            return reverseLookup(HDR, cameraConstant);
        }
    }
}

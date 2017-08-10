package com.otaliastudios.cameraview;

import android.hardware.Camera;
import android.util.SparseArray;

import java.util.HashMap;

abstract class Mapper {

    abstract <T> T mapFlash(@Flash int internalConstant);
    abstract <T> T mapFacing(@Facing int internalConstant);
    abstract <T> T mapWhiteBalance(@WhiteBalance int internalConstant);
    abstract <T> T mapFocus(@Focus int internalConstant);
    @Flash abstract <T> Integer unmapFlash(T cameraConstant);
    @Facing abstract <T> Integer unmapFacing(T cameraConstant);
    @WhiteBalance abstract <T> Integer unmapWhiteBalance(T cameraConstant);
    @Focus abstract <T> Integer unmapFocus(T cameraConstant);

    static class Mapper1 extends Mapper {
        private static final HashMap<Integer, String> FLASH = new HashMap<>();
        private static final HashMap<Integer, String> WB = new HashMap<>();
        private static final HashMap<Integer, Integer> FACING = new HashMap<>();
        private static final HashMap<Integer, String> FOCUS = new HashMap<>();

        static {
            FLASH.put(CameraConstants.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF);
            FLASH.put(CameraConstants.FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
            FLASH.put(CameraConstants.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
            FLASH.put(CameraConstants.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
            FACING.put(CameraConstants.FACING_BACK, Camera.CameraInfo.CAMERA_FACING_BACK);
            FACING.put(CameraConstants.FACING_FRONT, Camera.CameraInfo.CAMERA_FACING_FRONT);
            WB.put(CameraConstants.WHITE_BALANCE_AUTO, Camera.Parameters.WHITE_BALANCE_AUTO);
            WB.put(CameraConstants.WHITE_BALANCE_INCANDESCENT, Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
            WB.put(CameraConstants.WHITE_BALANCE_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
            WB.put(CameraConstants.WHITE_BALANCE_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
            WB.put(CameraConstants.WHITE_BALANCE_CLOUDY, Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
            // TODO FOCUS_MODE_FIXED is rarely supported.
            FOCUS.put(CameraConstants.FOCUS_FIXED, Camera.Parameters.FOCUS_MODE_FIXED);
            FOCUS.put(CameraConstants.FOCUS_CONTINUOUS, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            FOCUS.put(CameraConstants.FOCUS_TAP, Camera.Parameters.FOCUS_MODE_AUTO);
            FOCUS.put(CameraConstants.FOCUS_TAP_WITH_MARKER, Camera.Parameters.FOCUS_MODE_AUTO);
        }

        @Override
        <T> T mapFlash(int internalConstant) {
            return (T) FLASH.get(internalConstant);
        }

        @Override
        <T> T mapFacing(int internalConstant) {
            return (T) FACING.get(internalConstant);
        }

        @Override
        <T> T mapWhiteBalance(int internalConstant) {
            return (T) WB.get(internalConstant);
        }

        @Override
        <T> T mapFocus(@Focus int internalConstant) {
            return (T) FOCUS.get(internalConstant);
        }

        private Integer reverseLookup(HashMap<Integer, ?> map, Object object) {
            for (int value : map.keySet()) {
                if (map.get(value).equals(object)) {
                    return value;
                }
            }
            return null;
        }

        @Override
        <T> Integer unmapFlash(T cameraConstant) {
            return reverseLookup(FLASH, cameraConstant);
        }

        @Override
        <T> Integer unmapFacing(T cameraConstant) {
            return reverseLookup(FACING, cameraConstant);
        }

        @Override
        <T> Integer unmapWhiteBalance(T cameraConstant) {
            return reverseLookup(WB, cameraConstant);
        }

        // This will ignore FOCUS_TAP_WITH_MARKER but it's fine
        @Override
        <T> Integer unmapFocus(T cameraConstant) {
            return reverseLookup(FOCUS, cameraConstant);
        }
    }

    static class Mapper2 extends Mapper {

        @Override
        <T> T mapWhiteBalance(@WhiteBalance int internalConstant) {
            return null;
        }

        @Override
        <T> T mapFacing(@Facing int internalConstant) {
            return null;
        }

        @Override
        <T> T mapFlash(@Flash int internalConstant) {
            return null;
        }

        @Override
        <T> Integer unmapFlash(T cameraConstant) {
            return 0;
        }

        @Override
        <T> Integer unmapFacing(T cameraConstant) {
            return 0;
        }

        @Override
        <T> Integer unmapWhiteBalance(T cameraConstant) {
            return 0;
        }

        @Override
        <T> T mapFocus(@Focus int internalConstant) {
            return null;
        }

        @Override
        <T> Integer unmapFocus(T cameraConstant) {
            return 0;
        }
    }

}

package com.otaliastudios.cameraview;

import android.hardware.Camera;

import java.util.HashMap;

abstract class Mapper {

    abstract <T> T mapFlash(Flash internalConstant);
    abstract <T> T mapFacing(Facing internalConstant);
    abstract <T> T mapWhiteBalance(WhiteBalance internalConstant);
    abstract <T> Flash unmapFlash(T cameraConstant);
    abstract <T> Facing unmapFacing(T cameraConstant);
    abstract <T> WhiteBalance unmapWhiteBalance(T cameraConstant);

    static class Mapper1 extends Mapper {

        private static final HashMap<Flash, String> FLASH = new HashMap<>();
        private static final HashMap<WhiteBalance, String> WB = new HashMap<>();
        private static final HashMap<Facing, Integer> FACING = new HashMap<>();

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
        }

        @Override
        <T> T mapFlash(Flash internalConstant) {
            return (T) FLASH.get(internalConstant);
        }

        @Override
        <T> T mapFacing(Facing internalConstant) {
            return (T) FACING.get(internalConstant);
        }

        @Override
        <T> T mapWhiteBalance(WhiteBalance internalConstant) {
            return (T) WB.get(internalConstant);
        }

        private <T> T reverseLookup(HashMap<T, ?> map, Object object) {
            for (T value : map.keySet()) {
                if (map.get(value).equals(object)) {
                    return value;
                }
            }
            return null;
        }

        @Override
        <T> Flash unmapFlash(T cameraConstant) {
            return reverseLookup(FLASH, cameraConstant);
        }

        @Override
        <T> Facing unmapFacing(T cameraConstant) {
            return reverseLookup(FACING, cameraConstant);
        }

        @Override
        <T> WhiteBalance unmapWhiteBalance(T cameraConstant) {
            return reverseLookup(WB, cameraConstant);
        }
    }

    static class Mapper2 extends Mapper {

        @Override
        <T> T mapWhiteBalance(WhiteBalance internalConstant) {
            return null;
        }

        @Override
        <T> T mapFlash(Flash internalConstant) {
            return null;
        }

        @Override
        <T> Flash unmapFlash(T cameraConstant) {
            return null;
        }

        @Override
        <T> WhiteBalance unmapWhiteBalance(T cameraConstant) {
            return null;
        }

        @Override
        <T> T mapFacing(Facing internalConstant) {
            return null;
        }

        @Override
        <T> Facing unmapFacing(T cameraConstant) {
            return null;
        }
    }

}

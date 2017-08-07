package com.flurgle.camerakit;

import android.hardware.Camera;
import android.util.SparseArray;

abstract class Mapper {

    abstract <T> T mapFlash(@Flash int internalConstant);
    abstract <T> T mapFacing(@Facing int internalConstant);
    abstract <T> T mapWhiteBalance(@WhiteBalance int internalConstant);
    abstract <T> T mapFocus(@Focus int internalConstant);
    @Flash abstract <T> int unmapFlash(T cameraConstant);
    @Facing abstract <T> int unmapFacing(T cameraConstant);
    @WhiteBalance abstract <T> int unmapWhiteBalance(T cameraConstant);
    @Focus abstract <T> int unmapFocus(T cameraConstant);

    static class Mapper1 extends Mapper {
        private static final SparseArray<String> FLASH = new SparseArray<>();
        private static final SparseArray<String> WB = new SparseArray<>();
        private static final SparseArray<Integer> FACING = new SparseArray<>();
        private static final SparseArray<String> FOCUS = new SparseArray<>();

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
            FOCUS.put(CameraConstants.FOCUS_FIXED, Camera.Parameters.FOCUS_MODE_FIXED);
            FOCUS.put(CameraConstants.FOCUS_CONTINUOUS, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            FOCUS.put(CameraConstants.FOCUS_TAP, Camera.Parameters.FOCUS_MODE_AUTO);
            FOCUS.put(CameraConstants.FOCUS_TAP_WITH_MARKER, Camera.Parameters.FOCUS_MODE_AUTO);
        }

        @Override
        <T> T mapFlash(int internalConstant) {
            return (T) FLASH.get(internalConstant, null);
        }

        @Override
        <T> T mapFacing(int internalConstant) {
            return (T) FACING.get(internalConstant, null);
        }

        @Override
        <T> T mapWhiteBalance(int internalConstant) {
            return (T) WB.get(internalConstant, null);
        }

        @Override
        <T> T mapFocus(@Focus int internalConstant) {
            return (T) FOCUS.get(internalConstant, null);
        }

        @Override
        <T> int unmapFlash(T cameraConstant) {
            return FLASH.keyAt(FLASH.indexOfValue((String) cameraConstant));
        }

        @Override
        <T> int unmapFacing(T cameraConstant) {
            return FACING.keyAt(FACING.indexOfValue((Integer) cameraConstant));
        }

        @Override
        <T> int unmapWhiteBalance(T cameraConstant) {
            return WB.keyAt(WB.indexOfValue((String) cameraConstant));
        }

        // This will ignore FOCUS_TAP_WITH_MARKER but it's fine
        @Override
        <T> int unmapFocus(T cameraConstant) {
            return FOCUS.keyAt(FOCUS.indexOfValue((String) cameraConstant));
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
        <T> int unmapFlash(T cameraConstant) {
            return 0;
        }

        @Override
        <T> int unmapFacing(T cameraConstant) {
            return 0;
        }

        @Override
        <T> int unmapWhiteBalance(T cameraConstant) {
            return 0;
        }

        @Override
        <T> T mapFocus(@Focus int internalConstant) {
            return null;
        }

        @Override
        <T> int unmapFocus(T cameraConstant) {
            return 0;
        }
    }

}

package com.otaliastudios.cameraview.tools;

import android.os.Build;

public class Emulator {
    public static boolean isEmulator() {
        // From Android's RequiresDeviceFilter
        return Build.HARDWARE.equals("goldfish")
                || Build.HARDWARE.equals("ranchu")
                || Build.HARDWARE.equals("gce_x86");
    }
}

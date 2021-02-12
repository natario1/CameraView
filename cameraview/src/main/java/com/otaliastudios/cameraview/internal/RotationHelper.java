package com.otaliastudios.cameraview.internal;

import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;

/**
 * This will only be used on low APIs or when GL surface is not available.
 * This risks OOMs and was never a good tool.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public class RotationHelper {

    /**
     * Rotates the given yuv image into another yuv array, by the given angle.
     * @param yuv image
     * @param size image size
     * @param rotation desired angle
     * @return a new yuv array
     */
    public static byte[] rotate(@NonNull final byte[] yuv,
                                @NonNull final Size size,
                                final int rotation) {
        if (rotation == 0) return yuv;
        if (rotation % 90 != 0 || rotation < 0 || rotation > 270) {
            throw new IllegalArgumentException("0 <= rotation < 360, rotation % 90 == 0");
        }
        final int width = size.getWidth();
        final int height = size.getHeight();
        final byte[] output = new byte[yuv.length];
        final int frameSize = width * height;
        final boolean swap = rotation % 180 != 0;
        final boolean xflip = rotation % 270 != 0;
        final boolean yflip = rotation >= 180;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                final int yIn = j * width + i;
                final int uIn = frameSize + (j >> 1) * width + (i & ~1);
                final int vIn = uIn + 1;

                final int wOut = swap ? height : width;
                final int hOut = swap ? width : height;
                final int iSwapped = swap ? j : i;
                final int jSwapped = swap ? i : j;
                final int iOut = xflip ? wOut - iSwapped - 1 : iSwapped;
                final int jOut = yflip ? hOut - jSwapped - 1 : jSwapped;

                final int yOut = jOut * wOut + iOut;
                final int uOut = frameSize + (jOut >> 1) * wOut + (iOut & ~1);
                final int vOut = uOut + 1;

                output[yOut] = (byte) (0xff & yuv[yIn]);
                output[uOut] = (byte) (0xff & yuv[uIn]);
                output[vOut] = (byte) (0xff & yuv[vIn]);
            }
        }

        return output;
    }
}


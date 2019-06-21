package com.otaliastudios.cameraview.internal.utils;

import android.graphics.Rect;

import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;

/**
 * Simply computes the crop between a full size and a desired aspect ratio.
 */
public class CropHelper {

    // It's important that size and aspect ratio belong to the same reference.
    @NonNull
    public static Rect computeCrop(@NonNull Size currentSize, @NonNull AspectRatio targetRatio) {
        int currentWidth = currentSize.getWidth();
        int currentHeight = currentSize.getHeight();
        if (targetRatio.matches(currentSize)) {
            return new Rect(0, 0, currentWidth, currentHeight);
        }

        // They are not equal. Compute.
        AspectRatio currentRatio = AspectRatio.of(currentWidth, currentHeight);
        int x, y, width, height;
        if (currentRatio.toFloat() > targetRatio.toFloat()) {
            height = currentHeight;
            width = (int) (height * targetRatio.toFloat());
            y = 0;
            x = (currentWidth - width) / 2;
        } else {
            width = currentWidth;
            height = (int) (width / targetRatio.toFloat());
            y = (currentHeight - height) / 2;
            x = 0;
        }
        return new Rect(x, y, x + width, y + height);
    }
}


package com.otaliastudios.cameraview.internal.utils;

import android.graphics.ImageFormat;
import android.media.Image;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Conversions for {@link android.media.Image}s into byte arrays.
 */
@RequiresApi(19)
public class ImageHelper {

    /**
     * From https://stackoverflow.com/a/52740776/4288782 .
     * The result array should have a size that is at least 3/2 * w * h.
     * This is correctly computed by {@link com.otaliastudios.cameraview.frame.FrameManager}.
     *
     * @param image input image
     * @param result output array
     */
    public static void convertToNV21(@NonNull Image image, @NonNull byte[] result) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalStateException("CAn only convert from YUV_420_888.");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 4;

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();

        if (image.getPlanes()[0].getPixelStride() != 1) {
            throw new AssertionError("Something wrong in convertToNV21");
        }

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(result, 0, ySize);
            pos += ySize;
        }
        else {
            int yBufferPos = width - rowStride; // not an actual position
            for (; pos<ySize; pos+=width) {
                yBufferPos += rowStride - width;
                yBuffer.position(yBufferPos);
                yBuffer.get(result, pos, width);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        if (rowStride != image.getPlanes()[1].getRowStride()) {
            throw new AssertionError("Something wrong in convertToNV21");
        }
        if (pixelStride != image.getPlanes()[1].getPixelStride()) {
            throw new AssertionError("Something wrong in convertToNV21");
        }

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1]
            // is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            vBuffer.put(1, (byte)0);
            if (uBuffer.get(0) == 0) {
                vBuffer.put(1, (byte)255);
                //noinspection ConstantConditions
                if (uBuffer.get(0) == 255) {
                    vBuffer.put(1, savePixel);
                    vBuffer.get(result, ySize, uvSize);
                    return; // shortcut
                }
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row=0; row<height/2; row++) {
            for (int col=0; col<width/2; col++) {
                int vuPos = col*pixelStride + row*rowStride;
                result[pos++] = vBuffer.get(vuPos);
                result[pos++] = uBuffer.get(vuPos);
            }
        }
    }
}


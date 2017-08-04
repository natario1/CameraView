package com.flurgle.camerakit;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CropHelper {


    // TODO test this. How is YuvImage? Does it come already well rotated?
    public static byte[] cropToJpeg(YuvImage yuv, AspectRatio targetRatio, int jpegCompression) {
        Rect crop = computeCrop(yuv.getWidth(), yuv.getHeight(), targetRatio);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(crop, jpegCompression, out);
        return out.toByteArray();
    }


    // This reads a rotated Bitmap thanks to CameraUtils. Then crops and returns a byte array.
    // In doing so, EXIF data is deleted.
    public static byte[] cropToJpeg(byte[] jpeg, AspectRatio targetRatio, int jpegCompression) {
        Bitmap image = CameraUtils.decodeBitmap(jpeg);
        Log.e("CropHelper", "decoded image has width="+image.getWidth()+", height="+image.getHeight());
        Rect cropRect = computeCrop(image.getWidth(), image.getHeight(), targetRatio);
        Bitmap crop = Bitmap.createBitmap(image, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
        image.recycle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        crop.compress(Bitmap.CompressFormat.JPEG, jpegCompression, out);
        return out.toByteArray();
    }

    private static Rect computeCrop(int currentWidth, int currentHeight, AspectRatio targetRatio) {
        AspectRatio currentRatio = AspectRatio.of(currentWidth, currentHeight);
        int x, y, width, height;
        if (currentRatio.toFloat() > targetRatio.toFloat()) {
            height = currentHeight;
            width = (int) (height * targetRatio.toFloat());
            y = 0;
            x = (currentWidth - width) / 2;
        } else {
            width = currentWidth;
            height = (int) (width * targetRatio.inverse().toFloat());
            y = (currentHeight - height) / 2;
            x = 0;
        }
        return new Rect(x, y, x+width, y+height);
    }
}

package com.flurgle.camerakit.demo;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.flurgle.camerakit.Size;

import java.lang.ref.WeakReference;

public class ResultHolder {

    private static WeakReference<Bitmap> image;
    private static Size nativeCaptureSize;
    private static long timeToCallback;
    private static Uri video;


    public static Uri getVideo() {
        return video;
    }

    public static void setVideo(Uri video) {
        ResultHolder.video = video;
    }

    public static void setImage(@Nullable Bitmap image) {
        ResultHolder.image = image != null ? new WeakReference<>(image) : null;
    }

    @Nullable
    public static Bitmap getImage() {
        return image != null ? image.get() : null;
    }

    public static void setTimeToCallback(long timeToCallback) {
        ResultHolder.timeToCallback = timeToCallback;
    }

    public static long getTimeToCallback() {
        return timeToCallback;
    }

    public static void dispose() {
        setImage(null);
        setVideo(null);
        setTimeToCallback(0);
    }

}

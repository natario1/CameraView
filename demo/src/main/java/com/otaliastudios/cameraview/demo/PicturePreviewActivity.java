package com.otaliastudios.cameraview.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.Nullable;

import android.util.Log;
import android.widget.ImageView;

import com.otaliastudios.cameraview.AspectRatio;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.PictureResult;

import java.lang.ref.WeakReference;


public class PicturePreviewActivity extends Activity {

    private static WeakReference<PictureResult> image;

    public static void setPictureResult(@Nullable PictureResult im) {
        image = im != null ? new WeakReference<>(im) : null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        final ImageView imageView = findViewById(R.id.image);
        final MessageView captureResolution = findViewById(R.id.nativeCaptureResolution);
        final MessageView captureLatency = findViewById(R.id.captureLatency);
        final MessageView exifRotation = findViewById(R.id.exifRotation);
        PictureResult result = image == null ? null : image.get();
        if (result == null) {
            finish();
            return;
        }
        final long delay = getIntent().getLongExtra("delay", 0);
        AspectRatio ratio = AspectRatio.of(result.getSize());
        captureLatency.setTitleAndMessage("Approx. latency", delay + " milliseconds");
        captureResolution.setTitleAndMessage("Resolution", result.getSize() + " (" + ratio + ")");
        exifRotation.setTitleAndMessage("EXIF rotation", result.getRotation() + "");
        result.toBitmap(1000, 1000, new BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
            }
        });

        if (result.isSnapshot()) {
            // Log the real size for debugging reason.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(result.getData(), 0, result.getData().length, options);
            if (result.getRotation() % 180 != 0) {
                Log.e("PicturePreview", "The picture full size is " + result.getSize().getHeight() + "x" + result.getSize().getWidth());
            } else {
                Log.e("PicturePreview", "The picture full size is " + result.getSize().getWidth() + "x" + result.getSize().getHeight());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            setPictureResult(null);
        }
    }
}

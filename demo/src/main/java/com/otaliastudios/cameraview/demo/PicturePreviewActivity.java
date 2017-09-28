package com.otaliastudios.cameraview.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.TextView;

import com.otaliastudios.cameraview.AspectRatio;
import com.otaliastudios.cameraview.CameraUtils;

import java.lang.ref.WeakReference;


public class PicturePreviewActivity extends Activity {

    ImageView imageView;
    TextView nativeCaptureResolution;
    TextView actualResolution;
    TextView approxUncompressedSize;
    TextView captureLatency;

    private static WeakReference<byte[]> image;

    public static void setImage(@Nullable byte[] im) {
        image = im != null ? new WeakReference<>(im) : null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        imageView = findViewById(R.id.image);
        nativeCaptureResolution = findViewById(R.id.nativeCaptureResolution);
        actualResolution = findViewById(R.id.actualResolution);
        approxUncompressedSize = findViewById(R.id.approxUncompressedSize);
        captureLatency = findViewById(R.id.captureLatency);

        final long delay = getIntent().getLongExtra("delay", 0);
        final int nativeWidth = getIntent().getIntExtra("nativeWidth", 0);
        final int nativeHeight = getIntent().getIntExtra("nativeHeight", 0);
        byte[] b = image == null ? null : image.get();
        if (b == null) {
            finish();
            return;
        }

        CameraUtils.decodeBitmap(b, new CameraUtils.BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
                approxUncompressedSize.setText(getApproximateFileMegabytes(bitmap) + "MB");
                captureLatency.setText(delay + " milliseconds");

                // ncr and ar might be different when cropOutput is true.
                AspectRatio nativeRatio = AspectRatio.of(nativeWidth, nativeHeight);
                AspectRatio finalRatio = AspectRatio.of(bitmap.getWidth(), bitmap.getHeight());
                nativeCaptureResolution.setText(nativeWidth + "x" + nativeHeight + " (" + nativeRatio + ")");
                actualResolution.setText(bitmap.getWidth() + "x" + bitmap.getHeight() + " (" + finalRatio + ")");
            }
        });

    }

    private static float getApproximateFileMegabytes(Bitmap bitmap) {
        return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024 / 1024;
    }

}

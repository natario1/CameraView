package com.flurgle.camerakit.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.TextView;

import com.flurgle.camerakit.AspectRatio;
import com.flurgle.camerakit.CameraUtils;
import com.flurgle.camerakit.Size;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PicturePreviewActivity extends Activity {

    @BindView(R.id.image)
    ImageView imageView;

    @BindView(R.id.nativeCaptureResolution)
    TextView nativeCaptureResolution;

    @BindView(R.id.actualResolution)
    TextView actualResolution;

    @BindView(R.id.approxUncompressedSize)
    TextView approxUncompressedSize;

    @BindView(R.id.captureLatency)
    TextView captureLatency;

    private static WeakReference<byte[]> image;

    public static void setImage(@Nullable byte[] im) {
        image = im != null ? new WeakReference<>(im) : null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        ButterKnife.bind(this);

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

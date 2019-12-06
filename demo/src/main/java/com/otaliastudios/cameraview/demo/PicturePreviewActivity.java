package com.otaliastudios.cameraview.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.PictureResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;


public class PicturePreviewActivity extends AppCompatActivity {

    private static PictureResult picture;

    public static void setPictureResult(@Nullable PictureResult pictureResult) {
        picture = pictureResult;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        final PictureResult result = picture;
        if (result == null) {
            finish();
            return;
        }

        final ImageView imageView = findViewById(R.id.image);
        final MessageView captureResolution = findViewById(R.id.nativeCaptureResolution);
        final MessageView captureLatency = findViewById(R.id.captureLatency);
        final MessageView exifRotation = findViewById(R.id.exifRotation);

        final long delay = getIntent().getLongExtra("delay", 0);
        AspectRatio ratio = AspectRatio.of(result.getSize());
        captureLatency.setTitleAndMessage("Approx. latency", delay + " milliseconds");
        captureResolution.setTitleAndMessage("Resolution", result.getSize() + " (" + ratio + ")");
        exifRotation.setTitleAndMessage("EXIF rotation", result.getRotation() + "");
        try {
            result.toBitmap(1000, 1000, new BitmapCallback() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        } catch (UnsupportedOperationException e) {
            imageView.setImageDrawable(new ColorDrawable(Color.GREEN));
            Toast.makeText(this, "Can't preview this format: " + picture.getFormat(),
                    Toast.LENGTH_LONG).show();
        }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.share) {
            Toast.makeText(this, "Sharing...", Toast.LENGTH_SHORT).show();
            String extension;
            switch (picture.getFormat()) {
                case JPEG: extension = "jpg"; break;
                case DNG: extension = "dng"; break;
                default: throw new RuntimeException("Unknown format.");
            }
            File file = new File(getFilesDir(), "picture." + extension);
            CameraUtils.writeToFile(picture.getData(), file, new FileCallback() {
                @Override
                public void onFileReady(@Nullable File file) {
                    if (file != null) {
                        Context context = PicturePreviewActivity.this;
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("image/*");
                        Uri uri = FileProvider.getUriForFile(context,
                                context.getPackageName() + ".provider",
                                file);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } else {
                        Toast.makeText(PicturePreviewActivity.this,
                                "Error while writing file.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

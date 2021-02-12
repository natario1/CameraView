package com.otaliastudios.cameraview;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.otaliastudios.cameraview.tools.Op;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraUtilsTest extends BaseTest {

    @Test
    public void testHasCameras() {
        Context context = mock(Context.class);
        PackageManager pm = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(pm);
        when(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)).thenReturn(true);
        when(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)).thenReturn(true);
        assertTrue(CameraUtils.hasCameras(context));
        when(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)).thenReturn(false);
        when(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)).thenReturn(true);
        assertTrue(CameraUtils.hasCameras(context));
        when(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)).thenReturn(false);
        when(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)).thenReturn(false);
        assertFalse(CameraUtils.hasCameras(context));
    }

    @NonNull
    private Op<String> writeAndReadString(@NonNull String data) {
        final File file = new File(getContext().getFilesDir(), "string.txt");
        final byte[] bytes = data.getBytes(Charset.forName("UTF-8"));
        final Op<String> result = new Op<>();
        final FileCallback callback = new FileCallback() {
            @Override
            public void onFileReady(@Nullable File file) {
                if (file == null) {
                    result.controller().end(null);
                } else {
                    // Read back the file.
                    try {
                        FileInputStream stream = new FileInputStream(file);
                        byte[] bytes = new byte[stream.available()];
                        stream.read(bytes);
                        result.controller().end(new String(bytes, Charset.forName("UTF-8")));
                    } catch (IOException e) {
                        result.controller().end(null);
                    }
                }
            }
        };
        uiSync(new Runnable() {
            @Override
            public void run() {
                CameraUtils.writeToFile(bytes, file, callback);
            }
        });
        return result;
    }

    @Test
    public void testWriteToFile() {
        Op<String> op = writeAndReadString("testString");
        String result = op.await(2000);
        assertEquals("testString", result);
    }


    // Encodes bitmap and decodes again using our utility.
    private Op<Bitmap> encodeDecodeTask(@NonNull Bitmap source, final int maxWidth, final int maxHeight, boolean async) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        // Using lossy JPG we can't have strict comparison of values after compression.
        source.compress(Bitmap.CompressFormat.PNG, 100, os);
        final byte[] data = os.toByteArray();

        final Op<Bitmap> decode = new Op<>();
        if (async) {
            final BitmapCallback callback = new BitmapCallback() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    decode.controller().end(bitmap);
                }
            };

            // Run on ui because it involves handlers.
            uiSync(new Runnable() {
                @Override
                public void run() {
                    if (maxWidth > 0 && maxHeight > 0) {
                        CameraUtils.decodeBitmap(data, maxWidth, maxHeight, callback);
                    } else {
                        CameraUtils.decodeBitmap(data, callback);
                    }
                }
            });
        } else {
            Bitmap result;
            if (maxWidth > 0 && maxHeight > 0) {
                result = CameraUtils.decodeBitmap(data, maxWidth, maxHeight);
            } else {
                result = CameraUtils.decodeBitmap(data);
            }
            decode.controller().end(result);
        }
        return decode;
    }

    @Test
    public void testDecodeBitmap() {
        int w = 100, h = 200, color = Color.WHITE;
        Bitmap source = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        source.setPixel(0, 0, color);

        Op<Bitmap> decode = encodeDecodeTask(source, 0, 0, true);
        Bitmap other = decode.await(800);
        assertNotNull(other);
        assertEquals(100, w);
        assertEquals(200, h);
        assertEquals(color, other.getPixel(0, 0));
        assertEquals(0, other.getPixel(0, h-1));
        assertEquals(0, other.getPixel(w-1, 0));
        assertEquals(0, other.getPixel(w-1, h-1));
    }

    @Test
    public void testDecodeBitmapSync() {
        int w = 100, h = 200, color = Color.WHITE;
        Bitmap source = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        source.setPixel(0, 0, color);

        Op<Bitmap> decode = encodeDecodeTask(source, 0, 0, false);
        Bitmap other = decode.await(800);
        assertNotNull(other);
        assertEquals(100, w);
        assertEquals(200, h);
        assertEquals(color, other.getPixel(0, 0));
        assertEquals(0, other.getPixel(0, h-1));
        assertEquals(0, other.getPixel(w-1, 0));
        assertEquals(0, other.getPixel(w-1, h-1));
    }


    @Test
    public void testDecodeDownscaledBitmap() {
        int width = 1000, height = 2000;
        Bitmap source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Op<Bitmap> op;
        Bitmap other;

        op = encodeDecodeTask(source, 100, 100, true);
        other = op.await(800);
        assertNotNull(other);
        assertTrue(other.getWidth() <= 100);
        assertTrue(other.getHeight() <= 100);

        op = encodeDecodeTask(source, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        other = op.await(800);
        assertNotNull(other);
        assertEquals(other.getWidth(), width);
        assertEquals(other.getHeight(), height);

        op = encodeDecodeTask(source, 6000, 6000, true);
        other = op.await(800);
        assertNotNull(other);
        assertEquals(other.getWidth(), width);
        assertEquals(other.getHeight(), height);
    }
}

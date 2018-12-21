package com.otaliastudios.cameraview;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;

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
        when(pm.hasSystemFeature(anyString())).thenReturn(true);
        assertTrue(CameraUtils.hasCameras(context));

        when(pm.hasSystemFeature(anyString())).thenReturn(false);
        assertFalse(CameraUtils.hasCameras(context));
    }

    // Encodes bitmap and decodes again using our utility.
    private Task<Bitmap> encodeDecodeTask(Bitmap source) {
        return encodeDecodeTask(source, 0, 0);
    }

    // Encodes bitmap and decodes again using our utility.
    private Task<Bitmap> encodeDecodeTask(Bitmap source, final int maxWidth, final int maxHeight) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        // Using lossy JPG we can't have strict comparison of values after compression.
        source.compress(Bitmap.CompressFormat.PNG, 100, os);
        final byte[] data = os.toByteArray();

        final Task<Bitmap> decode = new Task<>(true);
        final BitmapCallback callback = new BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                decode.end(bitmap);
            }
        };

        // Run on ui because it involves handlers.
        ui(new Runnable() {
            @Override
            public void run() {
                if (maxWidth > 0 && maxHeight > 0) {
                    CameraUtils.decodeBitmap(data, maxWidth, maxHeight, callback);
                } else {
                    CameraUtils.decodeBitmap(data, callback);
                }
            }
        });
        return decode;
    }

    @Test
    public void testDecodeBitmap() {
        int w = 100, h = 200, color = Color.WHITE;
        Bitmap source = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        source.setPixel(0, 0, color);

        Task<Bitmap> decode = encodeDecodeTask(source);
        Bitmap other = decode.await(800);
        assertNotNull(other);
        assertEquals(100, w);
        assertEquals(200, h);
        assertEquals(color, other.getPixel(0, 0));
        assertEquals(0, other.getPixel(0, h-1));
        assertEquals(0, other.getPixel(w-1, 0));
        assertEquals(0, other.getPixel(w-1, h-1));

        // TODO: improve when we add EXIF writing to byte arrays
    }


    @Test
    public void testDecodeDownscaledBitmap() {
        int width = 1000, height = 2000;
        Bitmap source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Task<Bitmap> task;
        Bitmap other;

        task = encodeDecodeTask(source, 100, 100);
        other = task.await(800);
        assertNotNull(other);
        assertTrue(other.getWidth() <= 100);
        assertTrue(other.getHeight() <= 100);

        task = encodeDecodeTask(source, Integer.MAX_VALUE, Integer.MAX_VALUE);
        other = task.await(800);
        assertNotNull(other);
        assertEquals(other.getWidth(), width);
        assertEquals(other.getHeight(), height);

        task = encodeDecodeTask(source, 6000, 6000);
        other = task.await(800);
        assertNotNull(other);
        assertEquals(other.getWidth(), width);
        assertEquals(other.getHeight(), height);
    }
}

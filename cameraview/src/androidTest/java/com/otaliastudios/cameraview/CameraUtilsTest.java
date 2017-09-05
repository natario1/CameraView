package com.otaliastudios.cameraview;


import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.internal.runner.InstrumentationConnection;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraUtilsTest {

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

    @Test
    public void testDecodeBitmap() {
        int w = 100, h = 200, color = Color.WHITE;
        Bitmap source = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        source.setPixel(0, 0, color);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        // Using lossy JPG we can't have strict comparison of values after compression.
        source.compress(Bitmap.CompressFormat.PNG, 100, os);

        // No orientation.
        Bitmap other = CameraUtils.decodeBitmap(os.toByteArray());
        assertEquals(100, w);
        assertEquals(200, h);
        assertEquals(color, other.getPixel(0, 0));
        assertEquals(0, other.getPixel(0, h-1));
        assertEquals(0, other.getPixel(w-1, 0));
        assertEquals(0, other.getPixel(w-1, h-1));

        // TODO: improve when we add EXIF writing to byte arrays
    }
}

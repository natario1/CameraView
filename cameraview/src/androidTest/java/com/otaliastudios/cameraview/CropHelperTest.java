package com.otaliastudios.cameraview;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CropHelperTest {

    @Test
    public void testCropFromYuv() {
        testCropFromYuv(1600, 1600, AspectRatio.of(16, 9));
        testCropFromYuv(1600, 1600, AspectRatio.of(9, 16));
    }

    private void testCropFromYuv(final int w, final int h, final AspectRatio target) {
        final boolean wider = target.toFloat() > ((float) w / (float) h);

        // Not sure how to test YuvImages...
        YuvImage i = mock(YuvImage.class);
        when(i.getWidth()).thenReturn(w);
        when(i.getHeight()).thenReturn(h);
        when(i.compressToJpeg(any(Rect.class), anyInt(), any(OutputStream.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock iom) throws Throwable {
                Object[] args = iom.getArguments();
                Rect rect = (Rect) args[0];

                // Assert.
                AspectRatio ratio = AspectRatio.of(rect.width(), rect.height());
                assertEquals(target, ratio);
                if (wider) { // width must match.
                    assertEquals(rect.width(), w);
                } else {
                    assertEquals(rect.height(), h);
                }
                return true;
            }
        });
        CropHelper.cropToJpeg(i, target, 100);
    }

    @Test
    public void testCropFromJpeg() {
        testCropFromJpeg(1600, 1600, AspectRatio.of(16, 9));
        testCropFromJpeg(1600, 1600, AspectRatio.of(9, 16));
    }

    private void testCropFromJpeg(int w, int h, AspectRatio target) {
        final boolean wider = target.toFloat() > ((float) w / (float) h);

        Bitmap source = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        source.compress(Bitmap.CompressFormat.JPEG, 100, os);
        byte[] b = CropHelper.cropToJpeg(os.toByteArray(), target, 100);
        Bitmap result = BitmapFactory.decodeByteArray(b, 0, b.length);

        // Assert.
        AspectRatio ratio = AspectRatio.of(result.getWidth(), result.getHeight());
        assertEquals(target, ratio);
        if (wider) { // width must match.
            assertEquals(result.getWidth(), w);
        } else {
            assertEquals(result.getHeight(), h);
        }
    }
}

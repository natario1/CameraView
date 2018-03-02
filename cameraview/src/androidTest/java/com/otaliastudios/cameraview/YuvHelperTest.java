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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class YuvHelperTest extends BaseTest {

    @Test
    public void testCrop() {
        testCrop(new Size(1600, 1600), AspectRatio.of(16, 16));
        testCrop(new Size(1600, 1600), AspectRatio.of(16, 9));
        testCrop(new Size(1600, 1600), AspectRatio.of(9, 16));
    }

    private void testCrop(final Size inSize, final AspectRatio outRatio) {
        AspectRatio inRatio = AspectRatio.of(inSize.getWidth(), inSize.getHeight());
        Rect out = YuvHelper.computeCrop(inSize, outRatio);
        Size outSize = new Size(out.width(), out.height());
        assertTrue(outRatio.matches(outSize));

        if (outRatio.matches(inSize)) {
            // They are equal.
            assertEquals(outSize.getWidth(), inSize.getWidth());
            assertEquals(outSize.getHeight(), inSize.getHeight());
        } else if (outRatio.toFloat() > inRatio.toFloat()) {
            // Width must match.
            assertEquals(outSize.getWidth(), inSize.getWidth());
            assertNotEquals(outSize.getHeight(), inSize.getHeight());
        } else {
            // Height must match.
            assertEquals(outSize.getHeight(), inSize.getHeight());
            assertNotEquals(outSize.getWidth(), inSize.getWidth());
        }
    }

}

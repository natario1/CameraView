package com.otaliastudios.cameraview.internal;


import android.graphics.Rect;

import com.otaliastudios.cameraview.BaseTest;
import com.otaliastudios.cameraview.internal.CropHelper;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CropHelperTest extends BaseTest {

    @Test
    public void testCrop() {
        testCrop(new Size(1600, 1600), AspectRatio.of(16, 16));
        testCrop(new Size(1600, 1600), AspectRatio.of(16, 9));
        testCrop(new Size(1600, 1600), AspectRatio.of(9, 16));
    }

    private void testCrop(final Size inSize, final AspectRatio outRatio) {
        AspectRatio inRatio = AspectRatio.of(inSize.getWidth(), inSize.getHeight());
        Rect out = CropHelper.computeCrop(inSize, outRatio);
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

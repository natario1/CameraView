package com.otaliastudios.cameraview.internal;


import androidx.exifinterface.media.ExifInterface;

import com.otaliastudios.cameraview.internal.ExifHelper;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExifHelperTest {

    @Test
    public void testValues() {
        assertEquals(0, ExifHelper.getOrientation(ExifInterface.ORIENTATION_NORMAL));
        assertEquals(0, ExifHelper.getOrientation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL));
        assertEquals(180, ExifHelper.getOrientation(ExifInterface.ORIENTATION_ROTATE_180));
        assertEquals(180, ExifHelper.getOrientation(ExifInterface.ORIENTATION_FLIP_VERTICAL));
        assertEquals(90, ExifHelper.getOrientation(ExifInterface.ORIENTATION_ROTATE_90));
        assertEquals(90, ExifHelper.getOrientation(ExifInterface.ORIENTATION_TRANSPOSE));
        assertEquals(270, ExifHelper.getOrientation(ExifInterface.ORIENTATION_ROTATE_270));
        assertEquals(270, ExifHelper.getOrientation(ExifInterface.ORIENTATION_TRANSVERSE));
    }

    @Test
    public void testUnknownValues() {
        assertEquals(0, ExifHelper.getOrientation(-15));
        assertEquals(0, ExifHelper.getOrientation(-1));
        assertEquals(0, ExifHelper.getOrientation(195));
        assertEquals(0, ExifHelper.getOrientation(Integer.MAX_VALUE));
    }

}

package com.otaliastudios.cameraview.internal.utils;


import androidx.exifinterface.media.ExifInterface;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExifHelperTest {

    @Test
    public void testValues() {
        assertEquals(0, ExifHelper.readExifOrientation(ExifInterface.ORIENTATION_NORMAL));
        assertEquals(0, ExifHelper.readExifOrientation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL));
        assertEquals(180, ExifHelper.readExifOrientation(ExifInterface.ORIENTATION_ROTATE_180));
        assertEquals(180, ExifHelper.readExifOrientation(ExifInterface.ORIENTATION_FLIP_VERTICAL));
        assertEquals(90, ExifHelper.readExifOrientation(ExifInterface.ORIENTATION_ROTATE_90));
        assertEquals(90, ExifHelper.readExifOrientation(ExifInterface.ORIENTATION_TRANSPOSE));
        assertEquals(270, ExifHelper.readExifOrientation(ExifInterface.ORIENTATION_ROTATE_270));
        assertEquals(270, ExifHelper.readExifOrientation(ExifInterface.ORIENTATION_TRANSVERSE));
    }

    @Test
    public void testUnknownValues() {
        assertEquals(0, ExifHelper.readExifOrientation(-15));
        assertEquals(0, ExifHelper.readExifOrientation(-1));
        assertEquals(0, ExifHelper.readExifOrientation(195));
        assertEquals(0, ExifHelper.readExifOrientation(Integer.MAX_VALUE));
    }

}

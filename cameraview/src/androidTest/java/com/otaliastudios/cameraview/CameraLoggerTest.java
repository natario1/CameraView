package com.otaliastudios.cameraview;


import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraLoggerTest extends BaseTest {

    @Test
    public void testLoggerLevels() {
        CameraLogger logger = CameraLogger.create("logger");

        // Verbose
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        logger.i("i");
        assertEquals(CameraLogger.lastMessage, "i");
        logger.w("w");
        assertEquals(CameraLogger.lastMessage, "w");
        logger.e("e");
        assertEquals(CameraLogger.lastMessage, "e");

        // Warning
        CameraLogger.lastMessage = null;
        CameraLogger.setLogLevel(CameraLogger.LEVEL_WARNING);
        logger.i("i");
        assertNull(CameraLogger.lastMessage);
        logger.w("w");
        assertEquals(CameraLogger.lastMessage, "w");
        logger.e("e");
        assertEquals(CameraLogger.lastMessage, "e");

        // Error
        CameraLogger.lastMessage = null;
        CameraLogger.setLogLevel(CameraLogger.LEVEL_ERROR);
        logger.i("i");
        assertNull(CameraLogger.lastMessage);
        logger.w("w");
        assertNull(CameraLogger.lastMessage);
        logger.e("e");
        assertEquals(CameraLogger.lastMessage, "e");
    }

    @Test
    public void testLoggerObjectArray() {
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        CameraLogger logger = CameraLogger.create("logger");
        logger.i("test", "logger", 10, null);
        assertEquals(CameraLogger.lastMessage, "test logger 10 null");
    }
}

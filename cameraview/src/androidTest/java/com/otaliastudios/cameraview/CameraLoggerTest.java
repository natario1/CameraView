package com.otaliastudios.cameraview;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraLoggerTest extends BaseTest {

    private String loggerTag = "myLogger";
    private CameraLogger logger;

    @Before
    public void setUp() {
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        logger = CameraLogger.create(loggerTag);
    }

    @After
    public void tearDown() {
        logger = null;
    }

    @Test
    public void testLoggerLevels() {
        // Verbose
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        logger.v("v");
        assertEquals(CameraLogger.lastMessage, "v");
        logger.i("i");
        assertEquals(CameraLogger.lastMessage, "i");
        logger.w("w");
        assertEquals(CameraLogger.lastMessage, "w");
        logger.e("e");
        assertEquals(CameraLogger.lastMessage, "e");

        // Info
        CameraLogger.lastMessage = null;
        CameraLogger.setLogLevel(CameraLogger.LEVEL_INFO);
        logger.v("v");
        assertNull(CameraLogger.lastMessage);
        logger.i("i");
        assertEquals(CameraLogger.lastMessage, "i");
        logger.w("w");
        assertEquals(CameraLogger.lastMessage, "w");
        logger.e("e");
        assertEquals(CameraLogger.lastMessage, "e");

        // Warning
        CameraLogger.lastMessage = null;
        CameraLogger.setLogLevel(CameraLogger.LEVEL_WARNING);
        logger.v("v");
        assertNull(CameraLogger.lastMessage);
        logger.i("i");
        assertNull(CameraLogger.lastMessage);
        logger.w("w");
        assertEquals(CameraLogger.lastMessage, "w");
        logger.e("e");
        assertEquals(CameraLogger.lastMessage, "e");

        // Error
        CameraLogger.lastMessage = null;
        CameraLogger.setLogLevel(CameraLogger.LEVEL_ERROR);
        logger.v("v");
        assertNull(CameraLogger.lastMessage);
        logger.i("i");
        assertNull(CameraLogger.lastMessage);
        logger.w("w");
        assertNull(CameraLogger.lastMessage);
        logger.e("e");
        assertEquals(CameraLogger.lastMessage, "e");
    }

    @Test
    public void testMessage() {
        logger.i("test", "logger", 10, null);
        assertEquals(CameraLogger.lastTag, loggerTag);
        assertEquals(CameraLogger.lastMessage, "test logger 10 null");
    }

    @Test
    public void testExternal() {
        CameraLogger.Logger mock = mock(CameraLogger.Logger.class);
        CameraLogger.registerLogger(mock);
        logger.e("hey");
        verify(mock, times(1)).log(CameraLogger.LEVEL_ERROR, loggerTag, "hey", null);

        reset(mock);
        CameraLogger.unregisterLogger(mock);
        logger.e("hey again");
        verify(mock, never()).log(anyInt(), anyString(), anyString(), any(Throwable.class));
    }

    @Test
    public void testThrowable() {
        CameraLogger.Logger mock = mock(CameraLogger.Logger.class);
        CameraLogger.registerLogger(mock);

        final Task<Throwable> task = new Task<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Throwable throwable = (Throwable) args[3];
                task.end(throwable);
                return null;
            }
        }).when(mock).log(anyInt(), anyString(), anyString(), any(Throwable.class));

        task.listen();
        logger.e("Got no error.");
        assertNull(task.await(100));

        task.listen();
        logger.e("Got error:", new RuntimeException(""));
        assertNotNull(task.await(100));

        task.listen();
        logger.e("Got", new RuntimeException(""), "while starting");
        assertNotNull(task.await(100));
    }
}

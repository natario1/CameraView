package com.otaliastudios.cameraview;

import android.support.annotation.IntDef;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Utility class that can log traces and info.
 */
public final class CameraLogger {

    public final static int LEVEL_VERBOSE = 0;
    public final static int LEVEL_WARNING = 1;
    public final static int LEVEL_ERROR = 2;

    @IntDef({LEVEL_VERBOSE, LEVEL_WARNING, LEVEL_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    @interface LogLevel {}

    private static int level = LEVEL_ERROR;

    public static void setLogLevel(int logLevel) {
        level = logLevel;
    }

    static String lastMessage;
    static String lastTag;

    static CameraLogger create(String tag) {
        return new CameraLogger(tag);
    }

    private String mTag;

    private CameraLogger(String tag) {
        mTag = tag;
    }

    public void i(String message) {
        if (should(LEVEL_VERBOSE)) {
            Log.i(mTag, message);
            lastMessage = message;
            lastTag = mTag;
        }
    }

    public void w(String message) {
        if (should(LEVEL_WARNING)) {
            Log.w(mTag, message);
            lastMessage = message;
            lastTag = mTag;
        }
    }

    public void e(String message) {
        if (should(LEVEL_ERROR)) {
            Log.w(mTag, message);
            lastMessage = message;
            lastTag = mTag;
        }
    }

    private boolean should(int messageLevel) {
        return level <= messageLevel;
    }

    private String string(int messageLevel, Object... ofData) {
        String message = "";
        if (should(messageLevel)) {
            for (Object o : ofData) {
                message += String.valueOf(o);
                message += " ";
            }
        }
        return message.trim();
    }

    public void i(Object... data) {
        i(string(LEVEL_VERBOSE, data));
    }

    public void w(Object... data) {
        w(string(LEVEL_WARNING, data));
    }

    public void e(Object... data) {
        e(string(LEVEL_ERROR, data));
    }
}


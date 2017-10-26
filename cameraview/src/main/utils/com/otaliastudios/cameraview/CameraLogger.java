package com.otaliastudios.cameraview;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that can log traces and info.
 */
public final class CameraLogger {

    public final static int LEVEL_VERBOSE = 0;
    public final static int LEVEL_INFO = 1;
    public final static int LEVEL_WARNING = 2;
    public final static int LEVEL_ERROR = 3;

    /**
     * Interface of integers representing log levels.
     * @see #LEVEL_VERBOSE
     * @see #LEVEL_INFO
     * @see #LEVEL_WARNING
     * @see #LEVEL_ERROR
     */
    @IntDef({LEVEL_VERBOSE, LEVEL_INFO, LEVEL_WARNING, LEVEL_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LogLevel {}

    /**
     * A Logger can listen to internal log events
     * and log them to different providers.
     * The default logger will simply post to logcat.
     */
    public interface Logger {

        /**
         * Notifies that an internal log event was just triggered.
         *
         * @param level the log level
         * @param tag the log tag
         * @param message the log message
         * @param throwable an optional throwable
         */
        void log(@LogLevel int level, String tag, String message, @Nullable Throwable throwable);
    }

    static String lastMessage;
    static String lastTag;

    private static int sLevel;
    private static List<Logger> sLoggers;

    static {
        setLogLevel(LEVEL_ERROR);
        sLoggers = new ArrayList<>();
        sLoggers.add(new Logger() {
            @Override
            public void log(int level, String tag, String message, @Nullable Throwable throwable) {
                switch (level) {
                    case LEVEL_VERBOSE: Log.v(tag, message, throwable); break;
                    case LEVEL_INFO: Log.i(tag, message, throwable); break;
                    case LEVEL_WARNING: Log.w(tag, message, throwable); break;
                    case LEVEL_ERROR: Log.e(tag, message, throwable); break;
                }
            }
        });
    }

    static CameraLogger create(String tag) {
        return new CameraLogger(tag);
    }

    /**
     * Sets the log sLevel for logcat events.
     *
     * @see #LEVEL_VERBOSE
     * @see #LEVEL_INFO
     * @see #LEVEL_WARNING
     * @see #LEVEL_ERROR
     * @param logLevel the desired log sLevel
     */
    public static void setLogLevel(@LogLevel int logLevel) {
        sLevel = logLevel;
    }

    /**
     * Registers an external {@link Logger} for log events.
     * Make sure to unregister using {@link #unregisterLogger(Logger)}.
     *
     * @param logger logger to add
     */
    public static void registerLogger(Logger logger) {
        sLoggers.add(logger);
    }

    /**
     * Unregisters a previously registered {@link Logger} for log events.
     * This is needed in order to avoid leaks.
     *
     * @param logger logger to remove
     */
    public static void unregisterLogger(Logger logger) {
        sLoggers.remove(logger);
    }

    private String mTag;

    private CameraLogger(String tag) {
        mTag = tag;
    }

    private boolean should(int messageLevel) {
        return sLevel <= messageLevel && sLoggers.size() > 0;
    }

    void v(Object... data) {
        log(LEVEL_VERBOSE, data);
    }

    void i(Object... data) {
        log(LEVEL_INFO, data);
    }

    void w(Object... data) {
        log(LEVEL_WARNING, data);
    }

    void e(Object... data) {
        log(LEVEL_ERROR, data);
    }

    private void log(@LogLevel int level, Object... data) {
        if (!should(level)) return;

        String message = "";
        Throwable throwable = null;
        final int size = data.length;
        for (Object object : data) {
            if (object instanceof Throwable) {
                throwable = (Throwable) object;
            }
            message += String.valueOf(object);
            message += " ";
        }
        message = message.trim();
        for (Logger logger : sLoggers) {
            logger.log(level, mTag, message, throwable);
        }

        lastMessage = message;
        lastTag = mTag;
    }
}


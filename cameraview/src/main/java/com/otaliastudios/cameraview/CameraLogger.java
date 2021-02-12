package com.otaliastudios.cameraview;

import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Utility class that can log traces and info.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
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
        void log(@LogLevel int level,
                 @NonNull String tag,
                 @NonNull String message,
                 @Nullable Throwable throwable);
    }

    @VisibleForTesting static String lastMessage;
    @VisibleForTesting static String lastTag;

    private static int sLevel;
    private static Set<Logger> sLoggers = new CopyOnWriteArraySet<>();

    @VisibleForTesting static Logger sAndroidLogger = new Logger() {
        @Override
        public void log(int level,
                        @NonNull String tag,
                        @NonNull String message,
                        @Nullable Throwable throwable) {
            switch (level) {
                case LEVEL_VERBOSE: Log.v(tag, message, throwable); break;
                case LEVEL_INFO: Log.i(tag, message, throwable); break;
                case LEVEL_WARNING: Log.w(tag, message, throwable); break;
                case LEVEL_ERROR: Log.e(tag, message, throwable); break;
            }
        }
    };

    static {
        setLogLevel(LEVEL_ERROR);
        sLoggers.add(sAndroidLogger);
    }

    /**
     * Creates a CameraLogger that will stream logs into the
     * internal logs and dispatch them to {@link Logger}s.
     *
     * @param tag the logger tag
     * @return a new CameraLogger
     */
    public static CameraLogger create(@NonNull String tag) {
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
    @SuppressWarnings("WeakerAccess")
    public static void registerLogger(@NonNull Logger logger) {
        sLoggers.add(logger);
    }

    /**
     * Unregisters a previously registered {@link Logger} for log events.
     * This is needed in order to avoid leaks.
     *
     * @param logger logger to remove
     */
    @SuppressWarnings("WeakerAccess")
    public static void unregisterLogger(@NonNull Logger logger) {
        sLoggers.remove(logger);
    }

    @NonNull
    private String mTag;

    private CameraLogger(@NonNull String tag) {
        mTag = tag;
    }

    private boolean should(int messageLevel) {
        return sLevel <= messageLevel && sLoggers.size() > 0;
    }

    /**
     * Log to the verbose channel.
     * @param data log contents
     * @return the log message, if logged
     */
    @Nullable
    public String v(@NonNull Object... data) {
        return log(LEVEL_VERBOSE, data);
    }

    /**
     * Log to the info channel.
     * @param data log contents
     * @return the log message, if logged
     */
    @Nullable
    public String i(@NonNull Object... data) {
        return log(LEVEL_INFO, data);
    }

    /**
     * Log to the warning channel.
     * @param data log contents
     * @return the log message, if logged
     */
    @Nullable
    public String w(@NonNull Object... data) {
        return log(LEVEL_WARNING, data);
    }

    /**
     * Log to the error channel.
     * @param data log contents
     * @return the log message, if logged
     */
    @Nullable
    public String e(@NonNull Object... data) {
        return log(LEVEL_ERROR, data);
    }

    @Nullable
    private String log(@LogLevel int level, @NonNull Object... data) {
        if (!should(level)) return null;

        StringBuilder message = new StringBuilder();
        Throwable throwable = null;
        for (Object object : data) {
            if (object instanceof Throwable) {
                throwable = (Throwable) object;
            }
            message.append(String.valueOf(object));
            message.append(" ");
        }
        String string = message.toString().trim();
        for (Logger logger : sLoggers) {
            logger.log(level, mTag, string, throwable);
        }
        lastMessage = string;
        lastTag = mTag;
        return string;
    }
}


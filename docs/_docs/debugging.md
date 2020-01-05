---
layout: page
title: "Debugging"
order: 15
disqus: 1
---

`CameraView` will log a lot of interesting events related to the camera lifecycle. These are important
to identify bugs. The default logger will simply use Android `Log` methods posting to logcat.

You can attach and detach external loggers using `CameraLogger.registerLogger()`:

```java
CameraLogger.registerLogger(new Logger() {
    @Override
    public void log(@LogLevel int level, String tag, String message, @Nullable Throwable throwable) {
        // For example...
        Crashlytics.log(message);
    }
});
```

Make sure you enable the logger using `CameraLogger.setLogLevel(@LogLevel int)`. The default will only
log error events.
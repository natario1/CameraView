---
name: Bug report
about: Create a report to help us improve
title: ''
labels: ''
assignees: ''

---

### Describe the bug
Please add a clear description of what the bug is, **and** fill the list below.
- CameraView version: *version number*
- Camera engine used: *camera1/camera2/both*
- Reproducible in official demo app: *yes/no*
- Device / Android version: *Pixel, API 28*
- I have read the [FAQ page](https://natario1.github.io/CameraView/about/faq): *yes/no*

### To Reproduce
Steps to reproduce the behavior, possibly in the demo app:
1. Go to '...'
2. Click on '...'
3. See error

### Expected behavior
A clear and concise description of what you expected to happen.

### XML layout
Part of the XML layout with the CameraView declaration, so we can read its attributes.

```xml
<CameraView
    android:layout_width="match_parent"
    android:layout_height="match_parent"       
    ...>
</CameraView>
```

### Screenshots
If applicable, add screenshots to help explain your problem.

### Logs
Use `CameraLogger.setLogLevel(LEVEL_INFO)` to see all logs into LogCat.

Use `CameraLogger.registerLogger()` to export to file or crash reporting service.

### APK
Link to a Github repo where the bug is reproducible.

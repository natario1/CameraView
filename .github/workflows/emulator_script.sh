#!/usr/bin/env bash
adb logcat -c
adb logcat CameraOrchestrator:I CameraEngine:I CameraView:I CameraCallbacks:I *:E -v color &
./gradlew cameraview:connectedCheck
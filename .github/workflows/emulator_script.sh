#!/usr/bin/env bash
adb logcat -c
adb logcat Camera*:I *:E &
./gradlew cameraview:connectedCheck
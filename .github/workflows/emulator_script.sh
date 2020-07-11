#!/usr/bin/env bash
# Core
ADB_TAGS="CameraView:I CameraCallbacks:I CameraOrchestrator:I CameraEngine:I"
ADB_TAGS="$ADB_TAGS CameraUtils:I WorkerHandler:I"
# Recorders
ADB_TAGS="$ADB_TAGS VideoRecorder:I FullVideoRecorder:I SnapshotVideoRecorder:I"
ADB_TAGS="$ADB_TAGS FullPictureRecorder:I SnapshotPictureRecorder:I DeviceEncoders:I"
# Video encoders
ADB_TAGS="$ADB_TAGS MediaEncoderEngine:I MediaEncoder:I AudioMediaEncoder:I VideoMediaEncoder:I TextureMediaEncoder:I"
# Debugging
ADB_TAGS="$ADB_TAGS CameraIntegrationTest:I MessageQueue:W MPEG4Writer:I"
adb logcat -c
adb logcat $ADB_TAGS *:E -v color &
./gradlew cameraview:runAndroidTests
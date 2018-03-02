# Migrating to v2

- JpegQuality: both cameraJpegQuality and setJpegQuality() have been removed, because
  they were working only with specific setups. We'll use the default quality provided
  by the camera engine.
- CropOutput: both cameraCropOutput and setCropOutput() have been removed. If you want
  your output to be cropped to match the view bounds, use the *snapshot() APIs.
- ExtraProperties: this has been removed.
- capturePicture(): renamed to takePicture().
- captureSnapshot(): renamed to takePictureSnapshot().
- startCapturingVideo(): renamed to takeVideo(). Signature changed from long to int.
- getSnapshotSize(): removed. The size of snapshots (pictures and videos) is equal to
  the preview size as returned by getPreviewSize().
- onVideoTaken(): now passing a VideoResult. Use VideoResult.getFile() to access the video file.
- CameraUtils.BitmapCallback: has been moved in a separate BitmapCallback class.
- isCapturingVideo(): renamed to isTakingVideo().

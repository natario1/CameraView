# Migrating to v2

- JpegQuality: both cameraJpegQuality and setJpegQuality() have been removed, because
  they were working only with specific setups. We'll use the default quality provided
  by the camera engine.
- capturePicture: renamed to takePicture()
- captureSnapshot: renamed to takePictureSnapshot()
- startCapturingVideo: renamed to takeVideo(). Signature changed from long to int.
- getSnapshotSize(): removed. The size of snapshots (pictures and videos) is equal to
  the preview size as returned by getPreviewSize().
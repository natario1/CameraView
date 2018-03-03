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
- getPreviewSize(): removed.
- getPictureSize(): the size is now equal to the output picture size (includes rotation).
- getSnapshotSize(): this is the size of pictures taken with takePictureSnapshot() and videos taken
  with takeVideoSnapshot(). It includes rotation and cropping.
- onVideoTaken(): now passing a VideoResult. Use VideoResult.getFile() to access the video file.
- onPictureTaken(): now passing a PictureResult. Use PictureResult.getJpeg() to access the jpeg stream.
- CameraUtils.BitmapCallback: has been moved in a separate BitmapCallback class.
- isCapturingVideo(): renamed to isTakingVideo().
- SessionType: renamed to Mode. This means that setSessionType() and cameraSessionType are renamed to
  setMode() and cameraMode.
- CameraOptions.isVideoSnapshotSupported(): removed, this would be ambiguous now. While in video
  mode, you can only use takePictureSnapshot(), not takePicture().
- takePicture(): will now throw an exception if called when Mode == Mode.VIDEO. You can only take snapshots.
- VideoQuality: this has been removed.
- CameraOptions: methods returning a Set now return a Collection.
- CameraOptions: in addition to getSupportedPictureSizes and getSupportedPictureAspectRatio,
  now there are video mode equivalents too.
- getPictureSize(): now it returns null when mode == Mode.VIDEO.
- getVideoSize(): added. Returns the size of the capture in video mode. Returns null when 
  mode == Mode.PICTURE.
- VideoSizeSelector: added. It is needed to choose the capture size in VIDEO mode.
  Defaults to SizeSelectors.biggest(), but you can choose by aspect ratio or whatever.
  
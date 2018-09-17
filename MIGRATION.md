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
- isTakingPicture(): added on top of isTakingVideo().
- takeVideoSnapshot(): new api. API 18 and the Gl preview, or it will throw.
  Respects orientation, videocodec and max duration limit.
  Automatically rotates the data. Automatically crops the video.
  NO audio support.
  NO maxSize limit.
- New cameraPreview XML attribute lets you choose the backing preview engine (surfaceView, textureView, GlSurfaceView).
  The default is GlSurfaceView and it is highly recommended that you do not change this.
- New pictureRecorder interface for picture capturing.
- Created FullPictureRecorder and SnapshotPictureRecorder for capturing HQ pictures and snapshots.
- When preview is GlSurface, the SnapshotPictureRecorder will use the gl texture and draw it into JPEG.
  This is really fast and allows us to avoid RotationHelper, creating bitmap copies, OOMs, EXIF stuff.
- When preview is GlSurface, you can take snapshots while recording video (or video snapshots!).
  TODO: document this
- TODO: cameraPreview documentation    
- TODO: takeVideoSnapshot documentation

TODO: add audio to the video snapshots
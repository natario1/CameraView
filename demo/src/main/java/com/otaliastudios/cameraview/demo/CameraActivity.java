package com.otaliastudios.cameraview.demo;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Environment;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.filter.Filters;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;
import com.otaliastudios.transcoder.TranscoderOptions;
import com.otaliastudios.transcoder.sink.DataSink;
import com.otaliastudios.transcoder.sink.DefaultDataSink;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener, OptionView.Callback {

    private final static CameraLogger LOG = CameraLogger.create("DemoApp");
    private final static boolean USE_FRAME_PROCESSOR = true;
    private final static boolean DECODE_BITMAP = false;

    private CameraView camera;
    private ViewGroup controlPanel;
    private long mCaptureTime;
    private long mVideoCaptureTime;
    private int mCurrentFilter = 0;
    private final Filters[] mAllFilters = Filters.values();


    private TextView mTvTimer;

    private ImageButton mImgPlayPauseVideo;
    private ImageButton mImgStopVideo;

    private String videoFilePath;

    private static final int MAX_RECORDING_TIME = 2 * 60 * 1000; // 2 MINUTES!


    private Handler mUpdateDurationHandler;

    private static final String TAG = "CameraActivity_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);

        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(this);
        camera.addCameraListener(new Listener());

        mImgPlayPauseVideo = findViewById(R.id.imgPlayPauseVideo);
        mImgStopVideo = findViewById(R.id.imgStopVideo);

        mTvTimer = findViewById(R.id.tvTimer);
        mTvTimer.setText("");

        mImgStopVideo.setVisibility(View.GONE);


        mImgPlayPauseVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.setMode(Mode.VIDEO);
                mImgStopVideo.setVisibility(View.VISIBLE);
                if (camera.isTakingVideo()) {

                    // PAUSE THE VIDEO

                    mImgPlayPauseVideo.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
                    camera.pauseVideoRecording();

                    killVideoDurationElapsedTimeHandler();
                    mVideoCaptureTime = System.currentTimeMillis(); // saves the pause time!
                    long secondsElapsed = (System.currentTimeMillis() - mVideoCaptureTime)/1000;

                } else if (camera.isRecordingPaused()) {

                    // RESUME VIDEO RECORDING


                    mImgPlayPauseVideo.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
                    camera.resumeVideoRecording();

                    long pauseTime = System.currentTimeMillis() - mVideoCaptureTime;
                    mVideoCaptureTime = mVideoCaptureTime+pauseTime;

//                    mVideoCaptureTime = System.currentTimeMillis() + mVideoCaptureTime;
//                    mVideoCaptureTime = System.currentTimeMillis() -(System.currentTimeMillis() - mVideoCaptureTime);
//                    mVideoCaptureTime = System.currentTimeMillis() - mVideoCaptureTime;
                    long secondsElapsed = (System.currentTimeMillis() - mVideoCaptureTime)/1000;
                    updateVideoDurationElapsedTime();


                } else {

                    mVideoCaptureTime = System.currentTimeMillis();

                    updateVideoDurationElapsedTime();
                    mImgPlayPauseVideo.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
                    File albumF = getAlbumDir();
                    String videoFileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
                    videoFileName = videoFileName+".mp4";

                    File videoFile = new File(albumF,videoFileName);
                    videoFilePath = videoFile.getAbsolutePath();
                    camera.takeVideo(videoFile, MAX_RECORDING_TIME);
//                    camera.takeVideoSnapshot(videoFile, MAX_RECORDING_TIME);



//                    camera.takeVideo(new File(getFilesDir(), "video.mp4"), MAX_RECORDING_TIME);
                }

            }
        });


        mImgStopVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killVideoDurationElapsedTimeHandler(); // make sure not to init more then one instance
                camera.stopVideo();
            }
        });


        if (USE_FRAME_PROCESSOR) {
            camera.addFrameProcessor(new FrameProcessor() {
                private long lastTime = System.currentTimeMillis();

                @Override
                public void process(@NonNull Frame frame) {
                    long newTime = frame.getTime();
                    long delay = newTime - lastTime;
                    lastTime = newTime;
                    LOG.v("Frame delayMillis:", delay, "FPS:", 1000 / delay);
                    if (DECODE_BITMAP) {
                        if (frame.getFormat() == ImageFormat.NV21
                                && frame.getDataClass() == byte[].class) {
                            byte[] data = frame.getData();
                            YuvImage yuvImage = new YuvImage(data,
                                    frame.getFormat(),
                                    frame.getSize().getWidth(),
                                    frame.getSize().getHeight(),
                                    null);
                            ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
                            yuvImage.compressToJpeg(new Rect(0, 0,
                                    frame.getSize().getWidth(),
                                    frame.getSize().getHeight()), 100, jpegStream);
                            byte[] jpegByteArray = jpegStream.toByteArray();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray,
                                    0, jpegByteArray.length);
                            //noinspection ResultOfMethodCallIgnored
                            bitmap.toString();
                        }
                    }
                }
            });
        }

        findViewById(R.id.edit).setOnClickListener(this);
        findViewById(R.id.capturePicture).setOnClickListener(this);
        findViewById(R.id.capturePictureSnapshot).setOnClickListener(this);
        findViewById(R.id.captureVideo).setOnClickListener(this);
        findViewById(R.id.captureVideoSnapshot).setOnClickListener(this);
        findViewById(R.id.toggleCamera).setOnClickListener(this);
        findViewById(R.id.changeFilter).setOnClickListener(this);

        controlPanel = findViewById(R.id.controls);
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
        final View watermark = findViewById(R.id.watermark);

        List<Option<?>> options = Arrays.asList(
                // Layout
                new Option.Width(), new Option.Height(),
                // Engine and preview
                new Option.Mode(), new Option.Engine(), new Option.Preview(),
                // Some controls
                new Option.Flash(), new Option.WhiteBalance(), new Option.Hdr(),
                new Option.PictureMetering(), new Option.PictureSnapshotMetering(),
                new Option.PictureFormat(),
                // Video recording
                new Option.PreviewFrameRate(), new Option.VideoCodec(), new Option.Audio(),
                // Gestures
                new Option.Pinch(), new Option.HorizontalScroll(), new Option.VerticalScroll(),
                new Option.Tap(), new Option.LongTap(),
                // Watermarks
                new Option.OverlayInPreview(watermark),
                new Option.OverlayInPictureSnapshot(watermark),
                new Option.OverlayInVideoSnapshot(watermark),
                // Frame Processing
                new Option.FrameProcessingFormat(),
                // Other
                new Option.Grid(), new Option.GridColor(), new Option.UseDeviceOrientation()
        );
        List<Boolean> dividers = Arrays.asList(
                // Layout
                false, true,
                // Engine and preview
                false, false, true,
                // Some controls
                false, false, false, false, false, true,
                // Video recording
                false, false, true,
                // Gestures
                false, false, false, false, true,
                // Watermarks
                false, false, true,
                // Frame Processing
                true,
                // Other
                false, false, true
        );
        for (int i = 0; i < options.size(); i++) {
            OptionView view = new OptionView(this);
            //noinspection unchecked
            view.setOption(options.get(i), this);
            view.setHasDivider(dividers.get(i));
            group.addView(view,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        controlPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
                b.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        // Animate the watermark just to show we record the animation in video snapshots
        ValueAnimator animator = ValueAnimator.ofFloat(1F, 0.8F);
        animator.setDuration(300);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float scale = (float) animation.getAnimatedValue();
                watermark.setScaleX(scale);
                watermark.setScaleY(scale);
                watermark.setRotation(watermark.getRotation() + 2);
            }
        });
        animator.start();
    }



    private final String TEMP_FILE_DIR = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +getPackageName() + "/temp";

    public  File getTempMediaFileDirectory() throws NullPointerException {
        File result = new File(TEMP_FILE_DIR);
        if (!result.exists()){
            result.mkdirs();
        }
        if (result != null){
            return result;
        } else {
            throw new NullPointerException();
        }
    }

    private  File getAlbumDir() {
        File storageDir = null;
        AlbumStorageDirFactory mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getString(R.string.album_name));
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
//                        Logger.Log("failed to create directory");
//                        return storageDir;
                        return null;
                    }
                }
        } else {
//            Logger.Log("External storage is not mounted READ/WRITE.");
        }
        return storageDir;
    }



    private void message(@NonNull String content, boolean important) {
        if (important) {
            LOG.w(content);
            Toast.makeText(this, content, Toast.LENGTH_LONG).show();
        } else {
            LOG.i(content);
            Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
        }
    }

    private class Listener extends CameraListener {

        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {
            ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
            for (int i = 0; i < group.getChildCount(); i++) {
                OptionView view = (OptionView) group.getChildAt(i);
                view.onCameraOpened(camera, options);
            }
        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
            message("Got CameraException #" + exception.getReason(), true);
        }

        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
            super.onPictureTaken(result);
            if (camera.isTakingVideo()) {
                message("Captured while taking video. Size=" + result.getSize(), false);
                return;
            }

            // This can happen if picture was taken with a gesture.
            long callbackTime = System.currentTimeMillis();
            if (mCaptureTime == 0) mCaptureTime = callbackTime - 300;
            LOG.w("onPictureTaken called! Launching activity. Delay:", callbackTime - mCaptureTime);
            PicturePreviewActivity.setPictureResult(result);
            Intent intent = new Intent(CameraActivity.this, PicturePreviewActivity.class);
            intent.putExtra("delay", callbackTime - mCaptureTime);
            startActivity(intent);
            mCaptureTime = 0;
            LOG.w("onPictureTaken called! Launched activity.");
        }

        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);
            LOG.w("onVideoTaken called! Launching activity.");
            VideoPreviewActivity.setVideoResult(result);
            Intent intent = new Intent(CameraActivity.this, VideoPreviewActivity.class);
            startActivity(intent);
            LOG.w("onVideoTaken called! Launched activity.");




            // notify yhe gallery after a new video added
                if (videoFilePath != null) {
                    // check if android M has access to write external storage
                    if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    File f = new File(videoFilePath);
                    Uri contentUri;

                    contentUri = Uri.fromFile(f);

                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);


//                    transcodeVideo();

                }

        }

        @Override
        public void onVideoRecordingStart() {
            super.onVideoRecordingStart();
            LOG.w("onVideoRecordingStart!");
        }

        @Override
        public void onVideoRecordingEnd() {
            super.onVideoRecordingEnd();
            message("Video taken. Processing...", false);
            LOG.w("onVideoRecordingEnd!");
            mImgPlayPauseVideo.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
            mImgStopVideo.setVisibility(View.GONE);
            mTvTimer.setText("");
        }


        @Override
        public void onVideoRecordingPause() {
            super.onVideoRecordingPause();
            message("Video pause", true);
            mImgPlayPauseVideo.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
        }

        @Override
        public void onVideoRecordingResume() {
            super.onVideoRecordingResume();
            message("Video resume",true);
            mImgPlayPauseVideo.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
        }


        @Override
        public void onExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onExposureCorrectionChanged(newValue, bounds, fingers);
            message("Exposure correction:" + newValue, false);
        }

        @Override
        public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onZoomChanged(newValue, bounds, fingers);
            message("Zoom:" + newValue, false);
        }
    }

    private void transcodeVideo() {

//        File f = new File(videoFilePath);
//        Uri contentUri;
//
//        contentUri = Uri.fromFile(f);

        File albumF = getAlbumDir();
        String videoFileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        videoFileName = videoFileName+".mp4";

        File videoFile = new File(albumF,videoFileName);
        final String videoFilePathCoded = videoFile.getAbsolutePath();
        Transcoder.into(videoFilePathCoded).addDataSource(videoFilePath).setListener(new TranscoderListener() {
            @Override
            public void onTranscodeProgress(double progress) {
                Log.d(TAG, "onTranscodeProgress: "+progress);
            }

            @Override
            public void onTranscodeCompleted(int successCode) {
                if (successCode == Transcoder.SUCCESS_TRANSCODED) {


                    // notify yhe gallery after a new video added
                    // check if android M has access to write external storage
                    if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    File f = new File(videoFilePathCoded);
                    Uri contentUri;

                    contentUri = Uri.fromFile(f);

                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);


                }else{
                    // error
                }
            }

            @Override
            public void onTranscodeCanceled() {
                Log.d(TAG, "onTranscodeCanceled: ");
            }

            @Override
            public void onTranscodeFailed(@NonNull Throwable exception) {
                // error!
                Log.d(TAG, "onTranscodeFailed: "+exception.getMessage());

            }
        }).transcode();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit: edit(); break;
            case R.id.capturePicture: capturePicture(); break;
            case R.id.capturePictureSnapshot: capturePictureSnapshot(); break;
            case R.id.captureVideo: captureVideo(); break;
            case R.id.captureVideoSnapshot: captureVideoSnapshot(); break;
            case R.id.toggleCamera: toggleCamera(); break;
            case R.id.changeFilter: changeCurrentFilter(); break;
        }
    }

    @Override
    public void onBackPressed() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        super.onBackPressed();
    }

    private void edit() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void capturePicture() {
        if (camera.getMode() == Mode.VIDEO) {
            message("Can't take HQ pictures while in VIDEO mode.", false);
            return;
        }
        if (camera.isTakingPicture()) return;
        mCaptureTime = System.currentTimeMillis();
        message("Capturing picture...", false);
        camera.takePicture();
    }

    private void capturePictureSnapshot() {
        if (camera.isTakingPicture()) return;
        if (camera.getPreview() != Preview.GL_SURFACE) {
            message("Picture snapshots are only allowed with the GL_SURFACE preview.", true);
            return;
        }
        mCaptureTime = System.currentTimeMillis();
        message("Capturing picture snapshot...", false);
        camera.takePictureSnapshot();
    }

    private void captureVideo() {
        if (camera.getMode() == Mode.PICTURE) {
            message("Can't record HQ videos while in PICTURE mode.", false);
            return;
        }
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        message("Recording for 5 seconds...", true);
        camera.takeVideo(new File(getFilesDir(), "video.mp4"), 5000);
    }

    private void captureVideoSnapshot() {
        if (camera.isTakingVideo()) {
            message("Already taking video.", false);
            return;
        }
        if (camera.getPreview() != Preview.GL_SURFACE) {
            message("Video snapshots are only allowed with the GL_SURFACE preview.", true);
            return;
        }
        message("Recording snapshot for 5 seconds...", true);
        camera.takeVideoSnapshot(new File(getFilesDir(), "video.mp4"), 5000);
    }

    private void toggleCamera() {
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        switch (camera.toggleFacing()) {
            case BACK:
                message("Switched to back camera!", false);
                break;

            case FRONT:
                message("Switched to front camera!", false);
                break;
        }
    }

    private void changeCurrentFilter() {
        if (camera.getPreview() != Preview.GL_SURFACE) {
            message("Filters are supported only when preview is Preview.GL_SURFACE.", true);
            return;
        }
        if (mCurrentFilter < mAllFilters.length - 1) {
            mCurrentFilter++;
        } else {
            mCurrentFilter = 0;
        }
        Filters filter = mAllFilters[mCurrentFilter];
        message(filter.toString(), false);

        // Normal behavior:
        camera.setFilter(filter.newInstance());

        // To test MultiFilter:
        // DuotoneFilter duotone = new DuotoneFilter();
        // duotone.setFirstColor(Color.RED);
        // duotone.setSecondColor(Color.GREEN);
        // camera.setFilter(new MultiFilter(duotone, filter.newInstance()));
    }




    private void updateVideoDurationElapsedTime(){

        killVideoDurationElapsedTimeHandler(); // make sure not to init more then one instance

        mUpdateDurationHandler = new Handler();
        mUpdateDurationHandler.postDelayed(mVideoDurationRunnable,1000);

        long secondsElapsed = (System.currentTimeMillis() - mVideoCaptureTime)/1000;
        mTvTimer.setText(DateUtils.formatElapsedTime(secondsElapsed));


    }

    private Runnable mVideoDurationRunnable = new Runnable() {
        @Override
        public void run() {
            updateVideoDurationElapsedTime();
        }
    };


    private void killVideoDurationElapsedTimeHandler(){
        if (mUpdateDurationHandler!=null){
            mUpdateDurationHandler.removeCallbacks(mVideoDurationRunnable);
        }
    }


    @Override
    public <T> boolean onValueChanged(@NonNull Option<T> option, @NonNull T value, @NonNull String name) {
        if ((option instanceof Option.Width || option instanceof Option.Height)) {
            Preview preview = camera.getPreview();
            boolean wrapContent = (Integer) value == ViewGroup.LayoutParams.WRAP_CONTENT;
            if (preview == Preview.SURFACE && !wrapContent) {
                message("The SurfaceView preview does not support width or height changes. " +
                        "The view will act as WRAP_CONTENT by default.", true);
                return false;
            }
        }
        option.set(camera, value);
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_HIDDEN);
        message("Changed " + option.getName() + " to " + name, false);
        return true;
    }

    //region Permissions

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isOpened()) {
            camera.open();
        }
    }

    //endregion


    @Override
    protected void onPause() {
        super.onPause();
        killVideoDurationElapsedTimeHandler();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        killVideoDurationElapsedTimeHandler();
    }
}

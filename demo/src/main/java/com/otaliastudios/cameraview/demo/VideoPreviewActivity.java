package com.otaliastudios.cameraview.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class VideoPreviewActivity extends AppCompatActivity {

    private VideoView videoView;

    private static VideoResult videoResult;

    private static final String TAG = "CameraActivity_";

    public static void setVideoResult(@Nullable VideoResult result) {
        videoResult = result;
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);
        final VideoResult result = videoResult;
        if (result == null) {
            finish();
            return;
        }

        videoView = findViewById(R.id.video);
        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playVideo();
            }
        });
        final MessageView actualResolution = findViewById(R.id.actualResolution);
        final MessageView isSnapshot = findViewById(R.id.isSnapshot);
        final MessageView rotation = findViewById(R.id.rotation);
        final MessageView audio = findViewById(R.id.audio);
        final MessageView audioBitRate = findViewById(R.id.audioBitRate);
        final MessageView videoCodec = findViewById(R.id.videoCodec);
        final MessageView videoBitRate = findViewById(R.id.videoBitRate);
        final MessageView videoFrameRate = findViewById(R.id.videoFrameRate);

        AspectRatio ratio = AspectRatio.of(result.getSize());
        actualResolution.setTitleAndMessage("Size", result.getSize() + " (" + ratio + ")");
        isSnapshot.setTitleAndMessage("Snapshot", result.isSnapshot() + "");
        rotation.setTitleAndMessage("Rotation", result.getRotation() + "");
        audio.setTitleAndMessage("Audio", result.getAudio().name());
        audioBitRate.setTitleAndMessage("Audio bit rate", result.getAudioBitRate() + " bits per sec.");
        videoCodec.setTitleAndMessage("VideoCodec", result.getVideoCodec().name());
        videoBitRate.setTitleAndMessage("Video bit rate", result.getVideoBitRate() + " bits per sec.");
        videoFrameRate.setTitleAndMessage("Video frame rate", result.getVideoFrameRate() + " fps");
        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        controller.setMediaPlayer(videoView);
        videoView.setMediaController(controller);
        videoView.setVideoURI(Uri.fromFile(result.getFile()));
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                ViewGroup.LayoutParams lp = videoView.getLayoutParams();
                float videoWidth = mp.getVideoWidth();
                float videoHeight = mp.getVideoHeight();
                float viewWidth = videoView.getWidth();
                lp.height = (int) (viewWidth * (videoHeight / videoWidth));
                videoView.setLayoutParams(lp);
                playVideo();

                if (result.isSnapshot()) {
                    // Log the real size for debugging reason.
                    Log.e("VideoPreview", "The video full size is " + videoWidth + "x" + videoHeight);
                }
            }
        });



        transcodeVideo();
    }

    void playVideo() {
        if (!videoView.isPlaying()) {
            videoView.start();
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
        Transcoder.into(videoFilePathCoded).addDataSource(videoResult.getFile().getAbsolutePath()).setListener(new TranscoderListener() {
            @Override
            public void onTranscodeProgress(double progress) {
                Log.d(TAG, "onTranscodeProgress: "+progress);
            }

            @Override
            public void onTranscodeCompleted(int successCode) {
                if (successCode == Transcoder.SUCCESS_TRANSCODED) {


                    // notify yhe gallery after a new video added
                    // check if android M has access to write external storage
                    if (ActivityCompat.checkSelfPermission(VideoPreviewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            setVideoResult(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.share) {
            Toast.makeText(this, "Sharing...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            Uri uri = FileProvider.getUriForFile(this,
                    this.getPackageName() + ".provider",
                    videoResult.getFile());
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

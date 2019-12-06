package com.otaliastudios.cameraview.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

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

import java.io.File;


public class VideoPreviewActivity extends AppCompatActivity {

    private VideoView videoView;

    private static VideoResult videoResult;

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
    }

    void playVideo() {
        if (!videoView.isPlaying()) {
            videoView.start();
        }
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

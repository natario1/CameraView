package com.flurgle.camerakit.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.flurgle.camerakit.AspectRatio;
import com.flurgle.camerakit.Size;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VideoPreviewActivity extends Activity {

    @BindView(R.id.video)
    VideoView videoView;

    @BindView(R.id.actualResolution)
    TextView actualResolution;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);
        ButterKnife.bind(this);

        Uri videoUri = getIntent().getParcelableExtra("video");
        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        controller.setMediaPlayer(videoView);
        videoView.setMediaController(controller);
        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                actualResolution.setText(mp.getVideoWidth() + " x " + mp.getVideoHeight());
            }
        });
        playVideo();
    }


    @OnClick(R.id.video)
    void playVideo() {
        if (videoView.isPlaying()) return;
        videoView.start();
    }
}

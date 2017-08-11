package com.otaliastudios.cameraview.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraConstants;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Grid;
import com.otaliastudios.cameraview.Size;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements View.OnLayoutChangeListener {

    @BindView(R.id.activity_main)
    ViewGroup parent;

    @BindView(R.id.camera)
    CameraView camera;

    // Capture Mode:
    @BindView(R.id.sessionTypeRadioGroup)
    RadioGroup sessionTypeRadioGroup;

    // Crop Mode:
    @BindView(R.id.cropModeRadioGroup)
    RadioGroup cropModeRadioGroup;

    // Video Quality:
    @BindView(R.id.videoQualityRadioGroup)
    RadioGroup videoQualityRadioGroup;

    // Grid mode:
    @BindView(R.id.gridModeRadioGroup)
    RadioGroup gridModeRadioGroup;

    // Width:
    @BindView(R.id.screenWidth)
    TextView screenWidth;
    @BindView(R.id.width)
    EditText width;
    @BindView(R.id.widthUpdate)
    Button widthUpdate;
    @BindView(R.id.widthModeRadioGroup)
    RadioGroup widthModeRadioGroup;

    // Height:
    @BindView(R.id.screenHeight)
    TextView screenHeight;
    @BindView(R.id.height)
    EditText height;
    @BindView(R.id.heightUpdate)
    Button heightUpdate;
    @BindView(R.id.heightModeRadioGroup)
    RadioGroup heightModeRadioGroup;

    private boolean mCapturingPicture;
    private boolean mCapturingVideo;

    // To show stuff in the callback
    private Size mCaptureNativeSize;
    private long mCaptureTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        parent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                screenWidth.setText("screen: " + parent.getWidth() + "px");
                screenHeight.setText("screen: " + parent.getHeight() + "px");
            }
        });

        sessionTypeRadioGroup.setOnCheckedChangeListener(sessionTypeChangedListener);
        cropModeRadioGroup.setOnCheckedChangeListener(cropModeChangedListener);
        widthModeRadioGroup.setOnCheckedChangeListener(widthModeChangedListener);
        heightModeRadioGroup.setOnCheckedChangeListener(heightModeChangedListener);
        videoQualityRadioGroup.setOnCheckedChangeListener(videoQualityChangedListener);
        gridModeRadioGroup.setOnCheckedChangeListener(gridModeChangedListener);

        camera.addOnLayoutChangeListener(this);
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] jpeg) {
                super.onPictureTaken(jpeg);
                mCapturingPicture = false;
                long callbackTime = System.currentTimeMillis();
                if (mCapturingVideo) {
                    message("Captured while taking video. Size="+mCaptureNativeSize, false);
                    return;
                }
                PicturePreviewActivity.setImage(jpeg);
                Intent intent = new Intent(MainActivity.this, PicturePreviewActivity.class);
                intent.putExtra("delay", callbackTime - mCaptureTime);
                intent.putExtra("nativeWidth", mCaptureNativeSize.getWidth());
                intent.putExtra("nativeHeight", mCaptureNativeSize.getHeight());
                startActivity(intent);
            }

            @Override
            public void onVideoTaken(File video) {
                super.onVideoTaken(video);
                mCapturingVideo = false;
                Intent intent = new Intent(MainActivity.this, VideoPreviewActivity.class);
                intent.putExtra("video", Uri.fromFile(video));
                startActivity(intent);
            }
        });
    }

    private void message(String content, boolean important) {
        int length = important ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(this, content, length).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.destroy();
    }


    @OnClick(R.id.capturePhoto)
    void capturePhoto() {
        if (mCapturingPicture) return;
        mCapturingPicture = true;
        mCaptureTime = System.currentTimeMillis();
        mCaptureNativeSize = camera.getCaptureSize();
        message("Capturing picture...", false);
        camera.capturePicture();
    }

    @OnClick(R.id.captureVideo)
    void captureVideo() {
        if (camera.getSessionType() != CameraConstants.SESSION_TYPE_VIDEO) {
            message("Can't record video while session type is 'picture'.", false);
            return;
        }
        if (mCapturingPicture || mCapturingVideo) return;
        mCapturingVideo = true;
        message("Recording for 8 seconds...", true);
        camera.startCapturingVideo(null);
        camera.postDelayed(new Runnable() {
            @Override
            public void run() {
                camera.stopCapturingVideo();
            }
        }, 8000);
    }

    @OnClick(R.id.toggleCamera)
    void toggleCamera() {
        if (mCapturingPicture) return;
        switch (camera.toggleFacing()) {
            case BACK:
                message("Switched to back camera!", false);
                break;

            case FRONT:
                message("Switched to front camera!", false);
                break;
        }
    }

    @OnClick(R.id.toggleFlash)
    void toggleFlash() {
        if (mCapturingPicture) return;
        switch (camera.toggleFlash()) {
            case ON:
                message("Flash on!", false);
                break;

            case OFF:
                message("Flash off!", false);
                break;

            case AUTO:
                message("Flash auto!", false);
                break;
        }
    }

    RadioGroup.OnCheckedChangeListener sessionTypeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mCapturingPicture) return;
            camera.setSessionType(
                    checkedId == R.id.sessionTypePicture ?
                            CameraConstants.SESSION_TYPE_PICTURE :
                            CameraConstants.SESSION_TYPE_VIDEO
            );
            message("Session type set to" + (checkedId == R.id.sessionTypePicture ? " picture!" : " video!"), true);
        }
    };

    RadioGroup.OnCheckedChangeListener cropModeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mCapturingPicture) return;
            camera.setCropOutput(checkedId == R.id.modeCropVisible);
            message("Picture cropping is" + (checkedId == R.id.modeCropVisible ? " on!" : " off!"), false);
        }
    };

    RadioGroup.OnCheckedChangeListener videoQualityChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mCapturingVideo) return;
            int videoQuality = CameraConstants.VIDEO_QUALITY_HIGHEST;
            switch (checkedId) {
                case R.id.videoQualityLowest: videoQuality = CameraConstants.VIDEO_QUALITY_LOWEST; break;
                case R.id.videoQualityQvga: videoQuality = CameraConstants.VIDEO_QUALITY_QVGA; break;
                case R.id.videoQuality480p: videoQuality = CameraConstants.VIDEO_QUALITY_480P; break;
                case R.id.videoQuality720p: videoQuality = CameraConstants.VIDEO_QUALITY_720P; break;
                case R.id.videoQuality1080p: videoQuality = CameraConstants.VIDEO_QUALITY_1080P; break;
                case R.id.videoQuality2160p: videoQuality = CameraConstants.VIDEO_QUALITY_2160P; break;
                case R.id.videoQualityHighest: videoQuality = CameraConstants.VIDEO_QUALITY_HIGHEST; break;
            }
            camera.setVideoQuality(videoQuality);
            message("Video quality changed!", false);
        }
    };


    RadioGroup.OnCheckedChangeListener gridModeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Grid grid = Grid.OFF;
            switch (checkedId) {
                case R.id.gridModeOff: grid = Grid.OFF; break;
                case R.id.gridMode3x3: grid = Grid.DRAW_3X3; break;
                case R.id.gridMode4x4: grid = Grid.DRAW_4X4; break;
                case R.id.gridModeGolden: grid = Grid.DRAW_PHI; break;
            }
            camera.setGrid(grid);
            message("Grid mode changed!", false);
        }
    };


    @OnClick(R.id.widthUpdate)
    void widthUpdateClicked() {
        if (mCapturingPicture) return;
        if (widthUpdate.getAlpha() >= 1) {
            updateCamera(true, false);
        }
    }

    RadioGroup.OnCheckedChangeListener widthModeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mCapturingPicture) return;
            widthUpdate.setEnabled(checkedId == R.id.widthCustom);
            widthUpdate.setAlpha(checkedId == R.id.widthCustom ? 1f : 0.3f);
            width.clearFocus();
            width.setEnabled(checkedId == R.id.widthCustom);
            width.setAlpha(checkedId == R.id.widthCustom ? 1f : 0.5f);

            updateCamera(true, false);
        }
    };

    @OnClick(R.id.heightUpdate)
    void heightUpdateClicked() {
        if (mCapturingPicture) return;
        if (heightUpdate.getAlpha() >= 1) {
            updateCamera(false, true);
        }
    }

    RadioGroup.OnCheckedChangeListener heightModeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mCapturingPicture) return;
            heightUpdate.setEnabled(checkedId == R.id.heightCustom);
            heightUpdate.setAlpha(checkedId == R.id.heightCustom ? 1f : 0.3f);
            height.clearFocus();
            height.setEnabled(checkedId == R.id.heightCustom);
            height.setAlpha(checkedId == R.id.heightCustom ? 1f : 0.5f);

            updateCamera(false, true);
        }
    };

    private void updateCamera(boolean updateWidth, boolean updateHeight) {
        if (mCapturingPicture) return;
        ViewGroup.LayoutParams cameraLayoutParams = camera.getLayoutParams();
        int width = cameraLayoutParams.width;
        int height = cameraLayoutParams.height;

        if (updateWidth) {
            switch (widthModeRadioGroup.getCheckedRadioButtonId()) {
                case R.id.widthCustom:
                    String widthInput = this.width.getText().toString();
                    try { width = Integer.valueOf(widthInput); } catch (Exception e) {}
                    break;
                case R.id.widthWrapContent:
                    width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    break;
                case R.id.widthMatchParent:
                    width = ViewGroup.LayoutParams.MATCH_PARENT;
                    break;
            }
        }

        if (updateHeight) {
            switch (heightModeRadioGroup.getCheckedRadioButtonId()) {
                case R.id.heightCustom:
                    String heightInput = this.height.getText().toString();
                    try { height = Integer.valueOf(heightInput); } catch (Exception e) {}
                    break;
                case R.id.heightWrapContent:
                    height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    break;
                case R.id.heightMatchParent:
                    // We are in a vertically scrolling container, match parent would not work at all.
                    height = parent.getHeight();
                    break;
            }
        }

        cameraLayoutParams.width = width;
        cameraLayoutParams.height = height;
        camera.addOnLayoutChangeListener(this);
        camera.setLayoutParams(cameraLayoutParams);

        String what = (updateWidth && updateHeight ? "Width and height" : updateWidth ? "Width" : "Height");
        message(what + " updated! Internal preview size: " + camera.getPreviewSize(), false);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        int mCameraWidth = right - left;
        int mCameraHeight = bottom - top;
        width.setText(String.valueOf(mCameraWidth));
        height.setText(String.valueOf(mCameraHeight));
        camera.removeOnLayoutChangeListener(this);
    }

}

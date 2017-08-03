package com.flurgle.camerakit.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;

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

    private int mCameraWidth;
    private int mCameraHeight;
    private boolean mCapturing;

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

        camera.addOnLayoutChangeListener(this);

        sessionTypeRadioGroup.setOnCheckedChangeListener(sessionTypeChangedListener);
        cropModeRadioGroup.setOnCheckedChangeListener(cropModeChangedListener);
        widthModeRadioGroup.setOnCheckedChangeListener(widthModeChangedListener);
        heightModeRadioGroup.setOnCheckedChangeListener(heightModeChangedListener);
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
        if (mCapturing) return;
        mCapturing = true;
        final long startTime = System.currentTimeMillis();
        camera.clearCameraListeners();
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] jpeg) {
                super.onPictureTaken(jpeg);
                mCapturing = false;
                long callbackTime = System.currentTimeMillis();
                PicturePreviewActivity.setImage(jpeg);
                Intent intent = new Intent(MainActivity.this, PicturePreviewActivity.class);
                intent.putExtra("delay", callbackTime-startTime);
                startActivity(intent);
            }
        });
        camera.captureImage();
    }

    @OnClick(R.id.captureVideo)
    void captureVideo() {
        if (camera.getSessionType() != CameraKit.Constants.SESSION_TYPE_VIDEO) {
            Toast.makeText(this, "Can't record video while session type is 'picture'.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mCapturing) return;
        mCapturing = true;
        camera.clearCameraListeners();
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onVideoTaken(File video) {
                super.onVideoTaken(video);
                mCapturing = false;
                Intent intent = new Intent(MainActivity.this, VideoPreviewActivity.class);
                intent.putExtra("video", Uri.fromFile(video));
                startActivity(intent);
            }
        });
        Toast.makeText(this, "Recording for 8 seconds...", Toast.LENGTH_LONG).show();
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
        if (mCapturing) return;
        switch (camera.toggleFacing()) {
            case CameraKit.Constants.FACING_BACK:
                Toast.makeText(this, "Switched to back camera!", Toast.LENGTH_SHORT).show();
                break;

            case CameraKit.Constants.FACING_FRONT:
                Toast.makeText(this, "Switched to front camera!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @OnClick(R.id.toggleFlash)
    void toggleFlash() {
        if (mCapturing) return;
        switch (camera.toggleFlash()) {
            case CameraKit.Constants.FLASH_ON:
                Toast.makeText(this, "Flash on!", Toast.LENGTH_SHORT).show();
                break;

            case CameraKit.Constants.FLASH_OFF:
                Toast.makeText(this, "Flash off!", Toast.LENGTH_SHORT).show();
                break;

            case CameraKit.Constants.FLASH_AUTO:
                Toast.makeText(this, "Flash auto!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    RadioGroup.OnCheckedChangeListener sessionTypeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mCapturing) return;
            camera.setSessionType(
                    checkedId == R.id.sessionTypePicture ?
                            CameraKit.Constants.SESSION_TYPE_PICTURE :
                            CameraKit.Constants.SESSION_TYPE_VIDEO
            );
            Toast.makeText(MainActivity.this, "Session type set to" + (checkedId == R.id.sessionTypePicture ? " picture!" : " video!"), Toast.LENGTH_SHORT).show();
        }
    };

    RadioGroup.OnCheckedChangeListener cropModeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mCapturing) return;
            camera.setCropOutput(checkedId == R.id.modeCropVisible);
            Toast.makeText(MainActivity.this, "Picture cropping is" + (checkedId == R.id.modeCropVisible ? " on!" : " off!"), Toast.LENGTH_SHORT).show();
        }
    };

    @OnClick(R.id.widthUpdate)
    void widthUpdateClicked() {
        if (mCapturing) return;
        if (widthUpdate.getAlpha() >= 1) {
            updateCamera(true, false);
        }
    }

    RadioGroup.OnCheckedChangeListener widthModeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mCapturing) return;
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
        if (mCapturing) return;
        if (heightUpdate.getAlpha() >= 1) {
            updateCamera(false, true);
        }
    }

    RadioGroup.OnCheckedChangeListener heightModeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (mCapturing) return;
            heightUpdate.setEnabled(checkedId == R.id.heightCustom);
            heightUpdate.setAlpha(checkedId == R.id.heightCustom ? 1f : 0.3f);
            height.clearFocus();
            height.setEnabled(checkedId == R.id.heightCustom);
            height.setAlpha(checkedId == R.id.heightCustom ? 1f : 0.5f);

            updateCamera(false, true);
        }
    };

    private void updateCamera(boolean updateWidth, boolean updateHeight) {
        if (mCapturing) return;
        ViewGroup.LayoutParams cameraLayoutParams = camera.getLayoutParams();
        int width = cameraLayoutParams.width;
        int height = cameraLayoutParams.height;

        if (updateWidth) {
            switch (widthModeRadioGroup.getCheckedRadioButtonId()) {
                case R.id.widthCustom:
                    String widthInput = this.width.getText().toString();
                    if (widthInput.length() > 0) {
                        try {
                            width = Integer.valueOf(widthInput);
                        } catch (Exception e) {

                        }
                    }

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
                    if (heightInput.length() > 0) {
                        try {
                            height = Integer.valueOf(heightInput);
                        } catch (Exception e) {

                        }
                    }
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

        Toast.makeText(this, (updateWidth && updateHeight ? "Width and height" : updateWidth ? "Width" : "Height") + " updated!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        mCameraWidth = right - left;
        mCameraHeight = bottom - top;
        width.setText(String.valueOf(mCameraWidth));
        height.setText(String.valueOf(mCameraHeight));
        camera.removeOnLayoutChangeListener(this);
    }

}

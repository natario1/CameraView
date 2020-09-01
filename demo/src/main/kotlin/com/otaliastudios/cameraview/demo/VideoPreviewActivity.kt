package com.otaliastudios.cameraview.demo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.size.AspectRatio

class VideoPreviewActivity : AppCompatActivity() {
    companion object {
        var videoResult: VideoResult? = null
    }

    private val videoView: VideoView by lazy { findViewById<VideoView>(R.id.video) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_preview)
        val result = videoResult ?: run {
            finish()
            return
        }
        videoView.setOnClickListener { playVideo() }
        val actualResolution = findViewById<MessageView>(R.id.actualResolution)
        val isSnapshot = findViewById<MessageView>(R.id.isSnapshot)
        val rotation = findViewById<MessageView>(R.id.rotation)
        val audio = findViewById<MessageView>(R.id.audio)
        val audioBitRate = findViewById<MessageView>(R.id.audioBitRate)
        val videoCodec = findViewById<MessageView>(R.id.videoCodec)
        val audioCodec = findViewById<MessageView>(R.id.audioCodec)
        val videoBitRate = findViewById<MessageView>(R.id.videoBitRate)
        val videoFrameRate = findViewById<MessageView>(R.id.videoFrameRate)

        val ratio = AspectRatio.of(result.size)
        actualResolution.setTitleAndMessage("Size", "${result.size} ($ratio)")
        isSnapshot.setTitleAndMessage("Snapshot", result.isSnapshot.toString())
        rotation.setTitleAndMessage("Rotation", result.rotation.toString())
        audio.setTitleAndMessage("Audio", result.audio.name)
        audioBitRate.setTitleAndMessage("Audio bit rate", "${result.audioBitRate} bits per sec.")
        videoCodec.setTitleAndMessage("VideoCodec", result.videoCodec.name)
        audioCodec.setTitleAndMessage("AudioCodec", result.audioCodec.name)
        videoBitRate.setTitleAndMessage("Video bit rate", "${result.videoBitRate} bits per sec.")
        videoFrameRate.setTitleAndMessage("Video frame rate", "${result.videoFrameRate} fps")

        val controller = MediaController(this)
        controller.setAnchorView(videoView)
        controller.setMediaPlayer(videoView)
        videoView.setMediaController(controller)
        videoView.setVideoURI(Uri.fromFile(result.file))
        videoView.setOnPreparedListener { mp ->
            val lp = videoView.layoutParams
            val videoWidth = mp.videoWidth.toFloat()
            val videoHeight = mp.videoHeight.toFloat()
            val viewWidth = videoView.width.toFloat()
            lp.height = (viewWidth * (videoHeight / videoWidth)).toInt()
            videoView.layoutParams = lp
            playVideo()
            if (result.isSnapshot) {
                // Log the real size for debugging reason.
                Log.e("VideoPreview", "The video full size is " + videoWidth + "x" + videoHeight)
            }
        }
    }

    fun playVideo() {
        if (!videoView.isPlaying) {
            videoView.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            videoResult = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.share) {
            Toast.makeText(this, "Sharing...", Toast.LENGTH_SHORT).show()
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "video/*"
            val uri = FileProvider.getUriForFile(this,
                    this.packageName + ".provider",
                    videoResult!!.file)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
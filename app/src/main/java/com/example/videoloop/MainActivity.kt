package com.example.videoloop

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var videoViewTop: FitVideoView
    private lateinit var videoViewBottom: FitVideoView
    private var isPlaying = false

    // GetContent opens the system media picker. It does NOT require storage permissions!
    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            playVideo(uri)
        } else {
            Toast.makeText(this, "No video selected. Tap screen to retry.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoViewTop = findViewById(R.id.videoViewTop)
        videoViewBottom = findViewById(R.id.videoViewBottom)

        // Tap anywhere on the video views to select a new video
        videoViewTop.setOnClickListener { launchVideoPicker() }
        videoViewBottom.setOnClickListener { launchVideoPicker() }

        // Launch picker automatically on first start
        launchVideoPicker()
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            videoViewTop.pause()
            videoViewBottom.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPlaying) {
            videoViewTop.start()
            videoViewBottom.start()
        }
    }

    private fun launchVideoPicker() {
        // "video/*" filters the system picker to show only videos
        pickMedia.launch("video/*")
    }

    private fun playVideo(uri: Uri) {
        videoViewTop.setVideoURI(uri)
        videoViewBottom.setVideoURI(uri)

        videoViewTop.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoViewTop.start()
        }
        
        videoViewBottom.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoViewBottom.start()
        }
        
        // Fallback for some devices
        videoViewTop.setOnCompletionListener { it.start() }
        videoViewBottom.setOnCompletionListener { it.start() }
        
        isPlaying = true
    }
}

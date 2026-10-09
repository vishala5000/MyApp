package com.example.videoloop

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var videoViewTop: VideoView
    private lateinit var videoViewBottom: VideoView
    private var isPlaying = false

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
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

        checkAndRequestPermissions()
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

    private fun checkAndRequestPermissions() {
        val listPermissionsNeeded = mutableListOf<String>()
        for (p in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 101)
        } else {
            launchVideoPicker()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                launchVideoPicker()
            } else {
                Toast.makeText(this, "Permissions required to access video", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun launchVideoPicker() {
        pickMedia.launch(ActivityResultContracts.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    private fun playVideo(uri: Uri) {
        videoViewTop.setVideoURI(uri)
        videoViewBottom.setVideoURI(uri)

        // Endless looping setup
        videoViewTop.setOnCompletionListener { it.start() }
        videoViewBottom.setOnCompletionListener { it.start() }

        videoViewTop.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoViewTop.start()
        }
        
        videoViewBottom.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoViewBottom.start()
        }
        
        isPlaying = true
    }
}

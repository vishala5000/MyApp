package com.example.videoloop

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var videoViewTop: FitVideoView
    private lateinit var videoViewBottom: FitVideoView
    private var isPlaying = false

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            launchVideoPicker()
        } else {
            Toast.makeText(this, "Storage permission is required to select a video", Toast.LENGTH_LONG).show()
        }
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

        videoViewTop.setOnClickListener { checkPermissionsAndLaunch() }
        videoViewBottom.setOnClickListener { checkPermissionsAndLaunch() }

        checkPermissionsAndLaunch()
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

    private fun checkPermissionsAndLaunch() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            launchVideoPicker()
        } else {
            requestPermissions.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun launchVideoPicker() {
        pickMedia.launch(ActivityResultContracts.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
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
        
        videoViewTop.setOnCompletionListener { it.start() }
        videoViewBottom.setOnCompletionListener { it.start() }
        
        isPlaying = true
    }
}

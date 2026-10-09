package com.example.videoloop

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var videoViewTop: FitVideoView
    private lateinit var videoViewBottom: FitVideoView
    private lateinit var selectButtonContainer: LinearLayout
    private lateinit var btnSelectVideo: Button
    
    private var isPlaying = false

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            launchVideoPicker()
        } else {
            Toast.makeText(this, "Permissions denied. App needs Storage/Camera access to work.", Toast.LENGTH_LONG).show()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectButtonContainer.visibility = View.GONE // Hide button when video is chosen
            playVideo(uri)
        } else {
            Toast.makeText(this, "No video selected. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoViewTop = findViewById(R.id.videoViewTop)
        videoViewBottom = findViewById(R.id.videoViewBottom)
        selectButtonContainer = findViewById(R.id.selectButtonContainer)
        btnSelectVideo = findViewById(R.id.btnSelectVideo)

        // Setup error listeners to prevent silent black screens
        setupErrorHandling(videoViewTop, "Top")
        setupErrorHandling(videoViewBottom, "Bottom")

        btnSelectVideo.setOnClickListener {
            checkPermissionsAndLaunch()
        }
        
        // Also allow tapping the video areas to change video later
        videoViewTop.setOnClickListener { checkPermissionsAndLaunch() }
        videoViewBottom.setOnClickListener { checkPermissionsAndLaunch() }

        // Check permissions on first launch
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
        pickMedia.launch("video/*")
    }

    private fun playVideo(uri: Uri) {
        try {
            // Grant temporary read permission for the URI
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // Ignore if already granted or not persistable
        }

        videoViewTop.setVideoURI(uri)
        videoViewBottom.setVideoURI(uri)

        videoViewTop.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(1f, 1f) // Ensure sound is on
            videoViewTop.start()
        }
        
        videoViewBottom.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(1f, 1f)
            videoViewBottom.start()
        }
        
        isPlaying = true
    }

    private fun setupErrorHandling(videoView: FitVideoView, position: String) {
        videoView.setOnErrorListener { _, what, extra ->
            Toast.makeText(this, "Error loading $position video (Code: $what, $extra). Try a different video.", Toast.LENGTH_LONG).show()
            selectButtonContainer.visibility = View.VISIBLE // Show button again on error
            isPlaying = false
            true
        }
    }
}

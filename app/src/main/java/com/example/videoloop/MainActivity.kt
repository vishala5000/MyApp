package com.example.videoloop

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private lateinit var playerViewTop: PlayerView
    private lateinit var playerViewBottom: PlayerView
    private lateinit var selectButtonContainer: LinearLayout
    private lateinit var btnSelectVideo: Button
    
    // We now use TWO separate players to render the video on both screens
    private var playerTop: ExoPlayer? = null
    private var playerBottom: ExoPlayer? = null
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
            Toast.makeText(this, "Permissions denied. App needs access to work.", Toast.LENGTH_LONG).show()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectButtonContainer.visibility = View.GONE
            playVideo(uri)
        } else {
            Toast.makeText(this, "No video selected. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // THIS KEEPS THE SCREEN ON WHILE THE APP IS OPEN
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerViewTop = findViewById(R.id.playerViewTop)
        playerViewBottom = findViewById(R.id.playerViewBottom)
        selectButtonContainer = findViewById(R.id.selectButtonContainer)
        btnSelectVideo = findViewById(R.id.btnSelectVideo)

        btnSelectVideo.setOnClickListener {
            checkPermissionsAndLaunch()
        }
        
        playerViewTop.setOnClickListener { checkPermissionsAndLaunch() }
        playerViewBottom.setOnClickListener { checkPermissionsAndLaunch() }

        checkPermissionsAndLaunch()
    }

    override fun onPause() {
        super.onPause()
        playerTop?.pause()
        playerBottom?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (isPlaying) {
            playerTop?.play()
            playerBottom?.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up both players to prevent memory leaks
        playerTop?.release()
        playerBottom?.release()
        playerTop = null
        playerBottom = null
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
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // Ignore
        }

        // Release previous players if the user selects a new video
        playerTop?.release()
        playerBottom?.release()

        // 1. Create TOP Player (Muted to prevent double audio echo)
        playerTop = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE // Endless loop
            playWhenReady = true
            volume = 0f 
            prepare()
        }
        
        // 2. Create BOTTOM Player (Audio enabled)
        playerBottom = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE // Endless loop
            playWhenReady = true
            volume = 1f
            prepare()
        }

        // Attach each player to its respective view
        playerViewTop.player = playerTop
        playerViewBottom.player = playerBottom
        
        isPlaying = true
    }
}

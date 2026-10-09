package com.example.videoloop

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var playerViewTop: PlayerView
    private lateinit var previewViewBottom: PreviewView
    private lateinit var selectButtonContainer: LinearLayout
    private lateinit var btnSelectVideo: Button
    
    private var exoPlayer: ExoPlayer? = null
    private lateinit var cameraExecutor: ExecutorService
    
    private var isVideoSelected = false

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeApp()
        } else {
            Toast.makeText(this, "Permissions denied. App needs Camera/Storage access.", Toast.LENGTH_LONG).show()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectButtonContainer.visibility = android.view.View.GONE
            isVideoSelected = true
            playVideo(uri)
            // Restart camera immediately after picker closes to prevent black screen
            startCamera() 
        } else {
            Toast.makeText(this, "No video selected. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // KEEP SCREEN ON
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerViewTop = findViewById(R.id.playerViewTop)
        previewViewBottom = findViewById(R.id.previewViewBottom)
        selectButtonContainer = findViewById(R.id.selectButtonContainer)
        btnSelectVideo = findViewById(R.id.btnSelectVideo)

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnSelectVideo.setOnClickListener {
            pickMedia.launch("video/*")
        }

        checkPermissions()
    }

    // THIS IS THE MAGIC FIX: Always restart the camera when the app becomes visible
    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) {
            startCamera()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            initializeApp()
        } else {
            requestPermissions.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun initializeApp() {
        if (!isVideoSelected) {
            pickMedia.launch("video/*")
        }
    }

    private fun playVideo(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) { }

        exoPlayer?.release()
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            volume = 1f
            prepare()
        }
        playerViewTop.player = exoPlayer
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                // CRITICAL: Unbind all previous states to guarantee no black screen
                cameraProvider.unbindAll()
                
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewViewBottom.surfaceProvider)
                }
                
                // Bind fresh camera to the current lifecycle
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        cameraExecutor.shutdown()
    }
}

package com.assound

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.assound.audio.service.AudioProcessingService
import com.assound.ui.navigation.ASsoundNavHost
import com.assound.ui.theme.SurfaceDark
import com.assound.ui.theme.ASsoundTheme
import com.assound.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startAudioService()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep splash screen on while loading
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }
        
        enableEdgeToEdge()
        
        setContent {
            ASsoundTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceDark
                ) {
                    ASsoundNavHost(viewModel = viewModel)
                }
                
                // Mark as ready after first composition
                LaunchedEffect(Unit) {
                    isReady = true
                    checkPermissionsAndStartService()
                }
            }
        }
    }
    
    private fun checkPermissionsAndStartService() {
        val permissions = buildList {
            // Notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            
            // Record audio for audio processing (needed for some effects)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
            }
        }
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isEmpty()) {
            startAudioService()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun startAudioService() {
        AudioProcessingService.startService(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Service continues running in background
    }
}
package com.assound.audio.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors audio sessions and playback state to automatically attach effects
 */
class AudioSessionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioSessionManager"
        
        // Broadcast action for audio becoming noisy (e.g., headphones unplugged)
        const val ACTION_AUDIO_BECOMING_NOISY = AudioManager.ACTION_AUDIO_BECOMING_NOISY
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val _activeSessions = MutableStateFlow<List<AudioSessionInfo>>(emptyList())
    val activeSessions: StateFlow<List<AudioSessionInfo>> = _activeSessions.asStateFlow()
    
    private val _outputDevice = MutableStateFlow(OutputDeviceType.SPEAKER)
    val outputDevice: StateFlow<OutputDeviceType> = _outputDevice.asStateFlow()
    
    private var playbackCallback: AudioManager.AudioPlaybackCallback? = null
    private var audioNoisyReceiver: BroadcastReceiver? = null
    
    data class AudioSessionInfo(
        val sessionId: Int,
        val packageName: String,
        val playerType: PlayerType,
        val isActive: Boolean,
        val contentType: ContentType
    )
    
    enum class PlayerType {
        MUSIC,
        VIDEO,
        GAME,
        VOICE,
        UNKNOWN
    }
    
    enum class ContentType {
        MUSIC,
        MOVIE,
        SONIFICATION,
        SPEECH,
        UNKNOWN
    }
    
    enum class OutputDeviceType {
        SPEAKER,
        WIRED_HEADPHONES,
        WIRED_HEADSET,
        BLUETOOTH_A2DP,
        BLUETOOTH_LE,
        USB_AUDIO,
        DOCK,
        HDMI,
        UNKNOWN
    }
    
    /**
     * Start monitoring audio sessions
     */
    fun startMonitoring(onSessionChanged: (List<AudioSessionInfo>) -> Unit) {
        // Register playback callback for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackCallback = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                    val sessions = configs?.map { config ->
                        AudioSessionInfo(
                            sessionId = config.audioAttributes.hashCode(), // Use hashcode as fallback
                            packageName = getPackageNameFromConfig(config),
                            playerType = getPlayerType(config),
                            isActive = true,
                            contentType = getContentType(config)
                        )
                    } ?: emptyList()
                    
                    _activeSessions.value = sessions
                    onSessionChanged(sessions)
                    
                    Log.d(TAG, "Playback config changed: ${sessions.size} active sessions")
                }
            }
            
            audioManager.registerAudioPlaybackCallback(playbackCallback!!, null)
        }
        
        // Register for audio becoming noisy (headphones unplugged)
        audioNoisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_AUDIO_BECOMING_NOISY) {
                    Log.d(TAG, "Audio becoming noisy - output device changed")
                    updateOutputDevice()
                }
            }
        }
        
        context.registerReceiver(
            audioNoisyReceiver,
            IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
        )
        
        // Initial output device detection
        updateOutputDevice()
        
        Log.d(TAG, "Audio session monitoring started")
    }
    
    /**
     * Stop monitoring audio sessions
     */
    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackCallback?.let {
                audioManager.unregisterAudioPlaybackCallback(it)
            }
        }
        
        audioNoisyReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Receiver already unregistered")
            }
        }
        
        playbackCallback = null
        audioNoisyReceiver = null
        
        Log.d(TAG, "Audio session monitoring stopped")
    }
    
    /**
     * Get current active playback sessions
     */
    fun getActivePlaybackConfigurations(): List<AudioSessionInfo> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val configs = audioManager.activePlaybackConfigurations
            return configs.map { config ->
                AudioSessionInfo(
                    sessionId = getSessionIdFromConfig(config),
                    packageName = getPackageNameFromConfig(config),
                    playerType = getPlayerType(config),
                    isActive = true,
                    contentType = getContentType(config)
                )
            }
        }
        return emptyList()
    }
    
    /**
     * Update current output device type
     */
    private fun updateOutputDevice() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        
        val outputType = when {
            devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP } -> 
                OutputDeviceType.BLUETOOTH_A2DP
            devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET } -> 
                OutputDeviceType.BLUETOOTH_LE
            devices.any { it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES } -> 
                OutputDeviceType.WIRED_HEADPHONES
            devices.any { it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET } -> 
                OutputDeviceType.WIRED_HEADSET
            devices.any { it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET || 
                         it.type == android.media.AudioDeviceInfo.TYPE_USB_DEVICE } -> 
                OutputDeviceType.USB_AUDIO
            devices.any { it.type == android.media.AudioDeviceInfo.TYPE_DOCK } -> 
                OutputDeviceType.DOCK
            devices.any { it.type == android.media.AudioDeviceInfo.TYPE_HDMI } -> 
                OutputDeviceType.HDMI
            else -> OutputDeviceType.SPEAKER
        }
        
        _outputDevice.value = outputType
        Log.d(TAG, "Output device: $outputType")
    }
    
    /**
     * Get current output device
     */
    fun getCurrentOutputDevice(): OutputDeviceType = _outputDevice.value
    
    /**
     * Check if headphones are connected
     */
    fun isHeadphonesConnected(): Boolean {
        val output = _outputDevice.value
        return output == OutputDeviceType.WIRED_HEADPHONES ||
               output == OutputDeviceType.WIRED_HEADSET ||
               output == OutputDeviceType.BLUETOOTH_A2DP ||
               output == OutputDeviceType.BLUETOOTH_LE ||
               output == OutputDeviceType.USB_AUDIO
    }
    
    private fun getSessionIdFromConfig(config: AudioPlaybackConfiguration): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On Android 10+, we can get more info
                config.hashCode()
            } else {
                config.audioAttributes.hashCode()
            }
        } catch (e: Exception) {
            config.hashCode()
        }
    }
    
    private fun getPackageNameFromConfig(config: AudioPlaybackConfiguration): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Try to get client UID and resolve package
                "unknown"
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getPlayerType(config: AudioPlaybackConfiguration): PlayerType {
        return when (config.audioAttributes.usage) {
            android.media.AudioAttributes.USAGE_MEDIA -> PlayerType.MUSIC
            android.media.AudioAttributes.USAGE_GAME -> PlayerType.GAME
            android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION -> PlayerType.VOICE
            android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> PlayerType.VOICE
            else -> PlayerType.UNKNOWN
        }
    }
    
    private fun getContentType(config: AudioPlaybackConfiguration): ContentType {
        return when (config.audioAttributes.contentType) {
            android.media.AudioAttributes.CONTENT_TYPE_MUSIC -> ContentType.MUSIC
            android.media.AudioAttributes.CONTENT_TYPE_MOVIE -> ContentType.MOVIE
            android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION -> ContentType.SONIFICATION
            android.media.AudioAttributes.CONTENT_TYPE_SPEECH -> ContentType.SPEECH
            else -> ContentType.UNKNOWN
        }
    }
}

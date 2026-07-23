package com.assound.audio.engine

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.*
import android.util.Log
import com.assound.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class AudioEngineController(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioEngineController"
        private const val PRIORITY = 1 // Higher priority than other equalizers
        
        // Standard 10-band frequencies
        val FREQUENCIES = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // Audio effects per session
    private val equalizers = ConcurrentHashMap<Int, Equalizer>()
    private val bassBoosts = ConcurrentHashMap<Int, BassBoost>()
    private val virtualizers = ConcurrentHashMap<Int, Virtualizer>()
    private val presetReverbs = ConcurrentHashMap<Int, PresetReverb>()
    private val loudnessEnhancers = ConcurrentHashMap<Int, LoudnessEnhancer>()
    
    // State flows
    private val _engineState = MutableStateFlow(EngineState())
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()
    
    private val _activeSessions = MutableStateFlow<Set<Int>>(emptySet())
    val activeSessions: StateFlow<Set<Int>> = _activeSessions.asStateFlow()
    
    private var currentGlobalSettings: GlobalAudioState = GlobalAudioState()
    private var currentProfile: SoundProfile? = null
    
    data class EngineState(
        val isInitialized: Boolean = false,
        val isEnabled: Boolean = false,
        val activeSessionCount: Int = 0,
        val error: String? = null,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    /**
     * Initialize audio effects for a specific audio session
     */
    fun initializeSession(sessionId: Int): Boolean {
        if (sessionId == 0) {
            // Session 0 is the global output mix
            return initializeGlobalSession()
        }
        
        return try {
            createEqualizerForSession(sessionId)
            createBassBoostForSession(sessionId)
            createVirtualizerForSession(sessionId)
            createReverbForSession(sessionId)
            createLoudnessEnhancerForSession(sessionId)
            
            _activeSessions.value = _activeSessions.value + sessionId
            updateEngineState()
            
            Log.d(TAG, "Session $sessionId initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize session $sessionId", e)
            releaseSession(sessionId)
            false
        }
    }
    
    private fun initializeGlobalSession(): Boolean {
        return try {
            // Try to attach to global output mix (session 0)
            createEqualizerForSession(0)
            createBassBoostForSession(0)
            createVirtualizerForSession(0)
            createLoudnessEnhancerForSession(0)
            
            _activeSessions.value = _activeSessions.value + 0
            _engineState.value = _engineState.value.copy(
                isInitialized = true,
                isEnabled = true
            )
            
            Log.d(TAG, "Global session initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize global session", e)
            _engineState.value = _engineState.value.copy(
                error = "Cannot access global audio: ${e.message}"
            )
            false
        }
    }
    
    private fun createEqualizerForSession(sessionId: Int) {
        if (equalizers.containsKey(sessionId)) return
        
        val eq = Equalizer(PRIORITY, sessionId).apply {
            enabled = true
        }
        equalizers[sessionId] = eq
        Log.d(TAG, "Equalizer created for session $sessionId, bands: ${eq.numberOfBands}")
    }
    
    private fun createBassBoostForSession(sessionId: Int) {
        if (bassBoosts.containsKey(sessionId)) return
        
        try {
            val bb = BassBoost(PRIORITY, sessionId).apply {
                if (strengthSupported) {
                    enabled = false
                }
            }
            bassBoosts[sessionId] = bb
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost not supported for session $sessionId")
        }
    }
    
    private fun createVirtualizerForSession(sessionId: Int) {
        if (virtualizers.containsKey(sessionId)) return
        
        try {
            val virt = Virtualizer(PRIORITY, sessionId).apply {
                if (strengthSupported) {
                    enabled = false
                }
            }
            virtualizers[sessionId] = virt
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer not supported for session $sessionId")
        }
    }
    
    private fun createReverbForSession(sessionId: Int) {
        if (presetReverbs.containsKey(sessionId)) return
        
        try {
            val reverb = PresetReverb(PRIORITY, sessionId).apply {
                preset = PresetReverb.PRESET_NONE
                enabled = false
            }
            presetReverbs[sessionId] = reverb
        } catch (e: Exception) {
            Log.w(TAG, "PresetReverb not supported for session $sessionId")
        }
    }
    
    private fun createLoudnessEnhancerForSession(sessionId: Int) {
        if (loudnessEnhancers.containsKey(sessionId)) return
        
        try {
            val le = LoudnessEnhancer(sessionId).apply {
                enabled = false
            }
            loudnessEnhancers[sessionId] = le
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer not supported for session $sessionId")
        }
    }
    
    /**
     * Apply equalizer bands to a session
     */
    fun applyEqualizerBands(sessionId: Int, bands: List<Float>): Boolean {
        val eq = equalizers[sessionId] ?: return false
        
        return try {
            val numBands = eq.numberOfBands.toInt()
            val bandRange = eq.bandLevelRange
            val minLevel = bandRange[0]
            val maxLevel = bandRange[1]
            
            bands.take(numBands).forEachIndexed { index, dbValue ->
                // Convert dB to millibels and clamp to valid range
                val mbValue = (dbValue * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                eq.setBandLevel(index.toShort(), mbValue)
            }
            
            eq.enabled = true
            Log.d(TAG, "Applied EQ bands to session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply EQ to session $sessionId", e)
            false
        }
    }
    
    /**
     * Apply equalizer bands to all active sessions
     */
    fun applyEqualizerToAll(bands: List<Float>) {
        _activeSessions.value.forEach { sessionId ->
            applyEqualizerBands(sessionId, bands)
        }
    }
    
    /**
     * Set bass boost strength (0-1000)
     */
    fun setBassBoost(sessionId: Int, strength: Int): Boolean {
        val bb = bassBoosts[sessionId] ?: return false
        
        return try {
            if (bb.strengthSupported) {
                bb.setStrength(strength.coerceIn(0, 1000).toShort())
                bb.enabled = strength > 0
                Log.d(TAG, "Bass boost set to $strength for session $sessionId")
                true
            } else {
                Log.w(TAG, "Bass boost strength not supported")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set bass boost", e)
            false
        }
    }
    
    /**
     * Set virtualizer strength (0-1000)
     */
    fun setVirtualizer(sessionId: Int, strength: Int): Boolean {
        val virt = virtualizers[sessionId] ?: return false
        
        return try {
            if (virt.strengthSupported) {
                virt.setStrength(strength.coerceIn(0, 1000).toShort())
                virt.enabled = strength > 0
                Log.d(TAG, "Virtualizer set to $strength for session $sessionId")
                true
            } else {
                Log.w(TAG, "Virtualizer strength not supported")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set virtualizer", e)
            false
        }
    }
    
    /**
     * Set reverb preset
     */
    fun setReverb(sessionId: Int, preset: ReverbPreset, strength: Int): Boolean {
        val reverb = presetReverbs[sessionId] ?: return false
        
        return try {
            val androidPreset = when (preset) {
                ReverbPreset.NONE -> PresetReverb.PRESET_NONE
                ReverbPreset.SMALL_ROOM -> PresetReverb.PRESET_SMALLROOM
                ReverbPreset.MEDIUM_ROOM -> PresetReverb.PRESET_MEDIUMROOM
                ReverbPreset.LARGE_ROOM -> PresetReverb.PRESET_LARGEROOM
                ReverbPreset.MEDIUM_HALL -> PresetReverb.PRESET_MEDIUMHALL
                ReverbPreset.LARGE_HALL -> PresetReverb.PRESET_LARGEHALL
                ReverbPreset.PLATE -> PresetReverb.PRESET_PLATE
            }
            
            reverb.preset = androidPreset.toShort()
            reverb.enabled = preset != ReverbPreset.NONE && strength > 0
            Log.d(TAG, "Reverb set to ${preset.name} for session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set reverb", e)
            false
        }
    }
    
    /**
     * Set loudness enhancer gain in millibels
     */
    fun setLoudnessEnhancer(sessionId: Int, gainMb: Int): Boolean {
        val le = loudnessEnhancers[sessionId] ?: return false
        
        return try {
            le.setTargetGain(gainMb)
            le.enabled = gainMb > 0
            Log.d(TAG, "Loudness enhancer set to ${gainMb}mB for session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set loudness enhancer", e)
            false
        }
    }
    
    /**
     * Apply a complete sound profile to a session
     */
    fun applySoundProfile(sessionId: Int, profile: SoundProfile): Boolean {
        currentProfile = profile
        
        var success = true
        
        success = applyEqualizerBands(sessionId, profile.equalizerBands) && success
        success = setBassBoost(sessionId, profile.bassBoost) && success
        success = setVirtualizer(sessionId, profile.virtualizerStrength) && success
        success = setReverb(sessionId, profile.reverbPreset, profile.reverbStrength) && success
        success = setLoudnessEnhancer(sessionId, profile.loudnessGain) && success
        
        Log.d(TAG, "Applied profile ${profile.name} to session $sessionId: $success")
        return success
    }
    
    /**
     * Apply sound profile to all active sessions
     */
    fun applySoundProfileToAll(profile: SoundProfile) {
        currentProfile = profile
        _activeSessions.value.forEach { sessionId ->
            applySoundProfile(sessionId, profile)
        }
    }
    
    /**
     * Apply advanced settings to a session
     */
    fun applyAdvancedSettings(sessionId: Int, settings: AdvancedAudioSettings): Boolean {
        var success = true
        
        // Bass Boost
        if (settings.bassBoostEnabled) {
            success = setBassBoost(sessionId, settings.bassBoostStrength) && success
        } else {
            setBassBoost(sessionId, 0)
        }
        
        // Virtualizer
        if (settings.virtualizerEnabled) {
            success = setVirtualizer(sessionId, settings.virtualizerStrength) && success
        } else {
            setVirtualizer(sessionId, 0)
        }
        
        // Reverb
        if (settings.reverbEnabled) {
            success = setReverb(sessionId, settings.reverbPreset, settings.reverbStrength) && success
        } else {
            setReverb(sessionId, ReverbPreset.NONE, 0)
        }
        
        // Loudness
        if (settings.loudnessEnhancerEnabled) {
            success = setLoudnessEnhancer(sessionId, settings.loudnessTargetGain) && success
        } else {
            setLoudnessEnhancer(sessionId, 0)
        }
        
        return success
    }
    
    /**
     * Enable or disable all effects for a session
     */
    fun setSessionEnabled(sessionId: Int, enabled: Boolean) {
        equalizers[sessionId]?.enabled = enabled
        bassBoosts[sessionId]?.enabled = enabled && (bassBoosts[sessionId]?.roundedStrength ?: 0) > 0
        virtualizers[sessionId]?.enabled = enabled && (virtualizers[sessionId]?.roundedStrength ?: 0) > 0
        presetReverbs[sessionId]?.enabled = enabled
        loudnessEnhancers[sessionId]?.enabled = enabled
        
        updateEngineState()
    }
    
    /**
     * Enable or disable all sessions
     */
    fun setMasterEnabled(enabled: Boolean) {
        _activeSessions.value.forEach { sessionId ->
            setSessionEnabled(sessionId, enabled)
        }
        _engineState.value = _engineState.value.copy(isEnabled = enabled)
    }
    
    /**
     * Get current EQ band levels for a session
     */
    fun getEqualizerBands(sessionId: Int): List<Float>? {
        val eq = equalizers[sessionId] ?: return null
        
        return try {
            (0 until eq.numberOfBands).map { band ->
                eq.getBandLevel(band.toShort()).toFloat() / 100f // millibels to dB
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get EQ bands", e)
            null
        }
    }
    
    /**
     * Get supported band level range
     */
    fun getBandLevelRange(sessionId: Int): Pair<Int, Int>? {
        val eq = equalizers[sessionId] ?: return null
        
        return try {
            val range = eq.bandLevelRange
            Pair(range[0].toInt() / 100, range[1].toInt() / 100) // millibels to dB
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get center frequencies for all bands
     */
    fun getCenterFrequencies(sessionId: Int): List<Int>? {
        val eq = equalizers[sessionId] ?: return null
        
        return try {
            (0 until eq.numberOfBands).map { band ->
                eq.getCenterFreq(band.toShort()) / 1000 // milliHz to Hz
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get frequencies", e)
            null
        }
    }
    
    /**
     * Release effects for a specific session
     */
    fun releaseSession(sessionId: Int) {
        try {
            equalizers.remove(sessionId)?.release()
            bassBoosts.remove(sessionId)?.release()
            virtualizers.remove(sessionId)?.release()
            presetReverbs.remove(sessionId)?.release()
            loudnessEnhancers.remove(sessionId)?.release()
            
            _activeSessions.value = _activeSessions.value - sessionId
            updateEngineState()
            
            Log.d(TAG, "Session $sessionId released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing session $sessionId", e)
        }
    }
    
    /**
     * Release all effects
     */
    fun releaseAll() {
        _activeSessions.value.toList().forEach { sessionId ->
            releaseSession(sessionId)
        }
        
        _engineState.value = EngineState()
        Log.d(TAG, "All sessions released")
    }
    
    private fun updateEngineState() {
        _engineState.value = _engineState.value.copy(
            activeSessionCount = _activeSessions.value.size,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Check if device supports required audio effects
     */
    fun checkDeviceCapabilities(): DeviceCapabilities {
        val descriptors = AudioEffect.queryEffects()
        
        var hasEqualizer = false
        var hasBassBoost = false
        var hasVirtualizer = false
        var hasReverb = false
        var hasLoudness = false
        
        descriptors?.forEach { descriptor ->
            when (descriptor.type) {
                AudioEffect.EFFECT_TYPE_EQUALIZER -> hasEqualizer = true
                AudioEffect.EFFECT_TYPE_BASS_BOOST -> hasBassBoost = true
                AudioEffect.EFFECT_TYPE_VIRTUALIZER -> hasVirtualizer = true
                AudioEffect.EFFECT_TYPE_PRESET_REVERB -> hasReverb = true
                AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER -> hasLoudness = true
            }
        }
        
        return DeviceCapabilities(
            hasEqualizer = hasEqualizer,
            hasBassBoost = hasBassBoost,
            hasVirtualizer = hasVirtualizer,
            hasReverb = hasReverb,
            hasLoudnessEnhancer = hasLoudness
        )
    }
    
    data class DeviceCapabilities(
        val hasEqualizer: Boolean,
        val hasBassBoost: Boolean,
        val hasVirtualizer: Boolean,
        val hasReverb: Boolean,
        val hasLoudnessEnhancer: Boolean
    ) {
        val isFullySupported: Boolean
            get() = hasEqualizer && hasBassBoost && hasVirtualizer
    }
}

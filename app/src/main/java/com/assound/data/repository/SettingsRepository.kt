package com.assound.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.assound.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "assound_settings")

class SettingsRepository(private val context: Context) {
    
    private val gson = Gson()
    
    companion object {
        // Master settings
        private val KEY_MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        private val KEY_MASTER_VOLUME = floatPreferencesKey("master_volume")
        
        // Current profile
        private val KEY_CURRENT_PROFILE_ID = stringPreferencesKey("current_profile_id")
        private val KEY_USING_CUSTOM_EQ = booleanPreferencesKey("using_custom_eq")
        
        // Custom EQ bands
        private val KEY_CUSTOM_EQ_BANDS = stringPreferencesKey("custom_eq_bands")
        
        // Advanced settings
        private val KEY_ADVANCED_SETTINGS = stringPreferencesKey("advanced_settings")
        
        // Bass Boost
        private val KEY_BASS_BOOST_ENABLED = booleanPreferencesKey("bass_boost_enabled")
        private val KEY_BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        
        // Virtualizer
        private val KEY_VIRTUALIZER_ENABLED = booleanPreferencesKey("virtualizer_enabled")
        private val KEY_VIRTUALIZER_STRENGTH = intPreferencesKey("virtualizer_strength")
        private val KEY_VIRTUALIZER_MODE = stringPreferencesKey("virtualizer_mode")
        
        // Reverb
        private val KEY_REVERB_ENABLED = booleanPreferencesKey("reverb_enabled")
        private val KEY_REVERB_PRESET = stringPreferencesKey("reverb_preset")
        private val KEY_REVERB_STRENGTH = intPreferencesKey("reverb_strength")
        
        // Loudness
        private val KEY_LOUDNESS_ENABLED = booleanPreferencesKey("loudness_enabled")
        private val KEY_LOUDNESS_GAIN = intPreferencesKey("loudness_gain")
        
        // Stereo
        private val KEY_STEREO_WIDTH_ENABLED = booleanPreferencesKey("stereo_width_enabled")
        private val KEY_STEREO_WIDTH = floatPreferencesKey("stereo_width")
        
        // Compressor
        private val KEY_COMPRESSOR_ENABLED = booleanPreferencesKey("compressor_enabled")
        private val KEY_COMPRESSION_RATIO = floatPreferencesKey("compression_ratio")
        private val KEY_COMPRESSION_THRESHOLD = floatPreferencesKey("compression_threshold")
        
        // Dialog Enhancement
        private val KEY_DIALOG_ENABLED = booleanPreferencesKey("dialog_enhancement_enabled")
        private val KEY_DIALOG_LEVEL = floatPreferencesKey("dialog_enhancement_level")
        
        // Output Mode
        private val KEY_OUTPUT_MODE = stringPreferencesKey("output_mode")
        
        // Volume Leveling
        private val KEY_VOLUME_LEVELING_ENABLED = booleanPreferencesKey("volume_leveling_enabled")
        private val KEY_VOLUME_LEVELING_TARGET = floatPreferencesKey("volume_leveling_target")
        
        // Custom profiles
        private val KEY_CUSTOM_PROFILES = stringPreferencesKey("custom_profiles")
        
        // App settings
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_AUTO_START = booleanPreferencesKey("auto_start")
        private val KEY_SHOW_VISUALIZER = booleanPreferencesKey("show_visualizer")
        private val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }
    
    // Global Audio State
    val globalAudioState: Flow<GlobalAudioState> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val customBands = prefs[KEY_CUSTOM_EQ_BANDS]?.let {
                try {
                    gson.fromJson<List<Float>>(it, object : TypeToken<List<Float>>() {}.type)
                } catch (e: Exception) {
                    listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                }
            } ?: listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            
            GlobalAudioState(
                isMasterEnabled = prefs[KEY_MASTER_ENABLED] ?: true,
                masterVolume = prefs[KEY_MASTER_VOLUME] ?: 1.0f,
                currentProfileId = prefs[KEY_CURRENT_PROFILE_ID],
                isUsingCustomEq = prefs[KEY_USING_CUSTOM_EQ] ?: false,
                advancedSettings = getAdvancedSettingsFromPrefs(prefs),
                customEqualizerBands = customBands
            )
        }
    
    private fun getAdvancedSettingsFromPrefs(prefs: Preferences): AdvancedAudioSettings {
        return AdvancedAudioSettings(
            bassBoostEnabled = prefs[KEY_BASS_BOOST_ENABLED] ?: false,
            bassBoostStrength = prefs[KEY_BASS_BOOST_STRENGTH] ?: 500,
            virtualizerEnabled = prefs[KEY_VIRTUALIZER_ENABLED] ?: false,
            virtualizerStrength = prefs[KEY_VIRTUALIZER_STRENGTH] ?: 500,
            virtualizerMode = prefs[KEY_VIRTUALIZER_MODE]?.let {
                try { VirtualizerMode.valueOf(it) } catch (e: Exception) { VirtualizerMode.AUTO }
            } ?: VirtualizerMode.AUTO,
            reverbEnabled = prefs[KEY_REVERB_ENABLED] ?: false,
            reverbPreset = prefs[KEY_REVERB_PRESET]?.let {
                try { ReverbPreset.valueOf(it) } catch (e: Exception) { ReverbPreset.NONE }
            } ?: ReverbPreset.NONE,
            reverbStrength = prefs[KEY_REVERB_STRENGTH] ?: 300,
            loudnessEnhancerEnabled = prefs[KEY_LOUDNESS_ENABLED] ?: false,
            loudnessTargetGain = prefs[KEY_LOUDNESS_GAIN] ?: 500,
            stereoWidthEnabled = prefs[KEY_STEREO_WIDTH_ENABLED] ?: false,
            stereoWidth = prefs[KEY_STEREO_WIDTH] ?: 1.0f,
            compressorEnabled = prefs[KEY_COMPRESSOR_ENABLED] ?: false,
            compressionRatio = prefs[KEY_COMPRESSION_RATIO] ?: 1.0f,
            compressionThreshold = prefs[KEY_COMPRESSION_THRESHOLD] ?: -20f,
            dialogEnhancementEnabled = prefs[KEY_DIALOG_ENABLED] ?: false,
            dialogEnhancementLevel = prefs[KEY_DIALOG_LEVEL] ?: 0.5f,
            outputMode = prefs[KEY_OUTPUT_MODE]?.let {
                try { OutputMode.valueOf(it) } catch (e: Exception) { OutputMode.AUTO }
            } ?: OutputMode.AUTO,
            volumeLevelingEnabled = prefs[KEY_VOLUME_LEVELING_ENABLED] ?: false,
            volumeLevelingTarget = prefs[KEY_VOLUME_LEVELING_TARGET] ?: -14f
        )
    }
    
    suspend fun setMasterEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MASTER_ENABLED] = enabled
        }
    }
    
    suspend fun setMasterVolume(volume: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MASTER_VOLUME] = volume.coerceIn(0f, 1f)
        }
    }
    
    suspend fun setCurrentProfileId(profileId: String?) {
        context.dataStore.edit { prefs ->
            if (profileId != null) {
                prefs[KEY_CURRENT_PROFILE_ID] = profileId
            } else {
                prefs.remove(KEY_CURRENT_PROFILE_ID)
            }
        }
    }
    
    suspend fun setUsingCustomEq(using: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USING_CUSTOM_EQ] = using
        }
    }
    
    suspend fun setCustomEqualizerBands(bands: List<Float>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_EQ_BANDS] = gson.toJson(bands)
        }
    }
    
    // Advanced Settings
    
    suspend fun setBassBoost(enabled: Boolean, strength: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASS_BOOST_ENABLED] = enabled
            prefs[KEY_BASS_BOOST_STRENGTH] = strength.coerceIn(0, 1000)
        }
    }
    
    suspend fun setVirtualizer(enabled: Boolean, strength: Int, mode: VirtualizerMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIRTUALIZER_ENABLED] = enabled
            prefs[KEY_VIRTUALIZER_STRENGTH] = strength.coerceIn(0, 1000)
            prefs[KEY_VIRTUALIZER_MODE] = mode.name
        }
    }
    
    suspend fun setReverb(enabled: Boolean, preset: ReverbPreset, strength: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REVERB_ENABLED] = enabled
            prefs[KEY_REVERB_PRESET] = preset.name
            prefs[KEY_REVERB_STRENGTH] = strength.coerceIn(0, 1000)
        }
    }
    
    suspend fun setLoudnessEnhancer(enabled: Boolean, gain: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOUDNESS_ENABLED] = enabled
            prefs[KEY_LOUDNESS_GAIN] = gain.coerceIn(0, 1000)
        }
    }
    
    suspend fun setStereoWidth(enabled: Boolean, width: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STEREO_WIDTH_ENABLED] = enabled
            prefs[KEY_STEREO_WIDTH] = width.coerceIn(0.5f, 2.0f)
        }
    }
    
    suspend fun setCompressor(enabled: Boolean, ratio: Float, threshold: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COMPRESSOR_ENABLED] = enabled
            prefs[KEY_COMPRESSION_RATIO] = ratio.coerceIn(1f, 8f)
            prefs[KEY_COMPRESSION_THRESHOLD] = threshold.coerceIn(-60f, 0f)
        }
    }
    
    suspend fun setDialogEnhancement(enabled: Boolean, level: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DIALOG_ENABLED] = enabled
            prefs[KEY_DIALOG_LEVEL] = level.coerceIn(0f, 1f)
        }
    }
    
    suspend fun setOutputMode(mode: OutputMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OUTPUT_MODE] = mode.name
        }
    }
    
    suspend fun setVolumeLeveling(enabled: Boolean, target: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VOLUME_LEVELING_ENABLED] = enabled
            prefs[KEY_VOLUME_LEVELING_TARGET] = target.coerceIn(-24f, 0f)
        }
    }
    
    // Custom Profiles
    
    val customProfiles: Flow<List<SoundProfile>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            prefs[KEY_CUSTOM_PROFILES]?.let {
                try {
                    gson.fromJson<List<SoundProfile>>(it, object : TypeToken<List<SoundProfile>>() {}.type)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
    
    suspend fun saveCustomProfile(profile: SoundProfile) {
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_CUSTOM_PROFILES]?.let {
                try {
                    gson.fromJson<List<SoundProfile>>(it, object : TypeToken<List<SoundProfile>>() {}.type)
                        .toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } ?: mutableListOf()
            
            // Update or add
            val index = existing.indexOfFirst { it.id == profile.id }
            if (index >= 0) {
                existing[index] = profile
            } else {
                existing.add(profile)
            }
            
            prefs[KEY_CUSTOM_PROFILES] = gson.toJson(existing)
        }
    }
    
    suspend fun deleteCustomProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_CUSTOM_PROFILES]?.let {
                try {
                    gson.fromJson<List<SoundProfile>>(it, object : TypeToken<List<SoundProfile>>() {}.type)
                        .filter { it.id != profileId }
                } catch (e: Exception) {
                    emptyList<SoundProfile>()
                }
            } ?: emptyList()
            
            prefs[KEY_CUSTOM_PROFILES] = gson.toJson(existing)
        }
    }
    
    // App Settings
    
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_FIRST_LAUNCH] ?: true }
    
    val autoStart: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_AUTO_START] ?: true }
    
    val showVisualizer: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_SHOW_VISUALIZER] ?: true }
    
    val hapticFeedback: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_HAPTIC_FEEDBACK] ?: true }
    
    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH] = false
        }
    }
    
    suspend fun setAutoStart(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_START] = enabled
        }
    }
    
    suspend fun setShowVisualizer(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_VISUALIZER] = show
        }
    }
    
    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAPTIC_FEEDBACK] = enabled
        }
    }
    
    // Clear all data
    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

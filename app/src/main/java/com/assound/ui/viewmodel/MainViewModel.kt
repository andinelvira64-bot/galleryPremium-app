package com.assound.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assound.audio.engine.AudioEngineController
import com.assound.audio.engine.AudioSessionManager
import com.assound.audio.service.AudioProcessingService
import com.assound.data.model.*
import com.assound.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val settingsRepository = SettingsRepository(application)
    
    // Service binding
    private var audioService: AudioProcessingService? = null
    private var isServiceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioProcessingService.LocalBinder
            audioService = binder?.getService()
            isServiceBound = true
            
            viewModelScope.launch {
                audioService?.serviceState?.collect { state ->
                    _serviceState.value = state
                }
            }
            
            // Apply current profile if any
            _uiState.value.selectedProfile?.let { profile ->
                audioService?.applyProfile(profile)
            }
            
            Log.d(TAG, "Service connected")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            isServiceBound = false
            Log.d(TAG, "Service disconnected")
        }
    }
    
    // UI State
    private val _uiState = MutableStateFlow(ASsoundUiState())
    val uiState: StateFlow<ASsoundUiState> = _uiState.asStateFlow()
    
    private val _serviceState = MutableStateFlow(AudioProcessingService.ServiceState())
    val serviceState: StateFlow<AudioProcessingService.ServiceState> = _serviceState.asStateFlow()
    
    // EQ State
    private val _equalizerBands = MutableStateFlow(List(10) { 0f })
    val equalizerBands: StateFlow<List<Float>> = _equalizerBands.asStateFlow()
    
    // Advanced Settings State
    private val _advancedSettings = MutableStateFlow(AdvancedAudioSettings())
    val advancedSettings: StateFlow<AdvancedAudioSettings> = _advancedSettings.asStateFlow()
    
    data class ASsoundUiState(
        val isLoading: Boolean = true,
        val isMasterEnabled: Boolean = true,
        val selectedProfile: SoundProfile? = null,
        val isUsingCustomEq: Boolean = false,
        val allProfiles: List<SoundProfile> = SoundProfiles.allProfiles,
        val customProfiles: List<SoundProfile> = emptyList(),
        val outputDevice: AudioSessionManager.OutputDeviceType = AudioSessionManager.OutputDeviceType.SPEAKER,
        val deviceCapabilities: AudioEngineController.DeviceCapabilities? = null,
        val showVisualizer: Boolean = true,
        val hapticEnabled: Boolean = true,
        val error: String? = null
    )
    
    init {
        loadSettings()
        bindService()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            // Load global audio state
            settingsRepository.globalAudioState.collect { state ->
                _uiState.update { ui ->
                    ui.copy(
                        isLoading = false,
                        isMasterEnabled = state.isMasterEnabled,
                        isUsingCustomEq = state.isUsingCustomEq,
                        selectedProfile = state.currentProfileId?.let { id ->
                            SoundProfiles.getProfileById(id)
                        }
                    )
                }
                _equalizerBands.value = state.customEqualizerBands
                _advancedSettings.value = state.advancedSettings
            }
        }
        
        viewModelScope.launch {
            settingsRepository.customProfiles.collect { profiles ->
                _uiState.update { it.copy(customProfiles = profiles) }
            }
        }
        
        viewModelScope.launch {
            settingsRepository.showVisualizer.collect { show ->
                _uiState.update { it.copy(showVisualizer = show) }
            }
        }
        
        viewModelScope.launch {
            settingsRepository.hapticFeedback.collect { enabled ->
                _uiState.update { it.copy(hapticEnabled = enabled) }
            }
        }
    }
    
    private fun bindService() {
        val context = getApplication<Application>()
        val intent = Intent(context, AudioProcessingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    fun startAudioService() {
        val context = getApplication<Application>()
        AudioProcessingService.startService(context)
    }
    
    fun stopAudioService() {
        val context = getApplication<Application>()
        AudioProcessingService.stopService(context)
    }
    
    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMasterEnabled(enabled)
            audioService?.setEnabled(enabled)
            _uiState.update { it.copy(isMasterEnabled = enabled) }
        }
    }
    
    fun selectProfile(profile: SoundProfile?) {
        viewModelScope.launch {
            settingsRepository.setCurrentProfileId(profile?.id)
            settingsRepository.setUsingCustomEq(false)
            
            _uiState.update { it.copy(
                selectedProfile = profile,
                isUsingCustomEq = false
            )}
            
            if (profile != null) {
                _equalizerBands.value = profile.equalizerBands
                audioService?.applyProfile(profile)
            }
        }
    }
    
    fun setEqualizerBand(bandIndex: Int, value: Float) {
        val newBands = _equalizerBands.value.toMutableList()
        newBands[bandIndex] = value.coerceIn(-15f, 15f)
        _equalizerBands.value = newBands
        
        viewModelScope.launch {
            settingsRepository.setCustomEqualizerBands(newBands)
            settingsRepository.setUsingCustomEq(true)
            _uiState.update { it.copy(isUsingCustomEq = true) }
            
            audioService?.applyEqualizerBands(newBands)
        }
    }
    
    fun resetEqualizer() {
        val flatBands = List(10) { 0f }
        _equalizerBands.value = flatBands
        
        viewModelScope.launch {
            settingsRepository.setCustomEqualizerBands(flatBands)
            audioService?.applyEqualizerBands(flatBands)
        }
    }
    
    fun applyPreset(preset: EqualizerPreset) {
        val bands = preset.bands.map { it.coerceIn(-15f, 15f) }
        _equalizerBands.value = bands
        
        viewModelScope.launch {
            settingsRepository.setCustomEqualizerBands(bands)
            settingsRepository.setUsingCustomEq(true)
            _uiState.update { it.copy(isUsingCustomEq = true, selectedProfile = null) }
            settingsRepository.setCurrentProfileId(null)
            
            audioService?.applyEqualizerBands(bands)
        }
    }
    
    // Advanced Settings
    
    fun setBassBoost(enabled: Boolean, strength: Int) {
        val newSettings = _advancedSettings.value.copy(
            bassBoostEnabled = enabled,
            bassBoostStrength = strength
        )
        _advancedSettings.value = newSettings
        
        viewModelScope.launch {
            settingsRepository.setBassBoost(enabled, strength)
            audioService?.applyAdvancedSettings(newSettings)
        }
    }
    
    fun setVirtualizer(enabled: Boolean, strength: Int, mode: VirtualizerMode = VirtualizerMode.AUTO) {
        val newSettings = _advancedSettings.value.copy(
            virtualizerEnabled = enabled,
            virtualizerStrength = strength,
            virtualizerMode = mode
        )
        _advancedSettings.value = newSettings
        
        viewModelScope.launch {
            settingsRepository.setVirtualizer(enabled, strength, mode)
            audioService?.applyAdvancedSettings(newSettings)
        }
    }
    
    fun setReverb(enabled: Boolean, preset: ReverbPreset, strength: Int) {
        val newSettings = _advancedSettings.value.copy(
            reverbEnabled = enabled,
            reverbPreset = preset,
            reverbStrength = strength
        )
        _advancedSettings.value = newSettings
        
        viewModelScope.launch {
            settingsRepository.setReverb(enabled, preset, strength)
            audioService?.applyAdvancedSettings(newSettings)
        }
    }
    
    fun setLoudnessEnhancer(enabled: Boolean, gain: Int) {
        val newSettings = _advancedSettings.value.copy(
            loudnessEnhancerEnabled = enabled,
            loudnessTargetGain = gain
        )
        _advancedSettings.value = newSettings
        
        viewModelScope.launch {
            settingsRepository.setLoudnessEnhancer(enabled, gain)
            audioService?.applyAdvancedSettings(newSettings)
        }
    }
    
    fun setStereoWidth(enabled: Boolean, width: Float) {
        val newSettings = _advancedSettings.value.copy(
            stereoWidthEnabled = enabled,
            stereoWidth = width
        )
        _advancedSettings.value = newSettings
        
        viewModelScope.launch {
            settingsRepository.setStereoWidth(enabled, width)
            audioService?.applyAdvancedSettings(newSettings)
        }
    }
    
    fun setDialogEnhancement(enabled: Boolean, level: Float) {
        val newSettings = _advancedSettings.value.copy(
            dialogEnhancementEnabled = enabled,
            dialogEnhancementLevel = level
        )
        _advancedSettings.value = newSettings
        
        viewModelScope.launch {
            settingsRepository.setDialogEnhancement(enabled, level)
            audioService?.applyAdvancedSettings(newSettings)
        }
    }
    
    fun setOutputMode(mode: OutputMode) {
        val newSettings = _advancedSettings.value.copy(outputMode = mode)
        _advancedSettings.value = newSettings
        
        viewModelScope.launch {
            settingsRepository.setOutputMode(mode)
        }
    }
    
    // Custom Profile Management
    
    fun saveCurrentAsCustomProfile(name: String) {
        val customProfile = SoundProfile(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            brandName = "Custom",
            description = "Custom created profile",
            category = ProfileCategory.CUSTOM,
            equalizerBands = _equalizerBands.value,
            bassBoost = if (_advancedSettings.value.bassBoostEnabled) _advancedSettings.value.bassBoostStrength else 0,
            virtualizerStrength = if (_advancedSettings.value.virtualizerEnabled) _advancedSettings.value.virtualizerStrength else 0,
            reverbPreset = _advancedSettings.value.reverbPreset,
            reverbStrength = if (_advancedSettings.value.reverbEnabled) _advancedSettings.value.reverbStrength else 0,
            loudnessGain = if (_advancedSettings.value.loudnessEnhancerEnabled) _advancedSettings.value.loudnessTargetGain else 0,
            isCustom = true
        )
        
        viewModelScope.launch {
            settingsRepository.saveCustomProfile(customProfile)
        }
    }
    
    fun deleteCustomProfile(profileId: String) {
        viewModelScope.launch {
            settingsRepository.deleteCustomProfile(profileId)
            
            // If currently selected, deselect
            if (_uiState.value.selectedProfile?.id == profileId) {
                selectProfile(null)
            }
        }
    }
    
    // App Settings
    
    fun setShowVisualizer(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowVisualizer(show)
        }
    }
    
    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticFeedback(enabled)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        if (isServiceBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding service", e)
            }
        }
    }
}

package com.assound.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioEffectState(
    val isEnabled: Boolean = false,
    val strength: Int = 0 // 0-1000
)

@Serializable
data class AdvancedAudioSettings(
    // Bass Enhancement
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 500, // 0-1000
    val bassFrequencyCutoff: Int = 150, // Hz, where bass boost starts to roll off
    
    // Virtualizer / 3D Audio
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 500, // 0-1000
    val virtualizerMode: VirtualizerMode = VirtualizerMode.AUTO,
    
    // Reverb
    val reverbEnabled: Boolean = false,
    val reverbPreset: ReverbPreset = ReverbPreset.NONE,
    val reverbStrength: Int = 300, // 0-1000
    val reverbDecayTime: Float = 1.5f, // seconds
    val reverbDiffusion: Int = 500, // 0-1000
    
    // Loudness / Volume Enhancement
    val loudnessEnhancerEnabled: Boolean = false,
    val loudnessTargetGain: Int = 500, // in mB (millibels), typical range -1000 to 1000
    
    // Stereo Width
    val stereoWidthEnabled: Boolean = false,
    val stereoWidth: Float = 1.0f, // 0.5 = mono, 1.0 = normal, 2.0 = wide
    
    // Dynamic Range Compression
    val compressorEnabled: Boolean = false,
    val compressionRatio: Float = 1.0f, // 1.0 = no compression, 4.0 = heavy
    val compressionThreshold: Float = -20f, // dB
    val compressionAttack: Float = 10f, // ms
    val compressionRelease: Float = 100f, // ms
    
    // Dialog Enhancement
    val dialogEnhancementEnabled: Boolean = false,
    val dialogEnhancementLevel: Float = 0.5f, // 0-1.0
    
    // Speaker/Headphone Optimization
    val outputMode: OutputMode = OutputMode.AUTO,
    
    // Volume Leveling
    val volumeLevelingEnabled: Boolean = false,
    val volumeLevelingTarget: Float = -14f // LUFS target
)

@Serializable
enum class VirtualizerMode(val displayName: String, val description: String) {
    AUTO("Auto", "Automatically detect output device"),
    HEADPHONES("Headphones", "Optimized for headphone listening"),
    SPEAKERS("Speakers", "Optimized for speaker output"),
    HEADPHONES_CROSSFEED("Crossfeed", "Natural headphone experience with crossfeed")
}

@Serializable
enum class OutputMode(val displayName: String) {
    AUTO("Auto Detect"),
    HEADPHONES("Headphones"),
    EARPHONES("Earphones"),
    SPEAKERS("Device Speakers"),
    BLUETOOTH("Bluetooth"),
    USB_DAC("USB DAC"),
    WIRED_SPEAKERS("Wired Speakers")
}

@Serializable
data class GlobalAudioState(
    val isMasterEnabled: Boolean = true,
    val masterVolume: Float = 1.0f, // 0-1.0
    val currentProfileId: String? = null,
    val isUsingCustomEq: Boolean = false,
    val lastActiveSessionId: Int? = null,
    val advancedSettings: AdvancedAudioSettings = AdvancedAudioSettings(),
    val customEqualizerBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
)

@Serializable
data class AudioSession(
    val sessionId: Int,
    val packageName: String,
    val appName: String,
    val isActive: Boolean = true,
    val hasEqualizer: Boolean = false,
    val profileId: String? = null,
    val customSettings: AdvancedAudioSettings? = null
)

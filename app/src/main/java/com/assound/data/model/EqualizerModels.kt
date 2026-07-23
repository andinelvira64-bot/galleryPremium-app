package com.assound.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EqualizerBand(
    val id: Int,
    val centerFrequency: Int, // Hz
    val minLevel: Int = -15, // dB
    val maxLevel: Int = 15, // dB
    val currentLevel: Float = 0f // dB
) {
    val frequencyLabel: String
        get() = when {
            centerFrequency >= 1000 -> "${centerFrequency / 1000}kHz"
            else -> "${centerFrequency}Hz"
        }
    
    val normalizedLevel: Float
        get() = (currentLevel - minLevel) / (maxLevel - minLevel)
    
    companion object {
        fun fromNormalized(band: EqualizerBand, normalized: Float): EqualizerBand {
            val level = band.minLevel + (normalized * (band.maxLevel - band.minLevel))
            return band.copy(currentLevel = level)
        }
    }
}

@Serializable
data class EqualizerPreset(
    val id: String,
    val name: String,
    val bands: List<Float>, // dB values for each band
    val isCustom: Boolean = false,
    val isSystem: Boolean = false
) {
    companion object {
        val FLAT = EqualizerPreset(
            id = "flat",
            name = "Flat",
            bands = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            isSystem = true
        )
        
        val BASS_BOOST = EqualizerPreset(
            id = "bass_boost",
            name = "Bass Boost",
            bands = listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
            isSystem = true
        )
        
        val TREBLE_BOOST = EqualizerPreset(
            id = "treble_boost",
            name = "Treble Boost",
            bands = listOf(0f, 0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 6f),
            isSystem = true
        )
        
        val VOCAL = EqualizerPreset(
            id = "vocal",
            name = "Vocal",
            bands = listOf(-2f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -2f),
            isSystem = true
        )
        
        val ROCK = EqualizerPreset(
            id = "rock",
            name = "Rock",
            bands = listOf(5f, 4f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 5f),
            isSystem = true
        )
        
        val POP = EqualizerPreset(
            id = "pop",
            name = "Pop",
            bands = listOf(-1f, 1f, 3f, 4f, 3f, 1f, -1f, -1f, 1f, 2f),
            isSystem = true
        )
        
        val JAZZ = EqualizerPreset(
            id = "jazz",
            name = "Jazz",
            bands = listOf(3f, 2f, 1f, 2f, -2f, -2f, 0f, 1f, 2f, 3f),
            isSystem = true
        )
        
        val CLASSICAL = EqualizerPreset(
            id = "classical",
            name = "Classical",
            bands = listOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f),
            isSystem = true
        )
        
        val ELECTRONIC = EqualizerPreset(
            id = "electronic",
            name = "Electronic",
            bands = listOf(5f, 4f, 2f, 0f, -2f, -2f, 0f, 2f, 4f, 5f),
            isSystem = true
        )
        
        val HIPHOP = EqualizerPreset(
            id = "hiphop",
            name = "Hip-Hop",
            bands = listOf(5f, 4f, 3f, 1f, -1f, -1f, 1f, 0f, 2f, 3f),
            isSystem = true
        )
        
        val systemPresets = listOf(
            FLAT, BASS_BOOST, TREBLE_BOOST, VOCAL, ROCK, 
            POP, JAZZ, CLASSICAL, ELECTRONIC, HIPHOP
        )
    }
}

@Serializable
enum class EqualizerFrequency(val hz: Int) {
    BAND_31(31),
    BAND_62(62),
    BAND_125(125),
    BAND_250(250),
    BAND_500(500),
    BAND_1K(1000),
    BAND_2K(2000),
    BAND_4K(4000),
    BAND_8K(8000),
    BAND_16K(16000)
}

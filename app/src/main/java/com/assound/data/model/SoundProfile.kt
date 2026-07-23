package com.assound.data.model

import androidx.compose.ui.graphics.Color
import com.assound.ui.theme.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SoundProfile(
    val id: String,
    val name: String,
    val brandName: String,
    val description: String,
    val category: ProfileCategory,
    val equalizerBands: List<Float>,
    val bassBoost: Int = 0, // 0-1000
    val virtualizerStrength: Int = 0, // 0-1000
    val reverbPreset: ReverbPreset = ReverbPreset.NONE,
    val reverbStrength: Int = 0, // 0-1000
    val loudnessGain: Int = 0, // 0-1000 mB
    val stereoWidth: Float = 1.0f, // 0.5-2.0
    val dynamicRange: Float = 1.0f, // 0.5-2.0 compression ratio
    val dialogEnhancement: Float = 0f, // 0-1.0
    val isActive: Boolean = false,
    val isCustom: Boolean = false,
    val isPremium: Boolean = false,
    @Transient val iconResId: Int = 0,
    @Transient val gradientColors: List<Color> = emptyList()
)

@Serializable
enum class ProfileCategory {
    SIGNATURE,      // Brand signature sounds
    MUSIC,          // Music genres
    MEDIA,          // Movies, games, podcasts
    SPATIAL,        // 3D audio, surround
    CUSTOM          // User created
}

@Serializable
enum class ReverbPreset(val displayName: String) {
    NONE("Off"),
    SMALL_ROOM("Small Room"),
    MEDIUM_ROOM("Medium Room"),
    LARGE_ROOM("Large Room"),
    MEDIUM_HALL("Medium Hall"),
    LARGE_HALL("Large Hall"),
    PLATE("Plate")
}

object SoundProfiles {
    
    val DOLBY_ATMOS = SoundProfile(
        id = "dolby_atmos",
        name = "Atmos",
        brandName = "Dolby",
        description = "Cinematic 3D surround sound with deep bass and crystal clear dialogue",
        category = ProfileCategory.SIGNATURE,
        equalizerBands = listOf(3f, 2f, 1f, 2f, 3f, 3f, 2f, 1f, 2f, 3f),
        bassBoost = 400,
        virtualizerStrength = 800,
        reverbPreset = ReverbPreset.LARGE_HALL,
        reverbStrength = 300,
        loudnessGain = 200,
        stereoWidth = 1.5f,
        dynamicRange = 0.8f,
        dialogEnhancement = 0.6f,
        gradientColors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460))
    )
    
    val DOLBY_CINEMA = SoundProfile(
        id = "dolby_cinema",
        name = "Cinema",
        brandName = "Dolby",
        description = "Theater-like experience with immersive surround and enhanced dynamics",
        category = ProfileCategory.SIGNATURE,
        equalizerBands = listOf(4f, 3f, 1f, 0f, 1f, 2f, 2f, 1f, 2f, 3f),
        bassBoost = 600,
        virtualizerStrength = 900,
        reverbPreset = ReverbPreset.LARGE_HALL,
        reverbStrength = 500,
        loudnessGain = 300,
        stereoWidth = 1.8f,
        dynamicRange = 0.7f,
        dialogEnhancement = 0.8f,
        gradientColors = listOf(Color(0xFF000000), Color(0xFF1a1a1a), Color(0xFF2d2d2d))
    )
    
    val JBL_SIGNATURE = SoundProfile(
        id = "jbl_signature",
        name = "Signature",
        brandName = "JBL",
        description = "Powerful bass with energetic highs - the iconic JBL sound",
        category = ProfileCategory.SIGNATURE,
        equalizerBands = listOf(7f, 6f, 4f, 1f, -1f, -2f, 0f, 3f, 5f, 6f),
        bassBoost = 750,
        virtualizerStrength = 400,
        reverbPreset = ReverbPreset.MEDIUM_ROOM,
        reverbStrength = 200,
        loudnessGain = 400,
        stereoWidth = 1.3f,
        dynamicRange = 0.9f,
        gradientColors = listOf(Color(0xFFFF6600), Color(0xFFFF8533), Color(0xFFFFAD66))
    )
    
    val JBL_QUANTUM = SoundProfile(
        id = "jbl_quantum",
        name = "Quantum",
        brandName = "JBL",
        description = "Gaming-optimized audio with spatial awareness and footstep clarity",
        category = ProfileCategory.SIGNATURE,
        equalizerBands = listOf(5f, 4f, 2f, 1f, 2f, 3f, 4f, 5f, 4f, 3f),
        bassBoost = 500,
        virtualizerStrength = 700,
        reverbPreset = ReverbPreset.SMALL_ROOM,
        reverbStrength = 150,
        loudnessGain = 200,
        stereoWidth = 1.6f,
        dynamicRange = 1.0f,
        gradientColors = listOf(Color(0xFF00D4FF), Color(0xFF0099CC), Color(0xFF006699))
    )
    
    val HARMAN_KARDON = SoundProfile(
        id = "harman_kardon",
        name = "Signature",
        brandName = "Harman Kardon",
        description = "Warm, balanced sound with refined midrange and natural timbre",
        category = ProfileCategory.SIGNATURE,
        equalizerBands = listOf(2f, 2f, 3f, 4f, 4f, 3f, 2f, 1f, 1f, 2f),
        bassBoost = 300,
        virtualizerStrength = 350,
        reverbPreset = ReverbPreset.MEDIUM_ROOM,
        reverbStrength = 250,
        loudnessGain = 150,
        stereoWidth = 1.2f,
        dynamicRange = 0.85f,
        gradientColors = listOf(Color(0xFF2C3E50), Color(0xFF34495E), Color(0xFF5D6D7E))
    )
    
    val SONY_HIRES = SoundProfile(
        id = "sony_hires",
        name = "Hi-Res",
        brandName = "Sony",
        description = "High-resolution audio with exceptional detail and clarity",
        category = ProfileCategory.SIGNATURE,
        equalizerBands = listOf(1f, 1f, 2f, 2f, 2f, 3f, 4f, 5f, 5f, 4f),
        bassBoost = 200,
        virtualizerStrength = 300,
        reverbPreset = ReverbPreset.SMALL_ROOM,
        reverbStrength = 100,
        loudnessGain = 100,
        stereoWidth = 1.1f,
        dynamicRange = 1.0f,
        gradientColors = listOf(Color(0xFF000000), Color(0xFF1C1C1C), Color(0xFF383838))
    )
    
    val SONY_360RA = SoundProfile(
        id = "sony_360ra",
        name = "360 Reality",
        brandName = "Sony",
        description = "Immersive 360-degree audio experience with object-based sound",
        category = ProfileCategory.SPATIAL,
        equalizerBands = listOf(2f, 2f, 2f, 3f, 3f, 3f, 3f, 2f, 2f, 2f),
        bassBoost = 350,
        virtualizerStrength = 950,
        reverbPreset = ReverbPreset.LARGE_HALL,
        reverbStrength = 400,
        loudnessGain = 200,
        stereoWidth = 2.0f,
        dynamicRange = 0.85f,
        gradientColors = listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2), Color(0xFFB721FF))
    )
    
    val BOSE_SIGNATURE = SoundProfile(
        id = "bose_signature",
        name = "Signature",
        brandName = "Bose",
        description = "Crystal clear vocals with rich, balanced bass - the Bose experience",
        category = ProfileCategory.SIGNATURE,
        equalizerBands = listOf(3f, 3f, 2f, 3f, 4f, 4f, 3f, 2f, 2f, 2f),
        bassBoost = 450,
        virtualizerStrength = 400,
        reverbPreset = ReverbPreset.MEDIUM_ROOM,
        reverbStrength = 200,
        loudnessGain = 250,
        stereoWidth = 1.25f,
        dynamicRange = 0.9f,
        dialogEnhancement = 0.5f,
        gradientColors = listOf(Color(0xFF4B5563), Color(0xFF6B7280), Color(0xFF9CA3AF))
    )
    
    val SENNHEISER_HD = SoundProfile(
        id = "sennheiser_hd",
        name = "HD Studio",
        brandName = "Sennheiser",
        description = "Audiophile-grade accuracy with neutral reference sound",
        category = ProfileCategory.SIGNATURE,
        equalizerBands = listOf(0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, 0f, 0f),
        bassBoost = 100,
        virtualizerStrength = 200,
        reverbPreset = ReverbPreset.NONE,
        reverbStrength = 0,
        loudnessGain = 50,
        stereoWidth = 1.0f,
        dynamicRange = 1.0f,
        gradientColors = listOf(Color(0xFF1E3A5F), Color(0xFF2E5077), Color(0xFF4A6FA5))
    )
    
    val BANG_OLUFSEN = SoundProfile(
        id = "bang_olufsen",
        name = "Signature",
        brandName = "Bang & Olufsen",
        description = "Luxurious, warm sound with exceptional detail and craftsmanship",
        category = ProfileCategory.SIGNATURE,
        equalizerBands = listOf(2f, 3f, 3f, 4f, 3f, 2f, 2f, 3f, 3f, 2f),
        bassBoost = 350,
        virtualizerStrength = 450,
        reverbPreset = ReverbPreset.MEDIUM_HALL,
        reverbStrength = 300,
        loudnessGain = 180,
        stereoWidth = 1.35f,
        dynamicRange = 0.9f,
        gradientColors = listOf(Color(0xFFB8860B), Color(0xFFDAA520), Color(0xFFFFD700))
    )
    
    val STUDIO_FLAT = SoundProfile(
        id = "studio_flat",
        name = "Flat",
        brandName = "Studio",
        description = "Pure, uncolored reference sound for critical listening",
        category = ProfileCategory.MUSIC,
        equalizerBands = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        bassBoost = 0,
        virtualizerStrength = 0,
        reverbPreset = ReverbPreset.NONE,
        reverbStrength = 0,
        loudnessGain = 0,
        stereoWidth = 1.0f,
        dynamicRange = 1.0f,
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF6EE7B7))
    )
    
    val PODCAST = SoundProfile(
        id = "podcast",
        name = "Podcast",
        brandName = "Media",
        description = "Voice-optimized clarity with reduced background noise",
        category = ProfileCategory.MEDIA,
        equalizerBands = listOf(-3f, -2f, 0f, 3f, 4f, 4f, 3f, 1f, 0f, -1f),
        bassBoost = 100,
        virtualizerStrength = 100,
        reverbPreset = ReverbPreset.NONE,
        reverbStrength = 0,
        loudnessGain = 300,
        stereoWidth = 1.0f,
        dynamicRange = 0.6f,
        dialogEnhancement = 0.9f,
        gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6), Color(0xFFA78BFA))
    )
    
    val GAMING = SoundProfile(
        id = "gaming",
        name = "Gaming",
        brandName = "FX",
        description = "Enhanced spatial awareness with impactful explosions and clear footsteps",
        category = ProfileCategory.MEDIA,
        equalizerBands = listOf(5f, 4f, 2f, 0f, 1f, 3f, 5f, 6f, 5f, 4f),
        bassBoost = 600,
        virtualizerStrength = 800,
        reverbPreset = ReverbPreset.SMALL_ROOM,
        reverbStrength = 150,
        loudnessGain = 350,
        stereoWidth = 1.7f,
        dynamicRange = 1.1f,
        gradientColors = listOf(Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFF87171))
    )
    
    val allProfiles = listOf(
        DOLBY_ATMOS,
        DOLBY_CINEMA,
        JBL_SIGNATURE,
        JBL_QUANTUM,
        HARMAN_KARDON,
        SONY_HIRES,
        SONY_360RA,
        BOSE_SIGNATURE,
        SENNHEISER_HD,
        BANG_OLUFSEN,
        STUDIO_FLAT,
        PODCAST,
        GAMING
    )
    
    val signatureProfiles = allProfiles.filter { it.category == ProfileCategory.SIGNATURE }
    val mediaProfiles = allProfiles.filter { it.category == ProfileCategory.MEDIA }
    val spatialProfiles = allProfiles.filter { it.category == ProfileCategory.SPATIAL }
    
    fun getProfileById(id: String): SoundProfile? {
        return allProfiles.find { it.id == id }
    }
    
    fun getProfilesByBrand(brandName: String): List<SoundProfile> {
        return allProfiles.filter { it.brandName.equals(brandName, ignoreCase = true) }
    }
}

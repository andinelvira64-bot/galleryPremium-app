package com.assound.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.assound.data.model.OutputMode
import com.assound.data.model.ReverbPreset
import com.assound.data.model.VirtualizerMode
import com.assound.ui.components.*
import com.assound.ui.theme.*
import com.assound.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val advancedSettings by viewModel.advancedSettings.collectAsState()
    
    val scrollState = rememberScrollState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundGradientStart,
                        BackgroundGradientMid,
                        BackgroundGradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        "Advanced DSP",
                        style = ASsoundTypography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                // Bass Boost Section
                AdvancedSettingCard(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Bass Boost",
                    description = "Enhance low frequencies",
                    isEnabled = advancedSettings.bassBoostEnabled,
                    onEnabledChange = { enabled ->
                        viewModel.setBassBoost(enabled, advancedSettings.bassBoostStrength)
                    },
                    accentColor = EqBandColor1
                ) {
                    Column {
                        Text(
                            text = "Strength: ${(advancedSettings.bassBoostStrength / 10f).roundToInt()}%",
                            style = ASsoundTypography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalSliderWithLabel(
                            value = advancedSettings.bassBoostStrength / 1000f,
                            onValueChange = { value ->
                                viewModel.setBassBoost(
                                    advancedSettings.bassBoostEnabled,
                                    (value * 1000).roundToInt()
                                )
                            },
                            activeColor = EqBandColor1,
                            enabled = advancedSettings.bassBoostEnabled
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Virtualizer / 3D Audio Section
                AdvancedSettingCard(
                    icon = Icons.Outlined.Surround,
                    title = "3D Virtualizer",
                    description = "Create immersive spatial audio",
                    isEnabled = advancedSettings.virtualizerEnabled,
                    onEnabledChange = { enabled ->
                        viewModel.setVirtualizer(
                            enabled,
                            advancedSettings.virtualizerStrength,
                            advancedSettings.virtualizerMode
                        )
                    },
                    accentColor = ASsoundSecondary
                ) {
                    Column {
                        Text(
                            text = "Strength: ${(advancedSettings.virtualizerStrength / 10f).roundToInt()}%",
                            style = ASsoundTypography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalSliderWithLabel(
                            value = advancedSettings.virtualizerStrength / 1000f,
                            onValueChange = { value ->
                                viewModel.setVirtualizer(
                                    advancedSettings.virtualizerEnabled,
                                    (value * 1000).roundToInt(),
                                    advancedSettings.virtualizerMode
                                )
                            },
                            activeColor = ASsoundSecondary,
                            enabled = advancedSettings.virtualizerEnabled
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Mode",
                            style = ASsoundTypography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VirtualizerMode.values().take(3).forEach { mode ->
                                ASsoundChip(
                                    text = mode.displayName,
                                    selected = advancedSettings.virtualizerMode == mode,
                                    onClick = {
                                        viewModel.setVirtualizer(
                                            advancedSettings.virtualizerEnabled,
                                            advancedSettings.virtualizerStrength,
                                            mode
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Reverb Section
                AdvancedSettingCard(
                    icon = Icons.Outlined.Waves,
                    title = "Reverb",
                    description = "Add room ambience",
                    isEnabled = advancedSettings.reverbEnabled,
                    onEnabledChange = { enabled ->
                        viewModel.setReverb(
                            enabled,
                            advancedSettings.reverbPreset,
                            advancedSettings.reverbStrength
                        )
                    },
                    accentColor = ASsoundAccent
                ) {
                    Column {
                        Text(
                            text = "Preset",
                            style = ASsoundTypography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Reverb presets
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReverbPreset.values().take(4).forEach { preset ->
                                    ASsoundChip(
                                        text = preset.displayName,
                                        selected = advancedSettings.reverbPreset == preset,
                                        onClick = {
                                            viewModel.setReverb(
                                                advancedSettings.reverbEnabled,
                                                preset,
                                                advancedSettings.reverbStrength
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReverbPreset.values().drop(4).forEach { preset ->
                                    ASsoundChip(
                                        text = preset.displayName,
                                        selected = advancedSettings.reverbPreset == preset,
                                        onClick = {
                                            viewModel.setReverb(
                                                advancedSettings.reverbEnabled,
                                                preset,
                                                advancedSettings.reverbStrength
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Strength: ${(advancedSettings.reverbStrength / 10f).roundToInt()}%",
                            style = ASsoundTypography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalSliderWithLabel(
                            value = advancedSettings.reverbStrength / 1000f,
                            onValueChange = { value ->
                                viewModel.setReverb(
                                    advancedSettings.reverbEnabled,
                                    advancedSettings.reverbPreset,
                                    (value * 1000).roundToInt()
                                )
                            },
                            activeColor = ASsoundAccent,
                            enabled = advancedSettings.reverbEnabled
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Loudness Enhancer
                AdvancedSettingCard(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Loudness Enhancer",
                    description = "Boost perceived loudness",
                    isEnabled = advancedSettings.loudnessEnhancerEnabled,
                    onEnabledChange = { enabled ->
                        viewModel.setLoudnessEnhancer(enabled, advancedSettings.loudnessTargetGain)
                    },
                    accentColor = StatusWarning
                ) {
                    Column {
                        Text(
                            text = "Gain: +${(advancedSettings.loudnessTargetGain / 10f).roundToInt()}%",
                            style = ASsoundTypography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalSliderWithLabel(
                            value = advancedSettings.loudnessTargetGain / 1000f,
                            onValueChange = { value ->
                                viewModel.setLoudnessEnhancer(
                                    advancedSettings.loudnessEnhancerEnabled,
                                    (value * 1000).roundToInt()
                                )
                            },
                            activeColor = StatusWarning,
                            enabled = advancedSettings.loudnessEnhancerEnabled
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stereo Width
                AdvancedSettingCard(
                    icon = Icons.Outlined.Headphones,
                    title = "Stereo Width",
                    description = "Adjust stereo separation",
                    isEnabled = advancedSettings.stereoWidthEnabled,
                    onEnabledChange = { enabled ->
                        viewModel.setStereoWidth(enabled, advancedSettings.stereoWidth)
                    },
                    accentColor = ASsoundPrimaryLight
                ) {
                    Column {
                        val widthPercent = ((advancedSettings.stereoWidth - 0.5f) / 1.5f * 100).roundToInt()
                        Text(
                            text = when {
                                advancedSettings.stereoWidth < 0.7f -> "Narrow ($widthPercent%)"
                                advancedSettings.stereoWidth < 1.2f -> "Normal ($widthPercent%)"
                                else -> "Wide ($widthPercent%)"
                            },
                            style = ASsoundTypography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalSliderWithLabel(
                            value = (advancedSettings.stereoWidth - 0.5f) / 1.5f,
                            onValueChange = { value ->
                                val newWidth = 0.5f + (value * 1.5f)
                                viewModel.setStereoWidth(
                                    advancedSettings.stereoWidthEnabled,
                                    newWidth
                                )
                            },
                            activeColor = ASsoundPrimaryLight,
                            enabled = advancedSettings.stereoWidthEnabled
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dialog Enhancement
                AdvancedSettingCard(
                    icon = Icons.Outlined.RecordVoiceOver,
                    title = "Dialog Enhancement",
                    description = "Improve voice clarity",
                    isEnabled = advancedSettings.dialogEnhancementEnabled,
                    onEnabledChange = { enabled ->
                        viewModel.setDialogEnhancement(enabled, advancedSettings.dialogEnhancementLevel)
                    },
                    accentColor = StatusInfo
                ) {
                    Column {
                        Text(
                            text = "Level: ${(advancedSettings.dialogEnhancementLevel * 100).roundToInt()}%",
                            style = ASsoundTypography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalSliderWithLabel(
                            value = advancedSettings.dialogEnhancementLevel,
                            onValueChange = { value ->
                                viewModel.setDialogEnhancement(
                                    advancedSettings.dialogEnhancementEnabled,
                                    value
                                )
                            },
                            activeColor = StatusInfo,
                            enabled = advancedSettings.dialogEnhancementEnabled
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Output Mode
                SectionHeader(title = "Output Device")
                Spacer(modifier = Modifier.height(12.dp))
                
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        OutputMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        viewModel.setOutputMode(mode)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = advancedSettings.outputMode == mode,
                                    onClick = { viewModel.setOutputMode(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ASsoundPrimary,
                                        unselectedColor = TextTertiary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = mode.displayName,
                                    style = ASsoundTypography.bodyMedium,
                                    color = if (advancedSettings.outputMode == mode) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun AdvancedSettingCard(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        glassOpacity = if (isEnabled) 0.1f else 0.06f
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isEnabled) accentColor.copy(alpha = 0.2f) 
                                else SurfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isEnabled) accentColor else TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = title,
                            style = ASsoundTypography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = description,
                            style = ASsoundTypography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
                
                ASsoundSwitch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    activeColor = accentColor
                )
            }
            
            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = CardBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                }
            }
        }
    }
}

// Extension to support icon
private val Icons.Outlined.Surround: ImageVector
    get() = Icons.Outlined.SurroundSound

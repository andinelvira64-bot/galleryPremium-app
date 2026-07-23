package com.assound.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assound.data.model.EqualizerPreset
import com.assound.ui.components.*
import com.assound.ui.theme.*
import com.assound.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val equalizerBands by viewModel.equalizerBands.collectAsState()
    
    val scrollState = rememberScrollState()
    
    // Frequency labels for 10-band EQ
    val frequencyLabels = listOf(
        "31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K"
    )
    
    // EQ Band colors gradient
    val bandColors = listOf(
        EqBandColor1, EqBandColor1.copy(alpha = 0.9f),
        EqBandColor2, EqBandColor2.copy(alpha = 0.9f),
        EqBandColor3, EqBandColor3.copy(alpha = 0.9f),
        EqBandColor4, EqBandColor4.copy(alpha = 0.9f),
        EqBandColor5, EqBandColor5.copy(alpha = 0.9f)
    )
    
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    
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
                        "Equalizer",
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
                actions = {
                    // Reset button
                    IconButton(onClick = { viewModel.resetEqualizer() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = TextSecondary
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
                // Current state indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsingIndicator(
                        color = if (uiState.isMasterEnabled) StatusSuccess else StatusError,
                        size = 8.dp,
                        isActive = uiState.isMasterEnabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            uiState.selectedProfile != null -> 
                                "${uiState.selectedProfile!!.brandName} ${uiState.selectedProfile!!.name}"
                            uiState.isUsingCustomEq -> "Custom Equalizer"
                            else -> "No Active Profile"
                        },
                        style = ASsoundTypography.bodyMedium,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Main Equalizer
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    cornerRadius = 24.dp,
                    glassOpacity = 0.08f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // dB scale labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "+15 dB",
                                style = ASsoundTextStyles.frequencyLabel,
                                color = TextTertiary
                            )
                            Text(
                                text = "0 dB",
                                style = ASsoundTextStyles.frequencyLabel,
                                color = TextTertiary
                            )
                            Text(
                                text = "-15 dB",
                                style = ASsoundTextStyles.frequencyLabel,
                                color = TextTertiary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Equalizer bands
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            equalizerBands.forEachIndexed { index, value ->
                                EqualizerBandSlider(
                                    value = value,
                                    onValueChange = { newValue ->
                                        viewModel.setEqualizerBand(index, newValue)
                                        selectedPreset = null
                                    },
                                    frequencyLabel = frequencyLabels[index],
                                    modifier = Modifier.weight(1f),
                                    barColor = bandColors[index],
                                    glowColor = bandColors[index].copy(alpha = 0.4f),
                                    enabled = uiState.isMasterEnabled
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Presets Section
                SectionHeader(title = "Quick Presets")
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Preset chips row 1
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(EqualizerPreset.systemPresets.take(5)) { preset ->
                        ASsoundChip(
                            text = preset.name,
                            selected = selectedPreset == preset.id,
                            onClick = {
                                selectedPreset = preset.id
                                viewModel.applyPreset(preset)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Preset chips row 2
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(EqualizerPreset.systemPresets.drop(5)) { preset ->
                        ASsoundChip(
                            text = preset.name,
                            selected = selectedPreset == preset.id,
                            onClick = {
                                selectedPreset = preset.id
                                viewModel.applyPreset(preset)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Tips section
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    glassOpacity = 0.06f
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = StatusWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pro Tips",
                                style = ASsoundTypography.titleSmall,
                                color = TextPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "• Boost low frequencies (31-250Hz) for more bass\n" +
                                  "• Reduce mid-range (500Hz-2kHz) to minimize harshness\n" +
                                  "• Boost highs (8kHz-16kHz) for more clarity and sparkle\n" +
                                  "• Small adjustments (±3dB) often sound more natural",
                            style = ASsoundTypography.bodySmall,
                            color = TextSecondary,
                            lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

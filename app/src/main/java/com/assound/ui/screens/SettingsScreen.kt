package com.assound.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elvira.gallery.BuildConfig
import com.assound.audio.service.AudioProcessingService
import com.assound.ui.components.*
import com.assound.ui.theme.*
import com.assound.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val serviceState by viewModel.serviceState.collectAsState()
    val context = LocalContext.current
    
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
                        "Settings",
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
                // Audio Engine Section
                SectionHeader(title = "Audio Engine")
                Spacer(modifier = Modifier.height(12.dp))
                
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(
                            icon = Icons.Outlined.PowerSettingsNew,
                            title = "Audio Processing",
                            subtitle = if (serviceState.isRunning) "Service running" else "Service stopped",
                            trailing = {
                                ASsoundSwitch(
                                    checked = serviceState.isRunning,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            viewModel.startAudioService()
                                        } else {
                                            viewModel.stopAudioService()
                                        }
                                    }
                                )
                            }
                        )
                        
                        SettingsDivider()
                        
                        SettingsRow(
                            icon = Icons.Outlined.Memory,
                            title = "Active Sessions",
                            subtitle = "${serviceState.activeSessionCount} audio session(s)"
                        )
                        
                        SettingsDivider()
                        
                        SettingsRow(
                            icon = Icons.Outlined.Speaker,
                            title = "Output Device",
                            subtitle = serviceState.outputDevice.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // UI Settings Section
                SectionHeader(title = "Interface")
                Spacer(modifier = Modifier.height(12.dp))
                
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(
                            icon = Icons.Outlined.Equalizer,
                            title = "Show Visualizer",
                            subtitle = "Display audio waveform on home",
                            trailing = {
                                ASsoundSwitch(
                                    checked = uiState.showVisualizer,
                                    onCheckedChange = { viewModel.setShowVisualizer(it) }
                                )
                            }
                        )
                        
                        SettingsDivider()
                        
                        SettingsRow(
                            icon = Icons.Outlined.Vibration,
                            title = "Haptic Feedback",
                            subtitle = "Vibrate on control interaction",
                            trailing = {
                                ASsoundSwitch(
                                    checked = uiState.hapticEnabled,
                                    onCheckedChange = { viewModel.setHapticFeedback(it) }
                                )
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Current Profile Info
                SectionHeader(title = "Current Configuration")
                Spacer(modifier = Modifier.height(12.dp))
                
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(
                            icon = Icons.Outlined.Tune,
                            title = "Active Profile",
                            subtitle = uiState.selectedProfile?.let { 
                                "${it.brandName} ${it.name}" 
                            } ?: "None selected"
                        )
                        
                        if (uiState.isUsingCustomEq) {
                            SettingsDivider()
                            
                            SettingsRow(
                                icon = Icons.Outlined.GraphicEq,
                                title = "Custom EQ",
                                subtitle = "Using custom equalizer settings"
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Device Capabilities
                SectionHeader(title = "Device Support")
                Spacer(modifier = Modifier.height(12.dp))
                
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CapabilityRow("Equalizer", true)
                        SettingsDivider()
                        CapabilityRow("Bass Boost", true)
                        SettingsDivider()
                        CapabilityRow("Virtualizer", true)
                        SettingsDivider()
                        CapabilityRow("Reverb", true)
                        SettingsDivider()
                        CapabilityRow("Loudness Enhancer", true)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // About Section
                SectionHeader(title = "About")
                Spacer(modifier = Modifier.height(12.dp))
                
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(
                            icon = Icons.Outlined.Info,
                            title = "ASsound",
                            subtitle = "Professional Audio Equalizer"
                        )
                        
                        SettingsDivider()
                        
                        SettingsRow(
                            icon = Icons.Outlined.Numbers,
                            title = "Version",
                            subtitle = "1.0.0"
                        )
                        
                        SettingsDivider()
                        
                        SettingsRow(
                            icon = Icons.Outlined.Code,
                            title = "Build",
                            subtitle = "Release"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Credits
                Text(
                    text = "Made with ❤️ for audiophiles",
                    style = ASsoundTypography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ASsoundPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ASsoundTypography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = ASsoundTypography.bodySmall,
                color = TextTertiary
            )
        }
        
        trailing?.invoke()
    }
}

@Composable
private fun CapabilityRow(
    feature: String,
    isSupported: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = feature,
            style = ASsoundTypography.bodyMedium,
            color = TextPrimary
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isSupported) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isSupported) StatusSuccess else StatusError,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isSupported) "Supported" else "Not Available",
                style = ASsoundTypography.bodySmall,
                color = if (isSupported) StatusSuccess else StatusError
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Spacer(modifier = Modifier.height(4.dp))
    Divider(
        color = CardBorder.copy(alpha = 0.5f),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 52.dp)
    )
    Spacer(modifier = Modifier.height(4.dp))
}

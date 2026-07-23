package com.assound.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.assound.audio.service.AudioProcessingService
import com.assound.data.model.SoundProfiles
import com.assound.ui.components.*
import com.assound.ui.theme.*
import com.assound.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val serviceState by viewModel.serviceState.collectAsState()
    val equalizerBands by viewModel.equalizerBands.collectAsState()
    
    val scrollState = rememberScrollState()
    
    // Animated background gradient
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundGradientStart,
                        BackgroundGradientMid,
                        BackgroundGradientEnd
                    ),
                    startY = gradientOffset * 200f
                )
            )
    ) {
        // Subtle glow orbs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-50).dp, y = 100.dp)
                .blur(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ASsoundPrimary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = 200.dp, y = 400.dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ASsoundSecondary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            HomeHeader(
                isEnabled = uiState.isMasterEnabled,
                onEnabledChange = { viewModel.setMasterEnabled(it) },
                onSettingsClick = onNavigateToSettings
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status Card
            StatusCard(
                serviceState = serviceState,
                currentProfile = uiState.selectedProfile?.let { "${it.brandName} ${it.name}" },
                isEnabled = uiState.isMasterEnabled
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Quick EQ Preview
            SectionHeader(
                title = "Equalizer",
                action = {
                    TextButton(onClick = onNavigateToEqualizer) {
                        Text(
                            "Customize",
                            color = ASsoundPrimary,
                            style = ASsoundTypography.labelMedium
                        )
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = ASsoundPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Mini EQ Visualizer
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    equalizerBands.forEachIndexed { index, value ->
                        val normalizedValue = ((value + 15f) / 30f).coerceIn(0f, 1f)
                        val barColor = when (index) {
                            0, 1 -> EqBandColor1
                            2, 3 -> EqBandColor2
                            4, 5 -> EqBandColor3
                            6, 7 -> EqBandColor4
                            else -> EqBandColor5
                        }
                        
                        EqualizerVisualizerBar(
                            level = normalizedValue,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 2.dp),
                            barColor = barColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Sound Profiles Section
            SectionHeader(
                title = "Sound Profiles",
                action = {
                    TextButton(onClick = onNavigateToProfiles) {
                        Text(
                            "View All",
                            color = ASsoundPrimary,
                            style = ASsoundTypography.labelMedium
                        )
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = ASsoundPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Profile Cards Horizontal Scroll
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                items(SoundProfiles.signatureProfiles.take(5)) { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = uiState.selectedProfile?.id == profile.id,
                        onClick = { viewModel.selectProfile(profile) },
                        modifier = Modifier.width(160.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Quick Actions
            SectionHeader(title = "Quick Actions")
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Equalizer",
                    subtitle = "10-band EQ",
                    onClick = onNavigateToEqualizer,
                    modifier = Modifier.weight(1f),
                    accentColor = ASsoundPrimary
                )
                
                QuickActionCard(
                    icon = Icons.Outlined.Tune,
                    title = "Advanced",
                    subtitle = "DSP Effects",
                    onClick = onNavigateToAdvanced,
                    modifier = Modifier.weight(1f),
                    accentColor = ASsoundSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // Bottom padding for nav bar
        }
    }
}

@Composable
private fun HomeHeader(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "ASSOUND",
                style = ASsoundTypography.headlineMedium,
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PulsingIndicator(
                    color = if (isEnabled) StatusSuccess else StatusError,
                    size = 8.dp,
                    isActive = isEnabled
                )
                Text(
                    text = if (isEnabled) "Audio Engine Active" else "Disabled",
                    style = ASsoundTypography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Power toggle
            GlowingIconButton(
                icon = if (isEnabled) Icons.Filled.PowerSettingsNew else Icons.Outlined.PowerSettingsNew,
                onClick = { onEnabledChange(!isEnabled) },
                isActive = isEnabled,
                activeColor = StatusSuccess,
                inactiveColor = TextTertiary,
                size = 44.dp
            )
            
            // Settings
            GlowingIconButton(
                icon = Icons.Outlined.Settings,
                onClick = onSettingsClick,
                isActive = false,
                size = 44.dp
            )
        }
    }
}

@Composable
private fun StatusCard(
    serviceState: AudioProcessingService.ServiceState,
    currentProfile: String?,
    isEnabled: Boolean
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        glassOpacity = 0.1f
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Current Profile",
                        style = ASsoundTypography.labelSmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentProfile ?: "None Selected",
                        style = ASsoundTypography.titleLarge,
                        color = if (currentProfile != null) TextPrimary else TextTertiary
                    )
                }
                
                // Output device indicator
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Icon(
                        imageVector = when (serviceState.outputDevice) {
                            "WIRED_HEADPHONES", "WIRED_HEADSET" -> Icons.Outlined.Headphones
                            "BLUETOOTH_A2DP", "BLUETOOTH_LE" -> Icons.Outlined.Bluetooth
                            "USB_AUDIO" -> Icons.Outlined.Usb
                            else -> Icons.Outlined.Speaker
                        },
                        contentDescription = null,
                        tint = ASsoundPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = serviceState.outputDevice.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = ASsoundTypography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusIndicator(
                    label = "Engine",
                    value = if (serviceState.isRunning) "Running" else "Stopped",
                    isActive = serviceState.isRunning
                )
                StatusIndicator(
                    label = "Sessions",
                    value = serviceState.activeSessionCount.toString(),
                    isActive = serviceState.activeSessionCount > 0
                )
                StatusIndicator(
                    label = "Status",
                    value = if (isEnabled) "Enabled" else "Disabled",
                    isActive = isEnabled
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    label: String,
    value: String,
    isActive: Boolean
) {
    Column {
        Text(
            text = label,
            style = ASsoundTypography.labelSmall,
            color = TextTertiary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = if (isActive) StatusSuccess else TextTertiary,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
            Text(
                text = value,
                style = ASsoundTypography.labelMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = ASsoundPrimary
) {
    GlassmorphicCard(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .height(100.dp),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .then(
                    Modifier.clickableWithoutRipple { onClick() }
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            
            Column {
                Text(
                    text = title,
                    style = ASsoundTypography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = ASsoundTypography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return this.clickable(
        indication = null,
        interactionSource = MutableInteractionSource(),
        onClick = onClick
    )
}

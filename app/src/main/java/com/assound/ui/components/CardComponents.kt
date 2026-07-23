package com.assound.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.assound.data.model.SoundProfile
import com.assound.ui.theme.*

@Composable
fun ProfileCard(
    profile: SoundProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isSelected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ASsoundPrimary else CardBorder,
        animationSpec = tween(200),
        label = "borderColor"
    )
    
    val gradientColors = if (profile.gradientColors.isNotEmpty()) {
        profile.gradientColors
    } else {
        listOf(ProfileCardGradientStart, ProfileCardGradientEnd)
    }
    
    Box(
        modifier = modifier
            .scale(scale)
            .aspectRatio(1.2f)
            .clip(ASsoundCardShapes.profileCard)
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = ASsoundCardShapes.profileCard
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(16.dp)
    ) {
        // Glow effect when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(20.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ASsoundPrimary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand badge
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = profile.brandName.uppercase(),
                        style = ASsoundTextStyles.statusIndicator,
                        color = TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isSelected) {
                    PulsingIndicator(
                        color = StatusSuccess,
                        size = 8.dp,
                        isActive = true
                    )
                }
            }
            
            Column {
                Text(
                    text = profile.name,
                    style = ASsoundTextStyles.profileName,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = profile.description,
                    style = ASsoundTypography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CompactProfileCard(
    profile: SoundProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) ASsoundPrimaryDark else CardSurface,
        animationSpec = tween(200),
        label = "bgColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ASsoundPrimary else Color.Transparent,
        animationSpec = tween(200),
        label = "borderColor"
    )
    
    Row(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = if (profile.gradientColors.isNotEmpty()) {
                            profile.gradientColors
                        } else {
                            listOf(ASsoundPrimary, ASsoundPrimaryLight)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile.brandName.take(2).uppercase(),
                style = ASsoundTypography.labelMedium,
                color = TextOnPrimary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${profile.brandName} ${profile.name}",
                style = ASsoundTypography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = profile.category.name.lowercase().replaceFirstChar { it.uppercase() },
                style = ASsoundTypography.bodySmall,
                color = TextTertiary
            )
        }
        
        if (isSelected) {
            PulsingIndicator(
                color = StatusSuccess,
                size = 10.dp,
                isActive = true
            )
        }
    }
}

@Composable
fun GlowingIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    activeColor: Color = ASsoundPrimary,
    inactiveColor: Color = TextTertiary,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    contentDescription: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val color by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(200),
        label = "iconColor"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(size * 1.5f)
                    .blur(12.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            interactionSource = interactionSource,
            modifier = Modifier
                .size(size)
                .background(
                    color = if (isActive) {
                        activeColor.copy(alpha = 0.15f)
                    } else {
                        SurfaceContainerHigh
                    },
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun ASsoundSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    activeColor: Color = SwitchOn,
    inactiveColor: Color = SwitchOff
) {
    val haptic = LocalHapticFeedback.current
    
    val animatedTrackColor by animateColorAsState(
        targetValue = if (checked) activeColor else inactiveColor,
        animationSpec = tween(200),
        label = "trackColor"
    )
    
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbOffset"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = ASsoundTypography.bodyMedium,
                color = if (enabled) TextPrimary else TextDisabled,
                modifier = Modifier.weight(1f)
            )
        }
        
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(animatedTrackColor)
                .clickable(enabled = enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(!checked)
                }
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .padding(vertical = 2.dp)
                    .size(24.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SwitchThumb)
            )
        }
    }
}

@Composable
fun ASsoundChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val haptic = LocalHapticFeedback.current
    
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) ASsoundPrimary else CardSurface,
        animationSpec = tween(200),
        label = "chipBg"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (selected) TextOnPrimary else TextSecondary,
        animationSpec = tween(200),
        label = "chipText"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) ASsoundPrimary else CardBorder,
        animationSpec = tween(200),
        label = "chipBorder"
    )
    
    Row(
        modifier = modifier
            .clip(ASsoundCardShapes.chipShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, ASsoundCardShapes.chipShape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        
        Text(
            text = text,
            style = ASsoundTypography.labelMedium,
            color = textColor
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = ASsoundTextStyles.sectionHeader,
            color = TextTertiary
        )
        
        action?.invoke()
    }
}

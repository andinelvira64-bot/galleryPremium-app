package com.assound.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.assound.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun EqualizerBandSlider(
    value: Float, // -15 to 15 dB
    onValueChange: (Float) -> Unit,
    frequencyLabel: String,
    modifier: Modifier = Modifier,
    minValue: Float = -15f,
    maxValue: Float = 15f,
    barColor: Color = ASsoundPrimary,
    glowColor: Color = GlowPrimary,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    
    var isDragging by remember { mutableStateOf(false) }
    
    val animatedGlow by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.5f,
        animationSpec = tween(150),
        label = "glow"
    )
    
    val normalizedValue = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // dB Value
        Text(
            text = if (value >= 0) "+${value.roundToInt()}" else "${value.roundToInt()}",
            style = ASsoundTextStyles.dbValue,
            color = if (isDragging) barColor else TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Slider Track
        Box(
            modifier = Modifier
                .width(36.dp)
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceContainerHigh)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    
                    detectDragGestures(
                        onDragStart = { 
                            isDragging = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            val height = size.height.toFloat()
                            val y = change.position.y.coerceIn(0f, height)
                            val newNormalized = 1f - (y / height)
                            val newValue = minValue + (newNormalized * (maxValue - minValue))
                            
                            // Haptic at center (0 dB)
                            val oldCenter = (value >= -0.5f && value <= 0.5f)
                            val newCenter = (newValue >= -0.5f && newValue <= 0.5f)
                            if (!oldCenter && newCenter) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            
                            onValueChange(newValue.coerceIn(minValue, maxValue))
                        }
                    )
                }
        ) {
            // Glow effect
            if (isDragging) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(normalizedValue)
                        .align(Alignment.BottomCenter)
                        .blur(12.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = animatedGlow * 0.6f),
                                    glowColor.copy(alpha = animatedGlow * 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                )
            }
            
            // Fill bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(normalizedValue.coerceAtLeast(0.02f))
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                barColor,
                                barColor.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
            )
            
            // Center line (0 dB)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .background(TextTertiary.copy(alpha = 0.5f))
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Frequency Label
        Text(
            text = frequencyLabel,
            style = ASsoundTextStyles.frequencyLabel,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EqualizerVisualizerBar(
    level: Float, // 0 to 1
    modifier: Modifier = Modifier,
    barColor: Color = VisualizerLow,
    animationDuration: Int = 100
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = tween(animationDuration, easing = FastOutSlowInEasing),
        label = "level"
    )
    
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            VisualizerHigh,
            VisualizerMid,
            VisualizerLow
        )
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedLevel)
                .align(Alignment.BottomCenter)
                .background(gradientBrush, shape = RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun AudioWaveformVisualizer(
    waveformData: List<Float>, // amplitude values 0-1
    modifier: Modifier = Modifier,
    waveColor: Color = WaveformActive,
    backgroundColor: Color = Color.Transparent,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.dp,
    cornerRadius: Dp = 2.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )
    
    Canvas(
        modifier = modifier
            .background(backgroundColor)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidthPx = barWidth.toPx()
        val barSpacingPx = barSpacing.toPx()
        val totalBarWidth = barWidthPx + barSpacingPx
        val barCount = (canvasWidth / totalBarWidth).toInt()
        
        val gradient = Brush.verticalGradient(
            colors = listOf(
                WaveformGradientStart,
                WaveformGradientEnd
            )
        )
        
        for (i in 0 until barCount) {
            val dataIndex = (i + (waveOffset * 10).toInt()) % waveformData.size.coerceAtLeast(1)
            val amplitude = if (waveformData.isNotEmpty()) {
                waveformData.getOrElse(dataIndex % waveformData.size) { 0.5f }
            } else {
                0.3f + (kotlin.math.sin(i * 0.3 + waveOffset * kotlin.math.PI * 2) * 0.2).toFloat()
            }
            
            val barHeight = (amplitude * canvasHeight).coerceAtLeast(4f)
            val x = i * totalBarWidth
            val y = (canvasHeight - barHeight) / 2
            
            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    glassOpacity: Float = 0.08f,
    borderOpacity: Float = 0.15f,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val borderWidthPx = with(density) { borderWidth.toPx() }
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = glassOpacity),
                        Color.White.copy(alpha = glassOpacity * 0.5f)
                    )
                )
            )
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = borderOpacity),
                    style = Stroke(width = borderWidthPx),
                    cornerRadius = CornerRadius(cornerRadiusPx)
                )
            },
        content = content
    )
}

@Composable
fun NeonGlowBox(
    modifier: Modifier = Modifier,
    glowColor: Color = ASsoundPrimary,
    glowRadius: Dp = 16.dp,
    glowAlpha: Float = 0.4f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        // Glow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(glowRadius)
                .background(glowColor.copy(alpha = glowAlpha))
        )
        
        // Content
        content()
    }
}

@Composable
fun PulsingIndicator(
    modifier: Modifier = Modifier,
    color: Color = StatusSuccess,
    size: Dp = 8.dp,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Canvas(modifier = modifier.size(size * scale)) {
        drawCircle(
            color = color.copy(alpha = if (isActive) alpha else 0.3f),
            radius = this.size.minDimension / 2
        )
    }
}

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        BackgroundGradientStart,
        BackgroundGradientMid,
        BackgroundGradientEnd
    ),
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = colors,
                    startY = offset * 200f,
                    endY = Float.POSITIVE_INFINITY
                )
            ),
        content = content
    )
}

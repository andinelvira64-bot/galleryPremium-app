package com.assound.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.assound.ui.theme.*
import kotlin.math.*

@Composable
fun CircularKnob(
    value: Float, // 0 to 1
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    label: String = "",
    displayValue: String = "",
    minAngle: Float = 135f,
    maxAngle: Float = 405f,
    activeColor: Color = ASsoundPrimary,
    inactiveColor: Color = SurfaceContainerHigh,
    indicatorColor: Color = TextPrimary,
    enabled: Boolean = true,
    showTicks: Boolean = true,
    tickCount: Int = 11
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var dragStartAngle by remember { mutableStateOf(0f) }
    var dragStartValue by remember { mutableStateOf(0f) }
    
    val angleRange = maxAngle - minAngle
    val currentAngle = minAngle + (value * angleRange)
    
    val animatedAngle by animateFloatAsState(
        targetValue = currentAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "knobAngle"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val centerX = this.size.width / 2f
                            val centerY = this.size.height / 2f
                            dragStartAngle = atan2(
                                offset.y - centerY,
                                offset.x - centerX
                            ) * (180f / PI.toFloat())
                            dragStartValue = value
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            val centerX = this.size.width / 2f
                            val centerY = this.size.height / 2f
                            val currentDragAngle = atan2(
                                change.position.y - centerY,
                                change.position.x - centerX
                            ) * (180f / PI.toFloat())
                            
                            var angleDelta = currentDragAngle - dragStartAngle
                            if (angleDelta > 180) angleDelta -= 360
                            if (angleDelta < -180) angleDelta += 360
                            
                            val valueDelta = angleDelta / angleRange
                            val newValue = (dragStartValue + valueDelta).coerceIn(0f, 1f)
                            
                            // Haptic at notches
                            val oldNotch = (value * (tickCount - 1)).roundToInt()
                            val newNotch = (newValue * (tickCount - 1)).roundToInt()
                            if (oldNotch != newNotch) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            
                            dragStartAngle = currentDragAngle
                            dragStartValue = newValue
                            onValueChange(newValue)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Background ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = size.toPx() * 0.08f
                val radius = (size.toPx() - strokeWidth) / 2f
                
                // Inactive track
                drawArc(
                    color = inactiveColor,
                    startAngle = minAngle,
                    sweepAngle = angleRange,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
                
                // Active track with gradient
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to activeColor.copy(alpha = 0.7f),
                        0.5f to activeColor,
                        1f to activeColor.copy(alpha = 0.7f)
                    ),
                    startAngle = minAngle,
                    sweepAngle = animatedAngle - minAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
                
                // Tick marks
                if (showTicks) {
                    val tickRadius = radius - strokeWidth * 0.8f
                    for (i in 0 until tickCount) {
                        val tickAngle = minAngle + (i.toFloat() / (tickCount - 1)) * angleRange
                        val tickRad = tickAngle * (PI.toFloat() / 180f)
                        val innerRadius = tickRadius - (size.toPx() * 0.03f)
                        
                        val startX = center.x + cos(tickRad) * innerRadius
                        val startY = center.y + sin(tickRad) * innerRadius
                        val endX = center.x + cos(tickRad) * tickRadius
                        val endY = center.y + sin(tickRad) * tickRadius
                        
                        val isActive = (i.toFloat() / (tickCount - 1)) <= value
                        drawLine(
                            color = if (isActive) activeColor else TextTertiary.copy(alpha = 0.3f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            // Center knob with indicator
            Box(
                modifier = Modifier
                    .size(size * 0.5f)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KnobBackground.copy(alpha = 1f),
                                KnobBackground.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(1.dp, KnobBorder, CircleShape)
                    .rotate(animatedAngle - 90f),
                contentAlignment = Alignment.TopCenter
            ) {
                // Indicator line
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(4.dp, (size * 0.15f).value.dp)
                        .background(indicatorColor, RoundedCornerShape(2.dp))
                )
            }
            
            // Value display
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (displayValue.isNotEmpty()) {
                    Text(
                        text = displayValue,
                        style = ASsoundTextStyles.knobValue,
                        color = TextPrimary
                    )
                }
            }
        }
        
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label.uppercase(),
                style = ASsoundTextStyles.sectionHeader,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HorizontalSliderWithLabel(
    value: Float, // 0 to 1
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    displayValue: String = "",
    activeColor: Color = ASsoundPrimary,
    inactiveColor: Color = SurfaceContainerHigh,
    thumbColor: Color = TextPrimary,
    enabled: Boolean = true,
    height: Dp = 6.dp,
    thumbSize: Dp = 20.dp
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sliderValue"
    )
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = ASsoundTypography.labelMedium,
                    color = TextSecondary
                )
            }
            if (displayValue.isNotEmpty()) {
                Text(
                    text = displayValue,
                    style = ASsoundTypography.labelMedium,
                    color = if (isDragging) activeColor else TextTertiary
                )
            }
        }
        
        if (label.isNotEmpty() || displayValue.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbSize)
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
                            val width = size.width.toFloat()
                            val x = change.position.x.coerceIn(0f, width)
                            val newValue = x / width
                            
                            // Haptic at edges
                            if ((value > 0.02f && newValue <= 0.02f) || 
                                (value < 0.98f && newValue >= 0.98f)) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            
                            onValueChange(newValue.coerceIn(0f, 1f))
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(inactiveColor)
            )
            
            // Active track
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedValue.coerceAtLeast(0.01f))
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.8f),
                                activeColor
                            )
                        )
                    )
            )
            
            // Thumb
            Box(
                modifier = Modifier
                    .offset(
                        x = with(androidx.compose.ui.platform.LocalDensity.current) {
                            val trackWidth = 300.dp - thumbSize
                            (trackWidth * animatedValue)
                        }
                    )
                    .size(thumbSize)
                    .shadow(if (isDragging) 8.dp else 4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .border(
                        width = 2.dp,
                        color = if (isDragging) activeColor else TextTertiary.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun SegmentedProgressBar(
    progress: Float, // 0 to 1
    segments: Int = 10,
    modifier: Modifier = Modifier,
    activeColor: Color = StatusSuccess,
    inactiveColor: Color = SurfaceContainerHigh,
    segmentSpacing: Dp = 2.dp,
    height: Dp = 8.dp,
    cornerRadius: Dp = 4.dp
) {
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(segmentSpacing)
    ) {
        val activeSegments = (progress * segments).roundToInt()
        
        for (i in 0 until segments) {
            val isActive = i < activeSegments
            val segmentColor = when {
                !isActive -> inactiveColor
                i < segments * 0.6f -> VisualizerLow
                i < segments * 0.85f -> VisualizerMid
                else -> VisualizerHigh
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(segmentColor)
            )
        }
    }
}

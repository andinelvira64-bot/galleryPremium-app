package com.assound.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ASsoundShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object ASsoundCardShapes {
    val profileCard = RoundedCornerShape(20.dp)
    val equalizerCard = RoundedCornerShape(24.dp)
    val controlCard = RoundedCornerShape(16.dp)
    val buttonShape = RoundedCornerShape(12.dp)
    val chipShape = RoundedCornerShape(8.dp)
    val sliderThumb = RoundedCornerShape(6.dp)
    val bottomNav = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val topSheet = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    val fullRounded = RoundedCornerShape(percent = 50)
}

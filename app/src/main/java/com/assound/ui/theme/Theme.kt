package com.assound.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ASsoundDarkColorScheme = darkColorScheme(
    primary = ASsoundPrimary,
    onPrimary = TextOnPrimary,
    primaryContainer = ASsoundPrimaryDark,
    onPrimaryContainer = ASsoundPrimaryLight,
    
    secondary = ASsoundSecondary,
    onSecondary = TextOnSecondary,
    secondaryContainer = ASsoundSecondaryVariant,
    onSecondaryContainer = ASsoundSecondaryLight,
    
    tertiary = ASsoundAccent,
    onTertiary = TextOnPrimary,
    tertiaryContainer = ASsoundAccentVariant,
    onTertiaryContainer = ASsoundAccentLight,
    
    background = SurfaceDark,
    onBackground = TextPrimary,
    
    surface = SurfaceContainer,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = TextSecondary,
    
    surfaceContainerLowest = SurfaceDark,
    surfaceContainerLow = SurfaceContainer,
    surfaceContainer = SurfaceContainerHigh,
    surfaceContainerHigh = SurfaceContainerHighest,
    surfaceContainerHighest = SurfaceBright,
    
    outline = CardBorder,
    outlineVariant = CardBorderHighlight,
    
    error = StatusError,
    onError = TextOnPrimary,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    inverseSurface = TextPrimary,
    inverseOnSurface = SurfaceDark,
    inversePrimary = ASsoundPrimaryDark,
    
    scrim = Color(0xFF000000)
)

@Composable
fun ASsoundTheme(
    darkTheme: Boolean = true, // Force dark theme for this audio app
    content: @Composable () -> Unit
) {
    val colorScheme = ASsoundDarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ASsoundTypography,
        shapes = ASsoundShapes,
        content = content
    )
}
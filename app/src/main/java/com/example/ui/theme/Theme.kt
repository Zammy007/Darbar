package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarbarLightColorScheme = lightColorScheme(
    primary = DarbarNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7F7),
    onPrimaryContainer = DarbarNavyDark,
    secondary = DarbarTeal,
    onSecondary = Color.White,
    secondaryContainer = DarbarTealSoft,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = DarbarGold,
    onTertiary = Color.White,
    tertiaryContainer = DarbarGoldSoft,
    onTertiaryContainer = Color(0xFF5C3400),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = Color(0xFFE6E3DA),
    error = StatusErrorLight,
    onError = Color.White,
    errorContainer = StatusErrorContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarbarDarkColorScheme = darkColorScheme(
    primary = DarbarGoldBright,
    onPrimary = DarbarNavyDark,
    primaryContainer = Color(0xFF1F385C),
    onPrimaryContainer = Color(0xFFE2EEFF),
    secondary = DarbarTealLight,
    onSecondary = Color(0xFF052E2B),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = DarbarGoldBright,
    onTertiary = DarbarNavyDark,
    tertiaryContainer = Color(0xFF47310A),
    onTertiaryContainer = Color(0xFFFFE7B0),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E304C),
    error = StatusErrorDark,
    onError = Color(0xFF450A0A),
    errorContainer = StatusErrorContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

val DarbarShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DarbarTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarbarDarkColorScheme else DarbarLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DarbarTypography,
        shapes = DarbarShapes,
        content = content
    )
}

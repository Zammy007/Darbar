package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.rememberReducedMotionEnabled

private val DarbarLightColorScheme = lightColorScheme(
    primary = DarbarBlack,
    onPrimary = DarbarWhite,
    primaryContainer = Color(0xFFEEEEF0),
    onPrimaryContainer = DarbarBlack,
    secondary = DarbarCharcoal,
    onSecondary = DarbarWhite,
    secondaryContainer = Color(0xFFEAEAEF),
    onSecondaryContainer = DarbarBlack,
    tertiary = Color(0xFF27272A),
    onTertiary = DarbarWhite,
    tertiaryContainer = Color(0xFFF4F4F5),
    onTertiaryContainer = DarbarCharcoal,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = StatusErrorLight,
    onError = DarbarWhite,
    errorContainer = StatusErrorContainerLight,
    onErrorContainer = DarbarBlack
)

private val DarbarDarkColorScheme = darkColorScheme(
    primary = DarbarWhite,
    onPrimary = DarbarBlack,
    primaryContainer = Color(0xFF1F1F23),
    onPrimaryContainer = Color(0xFFFAFAFA),
    secondary = Color(0xFFE4E4E7),
    onSecondary = DarbarBlack,
    secondaryContainer = Color(0xFF26262A),
    onSecondaryContainer = Color(0xFFFAFAFA),
    tertiary = Color(0xFFD4D4D8),
    onTertiary = DarbarBlack,
    tertiaryContainer = Color(0xFF27272A),
    onTertiaryContainer = Color(0xFFF4F4F5),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = StatusErrorDark,
    onError = DarbarBlack,
    errorContainer = StatusErrorContainerDark,
    onErrorContainer = Color(0xFFFAFAFA)
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
    val reducedMotion = rememberReducedMotionEnabled()
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

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DarbarTypography,
            shapes = DarbarShapes,
            content = content
        )
    }
}

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CallAssistantColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val elevatedSurface: Color
)

val LocalCallAssistantColors = staticCompositionLocalOf {
    CallAssistantColors(
        success = EmeraldActive,
        successContainer = DeepEmeraldContainer,
        warning = AmberPulse,
        warningContainer = DeepAmberContainer,
        danger = CoralAlert,
        dangerContainer = DeepCoralContainer,
        elevatedSurface = ObsidianSurfaceHigh
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = SapphireIndigo,
    onSecondary = ObsidianBg,
    secondaryContainer = DeepIndigoContainer,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = AmberPulse,
    onTertiary = ObsidianBg,
    tertiaryContainer = DeepAmberContainer,
    onTertiaryContainer = AmberPulse,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = CoralAlert,
    onError = ObsidianBg,
    errorContainer = DeepCoralContainer,
    onErrorContainer = CoralAlert
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = TextPrimaryLight,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = LightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = TextPrimaryLight,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF9F1239)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AICallAssistantTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = darkTheme || isSystemInDarkTheme()
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val semanticColors = if (isDark) {
        CallAssistantColors(
            success = EmeraldActive,
            successContainer = DeepEmeraldContainer,
            warning = AmberPulse,
            warningContainer = DeepAmberContainer,
            danger = CoralAlert,
            dangerContainer = DeepCoralContainer,
            elevatedSurface = ObsidianSurfaceHigh
        )
    } else {
        CallAssistantColors(
            success = Color(0xFF059669),
            successContainer = Color(0xFFD1FAE5),
            warning = Color(0xFFD97706),
            warningContainer = Color(0xFFFEF3C7),
            danger = Color(0xFFE11D48),
            dangerContainer = Color(0xFFFFE4E6),
            elevatedSurface = Color(0xFFF1F5F9)
        )
    }

    CompositionLocalProvider(LocalCallAssistantColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

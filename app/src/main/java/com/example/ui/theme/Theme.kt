package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = ObsidianBg,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldPrimary,
    secondary = CyanSecondary,
    onSecondary = ObsidianBg,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanSecondary,
    tertiary = AmberSchedule,
    onTertiary = ObsidianBg,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberSchedule,
    error = CoralDanger,
    onError = TextPrimaryDark,
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralDanger,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = SlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SlateSurface,
    surfaceContainerHigh = SlateCardElevated,
    outline = SlateBorder
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = CrispSurface,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldDark,
    secondary = CyanDark,
    onSecondary = CrispSurface,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = CyanDark,
    tertiary = AmberDark,
    onTertiary = CrispSurface,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = AmberDark,
    error = CoralDark,
    onError = CrispSurface,
    errorContainer = CoralContainerLight,
    onErrorContainer = CoralDark,
    background = CloudBg,
    onBackground = TextPrimaryLight,
    surface = CrispSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = MistSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = CrispSurface,
    surfaceContainerHigh = MistSurfaceVariant,
    outline = MistBorder
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AppShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

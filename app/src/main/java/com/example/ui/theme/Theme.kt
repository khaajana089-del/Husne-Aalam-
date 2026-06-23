package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Force dark, luxury styling for CaptionX AI Studio
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNeonPink,
    secondary = SecondaryNeonGreen,
    tertiary = AccentNeonCyan,
    background = SlateBack,
    surface = SlateSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = SurfaceGlass,
    outline = SlateBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme
    dynamicColor: Boolean = false, // Disable dynamic pastels to preserve pristine studio identity
    content: @Composable () -> Unit
) {
    // We always use the custom DarkColorScheme to match the premium neon cyberpunk branding of CaptionX AI.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

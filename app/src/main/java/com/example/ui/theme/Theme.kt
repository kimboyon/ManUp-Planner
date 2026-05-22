package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CustomColorScheme = darkColorScheme(
    primary = SandGold,
    secondary = MutedGold,
    tertiary = BrightYellow,
    background = DeepForest,
    surface = SageCard,
    onPrimary = DeepForest,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceContainer = SageCardElevated,
    error = CoralRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark mode for premium, eye-safe clinical privacy
    dynamicColor: Boolean = false, // Use our handcrafted branding instead of system wallpaper tints
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CustomColorScheme,
        typography = Typography,
        content = content
    )
}

package com.bosankus.officeworks.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Placeholder brand palette until the creative work lands. Swap these values to rebrand;
// features only ever read MaterialTheme.colorScheme.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF2F6FED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF00205B),
    secondary = Color(0xFF5B6475),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6EAF2),
    onSecondaryContainer = Color(0xFF181C24),
    tertiary = Color(0xFFE8A317),
    onTertiary = Color(0xFF2B1D00),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF181B21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181B21),
    surfaceVariant = Color(0xFFF3F5F9),
    onSurfaceVariant = Color(0xFF6B7385),
    outline = Color(0xFFC9CED8),
    outlineVariant = Color(0xFFE4E7EE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB8FF),
    onPrimary = Color(0xFF002A75),
    primaryContainer = Color(0xFF1E4FB8),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFC1C7D4),
    onSecondary = Color(0xFF2B303A),
    secondaryContainer = Color(0xFF2A303B),
    onSecondaryContainer = Color(0xFFDDE2EC),
    tertiary = Color(0xFFFFC452),
    onTertiary = Color(0xFF3F2E00),
    background = Color(0xFF0F1216),
    onBackground = Color(0xFFE2E4EA),
    surface = Color(0xFF0F1216),
    onSurface = Color(0xFFE2E4EA),
    surfaceVariant = Color(0xFF1C2129),
    onSurfaceVariant = Color(0xFFA3AABA),
    outline = Color(0xFF4A5160),
    outlineVariant = Color(0xFF2A303B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

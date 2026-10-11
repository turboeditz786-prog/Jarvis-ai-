package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisColorScheme = darkColorScheme(
  primary = ArcCyan,
  onPrimary = HudBackground,
  primaryContainer = HudSurfaceVariant,
  onPrimaryContainer = ArcCyanGlow,
  secondary = ArcBlue,
  onSecondary = HudBackground,
  secondaryContainer = HudSurfaceVariant,
  onSecondaryContainer = ArcBlue,
  tertiary = StarkGold,
  onTertiary = HudBackground,
  background = HudBackground,
  onBackground = TextPrimary,
  surface = HudSurface,
  onSurface = TextPrimary,
  surfaceVariant = HudSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = HudBorder,
  outlineVariant = HudBorderGlow
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = JarvisColorScheme,
    typography = Typography,
    content = content
  )
}

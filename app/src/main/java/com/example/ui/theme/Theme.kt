package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClassWizDarkScheme = darkColorScheme(
  primary = BrandCyan,
  onPrimary = Color(0xFF0A1419),
  secondary = LegendSecondaryShift,
  onSecondary = Color(0xFF1A1200),
  background = DeskMatBackground,
  onBackground = BrandWhite,
  surface = ChassisDarkMid,
  onSurface = BrandWhite,
  surfaceVariant = KeyRoundDark,
  onSurfaceVariant = LegendMuted,
)

@Composable
fun ClassWizTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = ClassWizDarkScheme,
    typography = ClassWizTypography,
    content = content,
  )
}

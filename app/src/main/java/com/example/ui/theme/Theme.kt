package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema Moderno Branco / Translúcido com Glassmorphism
private val FinLinkGlassWhiteColorScheme = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F0FF),
    onPrimaryContainer = IosBlue,
    secondary = IosGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F8EE),
    tertiary = IosOrange,
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF1D1D1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1D1F),
    surfaceVariant = Color(0xFFECEEF3),
    onSurfaceVariant = Color(0xFF86868B),
    outline = Color(0xFFE2E4E9),
    outlineVariant = Color(0xFFECEEF2)
)

private val DarkColorScheme =
  darkColorScheme(
    primary = IosBlueDark,
    onPrimary = Color.Black,
    primaryContainer = IosDarkCardSecondary,
    onPrimaryContainer = IosBlueDark,
    secondary = IosGreenDark,
    onSecondary = Color.White,
    secondaryContainer = IosIndigoDark,
    tertiary = IosOrangeDark,
    background = IosDarkBackground,
    onBackground = IosDarkTextPrimary,
    surface = IosDarkCard,
    onSurface = IosDarkTextPrimary,
    surfaceVariant = IosDarkCardSecondary,
    onSurfaceVariant = IosDarkTextSecondary,
    outline = IosDarkBorder,
    outlineVariant = IosDarkDivider
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Padrão: Tema Branco / Translúcido
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else FinLinkGlassWhiteColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


package com.nyaai.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark palette ─────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFFB0C4DE),  // light-steel accent
    onPrimary        = Color(0xFF0D1B2A),
    secondary        = Color(0xFF8BA3BE),
    background       = Color(0xFF0D1B2A),  // deep navy
    surface          = Color(0xFF1C2A3A),  // card surface
    surfaceVariant   = Color(0xFF1C2A3A),  // input bar / icon bg
    onBackground     = Color(0xFFFFFFFF),
    onSurface        = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF8FAAB8),  // muted text / icon tint
    outline          = Color(0xFF2A3A4D),  // borders / dividers
)

// ── Light palette ─────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF1E3A8A),
    onPrimary        = Color.White,
    secondary        = Color(0xFF4A80C4),
    background       = Color(0xFFF2F5FA),  // clean off-white
    surface          = Color(0xFFFFFFFF),  // white cards
    surfaceVariant   = Color(0xFFEBEFF8),  // input bar / icon bg
    onBackground     = Color(0xFF0D1B2A),  // dark navy text
    onSurface        = Color(0xFF0D1B2A),
    onSurfaceVariant = Color(0xFF6B849E),  // muted blue-gray
    outline          = Color(0xFFD4DBE8),  // subtle borders
)

@Composable
fun NyaaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}

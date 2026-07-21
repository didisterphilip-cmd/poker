package com.homepoker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Table-felt greens and warm accents give a "home game" feel rather than neon casino.
val FeltGreen = Color(0xFF0B3D1E)
val FeltGreenLight = Color(0xFF14532D)
val Rail = Color(0xFF5A3B1E)
val Gold = Color(0xFFE8B84B)
val ChipRed = Color(0xFFC0392B)
val ChipBlue = Color(0xFF2E6BB8)
val CardCream = Color(0xFFF7F3E9)

private val Dark = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1206),
    secondary = ChipBlue,
    background = FeltGreen,
    onBackground = Color(0xFFEDE6D6),
    surface = FeltGreenLight,
    onSurface = Color(0xFFEDE6D6),
)

private val Light = Dark // The table always reads best on the dark felt palette.

@Composable
fun HomePokerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = Typography(),
        content = content,
    )
}

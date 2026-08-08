package com.aldef.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Palet HUD ala J.A.R.V.I.S. */
object Hud {
    val Background = Color(0xE6050A10)
    val Panel = Color(0x1400E5FF)
    val PanelBorder = Color(0x5500E5FF)
    val Cyan = Color(0xFF00E5FF)
    val CyanDim = Color(0xB300B8D4)
    val Amber = Color(0xFFFFB300)
    val Danger = Color(0xFFFF5252)
    val TextPrimary = Color(0xFFE3F7FB)
    val TextMuted = Color(0x9980DEEA)
    val Grid = Color(0x1A00E5FF)
}

private val HudColors = darkColorScheme(
    primary = Hud.Cyan,
    onPrimary = Color.Black,
    secondary = Hud.Amber,
    background = Hud.Background,
    onBackground = Hud.TextPrimary,
    surface = Hud.Panel,
    onSurface = Hud.TextPrimary,
    error = Hud.Danger,
)

private val HudTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Thin,
        fontSize = 72.sp,
        letterSpacing = 4.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 26.sp,
        letterSpacing = 8.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 17.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
    ),
)

@Composable
fun AldefTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme() // HUD selalu gelap; panggilan ini menjaga konsistensi sistem.
    MaterialTheme(
        colorScheme = HudColors,
        typography = HudTypography,
        content = content,
    )
}

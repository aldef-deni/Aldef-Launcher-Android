package com.aldef.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aldef.launcher.ui.theme.Hud

/*
 * Peta segmen tujuh-ruas:
 *
 *   aaaa
 *  f    b
 *  f    b
 *   gggg
 *  e    c
 *  e    c
 *   dddd
 */
private val SEGMENT_MAP: Map<Char, String> = mapOf(
    '0' to "abcdef",
    '1' to "bc",
    '2' to "abged",
    '3' to "abgcd",
    '4' to "fgbc",
    '5' to "afgcd",
    '6' to "afgedc",
    '7' to "abc",
    '8' to "abcdefg",
    '9' to "abcdfg",
)

/** Ruas horizontal berbentuk heksagon memanjang. */
private fun horizontalSegment(x: Float, y: Float, w: Float, t: Float): Path = Path().apply {
    moveTo(x + t / 2, y)
    lineTo(x + w - t / 2, y)
    lineTo(x + w, y + t / 2)
    lineTo(x + w - t / 2, y + t)
    lineTo(x + t / 2, y + t)
    lineTo(x, y + t / 2)
    close()
}

/** Ruas vertikal berbentuk heksagon memanjang. */
private fun verticalSegment(x: Float, y: Float, h: Float, t: Float): Path = Path().apply {
    moveTo(x, y + t / 2)
    lineTo(x + t / 2, y)
    lineTo(x + t, y + t / 2)
    lineTo(x + t, y + h - t / 2)
    lineTo(x + t / 2, y + h)
    lineTo(x, y + h - t / 2)
    close()
}

/**
 * Menggambar satu digit. Ruas yang mati tetap digambar samar — inilah yang
 * memberi kesan panel LED sungguhan, bukan sekadar teks bergaya.
 */
private fun DrawScope.drawSevenSegment(
    char: Char,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    lit: Color,
    dim: Color,
) {
    val t = h * 0.13f
    val vh = (h + t) / 2f
    val active = SEGMENT_MAP[char].orEmpty()

    val segments = listOf(
        'a' to horizontalSegment(x, y, w, t),
        'g' to horizontalSegment(x, y + (h - t) / 2f, w, t),
        'd' to horizontalSegment(x, y + h - t, w, t),
        'f' to verticalSegment(x, y, vh, t),
        'b' to verticalSegment(x + w - t, y, vh, t),
        'e' to verticalSegment(x, y + (h - t) / 2f, vh, t),
        'c' to verticalSegment(x + w - t, y + (h - t) / 2f, vh, t),
    )

    segments.forEach { (key, path) ->
        if (active.contains(key)) {
            // Pendar tipis di belakang ruas yang menyala.
            drawPath(path, color = lit.copy(alpha = 0.20f), style = Stroke(width = t * 0.85f))
            drawPath(path, color = lit)
        } else {
            drawPath(path, color = dim)
        }
    }
}

/**
 * Jam digital HUD tujuh-ruas.
 *
 * @param hhmm    format "HH:mm"
 * @param seconds dua digit detik, dipakai untuk blok kecil dan kedip titik dua
 */
@Composable
fun HudDigitalClock(
    hhmm: String,
    seconds: String,
    modifier: Modifier = Modifier,
    lit: Color = Hud.Cyan,
    accent: Color = Hud.Cyan,
) {
    val digits = hhmm.filter { it.isDigit() }.padStart(4, '0').take(4)
    val secDigits = seconds.filter { it.isDigit() }.padStart(2, '0').take(2)
    val blinkOn = (seconds.toIntOrNull() ?: 0) % 2 == 0

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
    ) {
        val dim = lit.copy(alpha = 0.07f)

        val h = size.height * 0.82f
        val w = h * 0.54f
        val gap = w * 0.20f
        val colonW = w * 0.44f

        val secH = h * 0.34f
        val secW = secH * 0.54f
        val secGap = secW * 0.22f
        val secBlockW = secW * 2 + secGap
        val secLead = w * 0.42f

        val totalW = w * 4 + gap * 3 + colonW + secLead + secBlockW
        var x = (size.width - totalW) / 2f
        val y = (size.height - h) / 2f

        // Jam
        drawSevenSegment(digits[0], x, y, w, h, lit, dim); x += w + gap
        drawSevenSegment(digits[1], x, y, w, h, lit, dim); x += w + gap

        // Titik dua berkedip tiap detik
        val dotR = w * 0.075f
        val colonX = x + colonW / 2f
        val colonColor = if (blinkOn) accent else accent.copy(alpha = 0.18f)
        drawCircle(colonColor, dotR, Offset(colonX, y + h * 0.33f))
        drawCircle(colonColor, dotR, Offset(colonX, y + h * 0.67f))
        x += colonW + gap

        // Menit
        drawSevenSegment(digits[2], x, y, w, h, lit, dim); x += w + gap
        drawSevenSegment(digits[3], x, y, w, h, lit, dim); x += w + secLead

        // Detik, lebih kecil dan sejajar bagian bawah
        val secY = y + h - secH
        drawSevenSegment(secDigits[0], x, secY, secW, secH, accent, dim); x += secW + secGap
        drawSevenSegment(secDigits[1], x, secY, secW, secH, accent, dim)
    }
}

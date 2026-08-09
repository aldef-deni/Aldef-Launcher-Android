package com.aldef.launcher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aldef.launcher.ui.theme.Hud

/**
 * Bingkai penargetan: siku di empat sudut layar plus tanda tengah di tiap sisi.
 * Elemen ini membingkai seluruh layar kunci dan tidak dipakai di layar depan.
 */
@Composable
fun TargetingFrame(modifier: Modifier = Modifier, color: Color = Hud.Cyan) {
    val transition = rememberInfiniteTransition(label = "frame")
    val breathe by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(3_200, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe",
    )

    Canvas(modifier.fillMaxSize()) {
        val inset = 18.dp.toPx()
        val arm = 30.dp.toPx()
        val stroke = 1.6f
        val c = color.copy(alpha = breathe)

        val l = inset
        val t = inset
        val r = size.width - inset
        val b = size.height - inset

        // Siku sudut
        drawLine(c, Offset(l, t), Offset(l + arm, t), stroke)
        drawLine(c, Offset(l, t), Offset(l, t + arm), stroke)
        drawLine(c, Offset(r, t), Offset(r - arm, t), stroke)
        drawLine(c, Offset(r, t), Offset(r, t + arm), stroke)
        drawLine(c, Offset(l, b), Offset(l + arm, b), stroke)
        drawLine(c, Offset(l, b), Offset(l, b - arm), stroke)
        drawLine(c, Offset(r, b), Offset(r - arm, b), stroke)
        drawLine(c, Offset(r, b), Offset(r, b - arm), stroke)

        // Tanda tengah tiap sisi
        val tick = 10.dp.toPx()
        val midX = size.width / 2f
        val midY = size.height / 2f
        val faint = color.copy(alpha = breathe * 0.6f)
        drawLine(faint, Offset(midX, t), Offset(midX, t + tick), stroke)
        drawLine(faint, Offset(midX, b), Offset(midX, b - tick), stroke)
        drawLine(faint, Offset(l, midY), Offset(l + tick, midY), stroke)
        drawLine(faint, Offset(r, midY), Offset(r - tick, midY), stroke)
    }
}

/**
 * Radar sapu: garis berputar dengan ekor memudar di dalam cincin bertingkat.
 * Menggantikan arc reactor sebagai penanda "sistem hidup" khusus layar kunci.
 */
@Composable
fun RadarSweep(
    modifier: Modifier = Modifier,
    diameter: Dp = 210.dp,
    color: Color = Hud.Cyan,
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3_600, easing = LinearEasing)),
        label = "angle",
    )
    val ping by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_600, easing = LinearEasing)),
        label = "ping",
    )

    Canvas(modifier.size(diameter)) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Cincin bertingkat
        listOf(1f, 0.72f, 0.44f).forEachIndexed { index, factor ->
            drawCircle(
                color = color.copy(alpha = if (index == 0) 0.4f else 0.18f),
                radius = r * factor,
                center = center,
                style = Stroke(width = if (index == 0) 1.8f else 1f),
            )
        }

        // Garis silang
        drawLine(color.copy(alpha = 0.14f), Offset(center.x - r, center.y), Offset(center.x + r, center.y), 1f)
        drawLine(color.copy(alpha = 0.14f), Offset(center.x, center.y - r), Offset(center.x, center.y + r), 1f)

        // Ekor sapu
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    color.copy(alpha = 0f),
                    color.copy(alpha = 0.30f),
                ),
                center = center,
            ),
            startAngle = angle - 70f,
            sweepAngle = 70f,
            useCenter = true,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(r * 2, r * 2),
        )

        // Garis sapu
        val rad = Math.toRadians(angle.toDouble())
        drawLine(
            color = color,
            start = center,
            end = Offset(
                center.x + r * kotlin.math.cos(rad).toFloat(),
                center.y + r * kotlin.math.sin(rad).toFloat(),
            ),
            strokeWidth = 2f,
        )

        // Gelombang ping keluar
        drawCircle(
            color = color.copy(alpha = (1f - ping) * 0.35f),
            radius = r * ping,
            center = center,
            style = Stroke(width = 1.5f),
        )

        drawCircle(color = color, radius = 3f, center = center)
    }
}

/** Pengukur baterai vertikal bergaya bilah HUD di tepi layar. */
@Composable
fun VerticalBatteryGauge(
    percent: Int,
    charging: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = when {
        percent <= 15 && !charging -> Hud.Danger
        charging -> Hud.Amber
        else -> Hud.Cyan
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val segments = 16
        val gap = h * 0.012f
        val segH = (h - gap * (segments - 1)) / segments
        val filled = (segments * percent / 100f).toInt().coerceIn(0, segments)

        for (i in 0 until segments) {
            // Ruas paling bawah mewakili daya tersisa.
            val top = h - (i + 1) * segH - i * gap
            val on = i < filled
            drawRect(
                color = if (on) accent else accent.copy(alpha = 0.10f),
                topLeft = Offset(0f, top),
                size = Size(w, segH),
            )
        }

        drawRect(
            color = accent.copy(alpha = 0.5f),
            topLeft = Offset(-3f, -3f),
            size = Size(w + 6f, h + 6f),
            style = Stroke(width = 1f),
        )
    }
}

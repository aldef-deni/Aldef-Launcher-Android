package com.aldef.launcher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aldef.launcher.R
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
 * Emblem logo Aldef dengan cincin HUD yang **berayun kiri–kanan** di belakangnya.
 *
 * Cincin sengaja digambar lebih lebar daripada logo, sebab logo sudah membawa
 * cincinnya sendiri di dalam gambar; kalau radiusnya berdekatan, keduanya
 * bertabrakan secara visual. Dua lapis cincin berayun berlawanan arah supaya
 * gerakannya terbaca tanpa perlu berputar penuh.
 */
@Composable
fun LogoEmblem(
    modifier: Modifier = Modifier,
    diameter: Dp = 230.dp,
    color: Color = Hud.Cyan,
    warm: Color = Hud.Amber,
) {
    val transition = rememberInfiniteTransition(label = "emblem")

    val swingOuter by transition.animateFloat(
        initialValue = -22f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(4_200, easing = LinearEasing), RepeatMode.Reverse),
        label = "swingOuter",
    )
    val swingInner by transition.animateFloat(
        initialValue = 16f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(tween(3_100, easing = LinearEasing), RepeatMode.Reverse),
        label = "swingInner",
    )
    val glow by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Pendar lembut di belakang logo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = glow * 0.22f), Color.Transparent),
                    center = center,
                    radius = r * 0.72f,
                ),
                radius = r * 0.72f,
                center = center,
            )

            // Cincin luar: ruas putus-putus, berayun ke satu arah
            rotateCanvas(degrees = swingOuter, pivot = center) {
                for (i in 0 until 24) {
                    val on = i % 2 == 0
                    if (!on) continue
                    drawArc(
                        color = if (i % 8 == 0) warm.copy(alpha = 0.75f) else color.copy(alpha = 0.5f),
                        startAngle = i * 15f,
                        sweepAngle = 9f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 2.5f),
                    )
                }
            }

            // Cincin tengah: garis utuh dengan celah, berayun berlawanan
            rotateCanvas(degrees = swingInner, pivot = center) {
                val rm = r * 0.86f
                listOf(0f to 96f, 130f to 74f, 236f to 88f).forEach { (start, sweep) ->
                    drawArc(
                        color = color.copy(alpha = 0.45f),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - rm, center.y - rm),
                        size = Size(rm * 2, rm * 2),
                        style = Stroke(width = 1.6f),
                    )
                }

                // Tanda skala kecil di sepanjang cincin tengah
                for (i in 0 until 36) {
                    val rad = Math.toRadians(i * 10.0)
                    val cos = kotlin.math.cos(rad).toFloat()
                    val sin = kotlin.math.sin(rad).toFloat()
                    val inner = rm * 0.94f
                    drawLine(
                        color = color.copy(alpha = 0.28f),
                        start = Offset(center.x + inner * cos, center.y + inner * sin),
                        end = Offset(center.x + rm * cos, center.y + rm * sin),
                        strokeWidth = 1f,
                    )
                }
            }
        }

        // Logo diam di tengah — hanya cincin di belakangnya yang bergerak.
        Image(
            painter = painterResource(R.drawable.aldef_logo),
            contentDescription = null,
            modifier = Modifier.size(diameter * 0.62f),
        )
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

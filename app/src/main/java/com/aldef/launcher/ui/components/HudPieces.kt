package com.aldef.launcher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateCanvas
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aldef.launcher.ui.theme.Hud

/** Garis pemisah horizontal bergaya HUD. */
@Composable
fun HudDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Hud.Cyan.copy(alpha = 0.7f), Color.Transparent),
            ),
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
        )
    }
}

// ---------------------------------------------------------------- Bingkai

/** Segi delapan (sudut terpotong) — bentuk dasar panel dan ikon HUD. */
private fun cutCornerPath(size: Size, cut: Float): Path = Path().apply {
    moveTo(cut, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(cut, size.height)
    lineTo(0f, size.height - cut)
    lineTo(0f, cut)
    close()
}

/** Siku penanda di keempat sudut, khas panel HUD. */
private fun DrawScope.drawCornerTicks(color: Color, length: Float, inset: Float, width: Float) {
    val w = size.width
    val h = size.height
    listOf(
        Offset(inset, inset) to listOf(Offset(inset + length, inset), Offset(inset, inset + length)),
        Offset(w - inset, inset) to listOf(Offset(w - inset - length, inset), Offset(w - inset, inset + length)),
        Offset(inset, h - inset) to listOf(Offset(inset + length, h - inset), Offset(inset, h - inset - length)),
        Offset(w - inset, h - inset) to listOf(
            Offset(w - inset - length, h - inset),
            Offset(w - inset, h - inset - length),
        ),
    ).forEach { (corner, arms) ->
        arms.forEach { arm -> drawLine(color, corner, arm, strokeWidth = width) }
    }
}

/** Panel bersudut potong dengan siku di setiap sudut. */
@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    accent: Color = Hud.Cyan,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val cut = 10.dp.toPx()
            val path = cutCornerPath(size, cut)
            drawPath(path, color = accent.copy(alpha = 0.07f))
            drawPath(path, color = accent.copy(alpha = 0.45f), style = Stroke(width = 1.4f))
            drawCornerTicks(accent, length = 10.dp.toPx(), inset = 3.dp.toPx(), width = 2f)
        }
        content()
    }
}

// ----------------------------------------------------------- Kartu status

/**
 * Kartu status HUD.
 *
 * @param value teks utama, dibuat besar agar terbaca sekilas
 * @param sub   keterangan kecil di bawah nilai (opsional)
 */
@Composable
fun StatusTile(
    icon: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    accent: Color = Hud.Cyan,
) {
    HudPanel(modifier = modifier, accent = accent) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(icon, style = MaterialTheme.typography.labelSmall, color = accent)
                Text(title, style = MaterialTheme.typography.labelSmall, color = Hud.TextMuted)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!sub.isNullOrBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = Hud.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// -------------------------------------------------------------- Ikon HUD

/**
 * Matriks warna yang mengubah ikon aplikasi berwarna-warni menjadi monokrom
 * sian. Luminansi asli dipertahankan supaya bentuk ikon tetap dikenali,
 * lalu dipetakan ke kanal hijau-biru agar menyatu dengan tema HUD.
 */
private val hudIconFilter: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.0299f, 0.0587f, 0.0114f, 0f, 10f, // R — ditekan
            0.2542f, 0.4990f, 0.0969f, 0f, 30f, // G
            0.2990f, 0.5870f, 0.1140f, 0f, 45f, // B — paling terang
            0f, 0f, 0f, 1f, 0f, // alpha apa adanya
        ),
    ),
)

/** Ikon aplikasi dalam bingkai heksagonal HUD, bukan lingkaran standar. */
@Composable
fun HudAppIcon(
    icon: ImageBitmap?,
    fallbackLetter: String,
    modifier: Modifier = Modifier,
    tileSize: Dp = 58.dp,
) {
    val filter = remember { hudIconFilter }

    Box(modifier = modifier.size(tileSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cut = size.minDimension * 0.26f
            val path = cutCornerPath(size, cut)
            drawPath(path, color = Hud.Cyan.copy(alpha = 0.10f))
            drawPath(path, color = Hud.Cyan.copy(alpha = 0.55f), style = Stroke(width = 1.6f))
        }

        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                colorFilter = filter,
                modifier = Modifier.size(tileSize * 0.56f),
            )
        } else {
            Text(
                text = fallbackLetter,
                style = MaterialTheme.typography.titleSmall,
                color = Hud.Cyan,
            )
        }
    }
}

// ---------------------------------------------------------- Arc reactor

/**
 * Arc reactor: dua cincin berputar berlawanan arah + inti berdenyut.
 * Warna ikut status (mendengar = amber, berpikir = putih-sian).
 */
@Composable
fun ArcReactor(
    active: Boolean,
    thinking: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "reactor")
    val spinSpeed = if (active || thinking) 2_600 else 9_000

    val outer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(spinSpeed, easing = LinearEasing)),
        label = "outer",
    )
    val inner by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(spinSpeed + 1_800, easing = LinearEasing)),
        label = "inner",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (active) 500 else 1_800, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val color = when {
        thinking -> Hud.TextPrimary
        active -> Hud.Amber
        else -> Hud.Cyan
    }

    Canvas(modifier = modifier.size(150.dp).rotate(outer)) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2, size.height / 2)

        for (i in 0 until 12) {
            drawArc(
                color = color.copy(alpha = 0.55f),
                startAngle = i * 30f,
                sweepAngle = 18f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 3f),
            )
        }

        rotateCanvas(degrees = inner - outer, pivot = center) {
            drawCircle(
                color = color.copy(alpha = 0.35f),
                radius = r * 0.72f,
                center = center,
                style = Stroke(width = 2f),
            )
            for (i in 0 until 6) {
                drawArc(
                    color = color,
                    startAngle = i * 60f + 8f,
                    sweepAngle = 26f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 0.55f, center.y - r * 0.55f),
                    size = Size(r * 1.1f, r * 1.1f),
                    style = Stroke(width = 5f),
                )
            }
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = pulse), color.copy(alpha = 0f)),
                center = center,
                radius = r * 0.5f * pulse,
            ),
            radius = r * 0.5f * pulse,
            center = center,
        )
        drawCircle(color = color, radius = r * 0.16f * pulse, center = center)
    }
}

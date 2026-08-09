package com.aldef.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldef.launcher.LockState
import com.aldef.launcher.ui.components.HudHollowClock
import com.aldef.launcher.ui.components.RadarSweep
import com.aldef.launcher.ui.components.TargetingFrame
import com.aldef.launcher.ui.components.VerticalBatteryGauge
import com.aldef.launcher.ui.components.swipeUpToOpen
import com.aldef.launcher.ui.theme.Hud

/**
 * Layar kunci Aldef.
 *
 * Sengaja dibuat berbeda dari layar depan: bingkai penargetan di tepi layar,
 * jam berongga (bukan padat), radar sapu (bukan arc reactor), dan pengukur
 * baterai vertikal di sisi kanan. Tidak ada kartu status maupun laci aplikasi —
 * layar ini hanya untuk dilihat sekilas, lalu dibuka.
 */
@Composable
fun LockScreen(state: LockState, onUnlock: () -> Unit) {
    val id = state.isIndonesian

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Hud.Background)
            .swipeUpToOpen(thresholdPx = 160f) { onUnlock() },
    ) {
        ScanlineBackdrop(Modifier.fillMaxSize())
        TargetingFrame()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(58.dp))

            // ---- Baris status atas ------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LockIndicator()
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (id) "TERKUNCI" else "LOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Hud.Amber,
                    )
                }
                Text(
                    text = state.network,
                    style = MaterialTheme.typography.labelSmall,
                    color = Hud.CyanDim,
                )
            }

            Spacer(Modifier.height(62.dp))

            // ---- Jam berongga -------------------------------------------------
            Text(
                text = state.dayName,
                style = MaterialTheme.typography.labelSmall,
                color = Hud.Cyan,
            )
            Spacer(Modifier.height(10.dp))

            HudHollowClock(
                hhmm = state.time,
                blinkOn = state.seconds % 2 == 0,
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    append(state.dateLine)
                    if (state.zoneLabel.isNotBlank()) append("  ·  ${state.zoneLabel}")
                },
                style = MaterialTheme.typography.labelSmall,
                color = Hud.TextMuted,
            )

            Spacer(Modifier.height(28.dp))

            // ---- Sapaan tipis --------------------------------------------------
            Text(
                text = buildString {
                    append(state.greeting)
                    if (state.userName.isNotBlank()) append(", ${state.userName.uppercase()}")
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
                color = Hud.TextPrimary,
                textAlign = TextAlign.Center,
            )

            // ---- Radar + baterai ------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                RadarSweep()

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.46f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "${state.battery}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.charging) Hud.Amber else Hud.Cyan,
                    )
                    Spacer(Modifier.height(8.dp))
                    VerticalBatteryGauge(
                        percent = state.battery,
                        charging = state.charging,
                        modifier = Modifier
                            .width(9.dp)
                            .weight(1f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.charging) "CHG" else "PWR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Hud.TextMuted,
                    )
                }

                // Cuaca dari cache HUD, di sisi kiri agar seimbang dengan baterai.
                if (state.temperature != null) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = state.weatherIcon,
                            style = MaterialTheme.typography.labelSmall,
                            color = Hud.Cyan,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${state.temperature}°",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            color = Hud.TextPrimary,
                        )
                        Text(
                            text = state.weatherText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Hud.TextMuted,
                        )
                    }
                }
            }

            // ---- Petunjuk buka ---------------------------------------------------
            if (state.place.isNotBlank()) {
                Text(
                    text = "📍  ${state.place}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Hud.TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            }

            UnlockHint(isIndonesian = id)
            Spacer(Modifier.height(34.dp))
        }
    }
}

/** Titik status berdenyut di samping tulisan TERKUNCI. */
@Composable
private fun LockIndicator() {
    val transition = rememberInfiniteTransition(label = "lockdot")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_100, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Canvas(Modifier.size(7.dp)) {
        drawCircle(Hud.Amber.copy(alpha = alpha))
    }
}

/** Tiga tanda panah yang menyala bergiliran ke atas. */
@Composable
private fun UnlockHint(isIndonesian: Boolean) {
    val transition = rememberInfiniteTransition(label = "hint")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1_500, easing = LinearEasing)),
        label = "phase",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .width(38.dp)
                .height(30.dp),
        ) {
            val step = size.height / 3f
            for (i in 0 until 3) {
                // Indeks dibalik agar nyala berjalan ke atas.
                val lit = (2 - i) == phase.toInt()
                val y = i * step + step * 0.8f
                val color = Hud.Cyan.copy(alpha = if (lit) 0.95f else 0.22f)
                drawLine(color, Offset(0f, y), Offset(size.width / 2f, y - step * 0.65f), 2.5f)
                drawLine(color, Offset(size.width, y), Offset(size.width / 2f, y - step * 0.65f), 2.5f)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isIndonesian) "GESER ATAS UNTUK MEMBUKA" else "SWIPE UP TO UNLOCK",
            style = MaterialTheme.typography.labelSmall,
            color = Hud.TextMuted,
        )
    }
}

/** Garis pindai horizontal samar — memberi kesan panel kaca. */
@Composable
private fun ScanlineBackdrop(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "offset",
    )

    Canvas(modifier) {
        var y = 0f
        val step = 5.dp.toPx()
        while (y < size.height) {
            drawLine(
                color = Color.White.copy(alpha = 0.012f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += step
        }

        // Satu pita terang yang bergerak turun perlahan.
        val bandY = size.height * offset
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Hud.Cyan.copy(alpha = 0.05f), Color.Transparent),
                startY = bandY - 90f,
                endY = bandY + 90f,
            ),
            topLeft = Offset(0f, bandY - 90f),
            size = androidx.compose.ui.geometry.Size(size.width, 180f),
        )
    }
}

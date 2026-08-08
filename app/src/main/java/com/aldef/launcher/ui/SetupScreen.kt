package com.aldef.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldef.launcher.ui.components.HudDivider
import com.aldef.launcher.ui.components.HudPanel
import com.aldef.launcher.ui.components.tapOnly
import com.aldef.launcher.ui.theme.Hud
import kotlinx.coroutines.delay

/**
 * Layar yang muncul ketika ikon Aldef diketuk dari launcher lain.
 * Berfungsi sebagai gerbang aktivasi antarmuka HUD.
 */
@Composable
fun SetupScreen(
    enabled: Boolean,
    isDefaultLauncher: Boolean,
    isIndonesian: Boolean,
    /** true setelah izin selesai diproses — barulah urutan boot dijalankan. */
    booting: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onChooseLauncher: () -> Unit,
    onOpenHud: () -> Unit,
    onBootFinished: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Hud.Background),
    ) {
        HexGrid(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("SYS · ALDEF", style = MaterialTheme.typography.labelSmall, color = Hud.CyanDim)
                Text("v1.0", style = MaterialTheme.typography.labelSmall, color = Hud.TextMuted)
            }

            Spacer(Modifier.height(10.dp))
            HudDivider()
            Spacer(Modifier.height(48.dp))

            Emblem(active = enabled)

            Spacer(Modifier.height(32.dp))

            Text(
                text = "A L D E F",
                fontSize = 38.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = 12.sp,
                color = Hud.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isIndonesian) "ANTARMUKA HUD · ASISTEN AI" else "HUD INTERFACE · AI ASSISTANT",
                style = MaterialTheme.typography.labelSmall,
                color = Hud.CyanDim,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(44.dp))

            // ---- Saklar utama --------------------------------------------
            HudPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (isIndonesian) "ANTARMUKA HUD" else "HUD INTERFACE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Hud.TextMuted,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = if (enabled) "ONLINE" else "STANDBY",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (enabled) Hud.Cyan else Hud.Amber,
                            )
                        }
                        HudSwitch(
                            checked = enabled,
                            onCheckedChange = { on -> if (on) onActivate() else onDeactivate() },
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HudDivider()
                    Spacer(Modifier.height(14.dp))

                    ReadoutRow(
                        label = if (isIndonesian) "LAUNCHER UTAMA" else "DEFAULT LAUNCHER",
                        value = when {
                            isDefaultLauncher -> "ALDEF"
                            isIndonesian -> "BELUM DIATUR"
                            else -> "NOT SET"
                        },
                        ok = isDefaultLauncher,
                    )
                    Spacer(Modifier.height(8.dp))
                    ReadoutRow(
                        label = if (isIndonesian) "MODE TAMPILAN" else "DISPLAY MODE",
                        value = if (enabled) "HUD" else if (isIndonesian) "NONAKTIF" else "DISABLED",
                        ok = enabled,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (enabled) {
                ActionButton(
                    label = if (isDefaultLauncher) {
                        if (isIndonesian) "BUKA HUD" else "OPEN HUD"
                    } else {
                        if (isIndonesian) "PILIH ALDEF SEBAGAI LAUNCHER" else "SET ALDEF AS LAUNCHER"
                    },
                    onClick = { if (isDefaultLauncher) onOpenHud() else onChooseLauncher() },
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = if (isIndonesian) {
                    "Menyalakan saklar akan memulai ulang sistem agar antarmuka HUD " +
                        "dimuat dari keadaan bersih."
                } else {
                    "Turning the switch on restarts the system so the HUD loads from a clean state."
                },
                style = MaterialTheme.typography.labelSmall,
                color = Hud.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 36.dp),
            )
        }

        if (booting) {
            BootOverlay(isIndonesian = isIndonesian, onFinished = onBootFinished)
        }
    }
}

/**
 * Ditampilkan bila MainActivity berjalan sebagai Home padahal saklar HUD mati.
 * Mencegah pengguna terjebak di layar kosong tanpa jalan keluar.
 */
@Composable
fun HudDisabledScreen(isIndonesian: Boolean, onOpenSetup: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Hud.Background),
        contentAlignment = Alignment.Center,
    ) {
        HexGrid(Modifier.fillMaxSize())

        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Emblem(active = false)
            Spacer(Modifier.height(28.dp))
            Text(
                text = if (isIndonesian) "ANTARMUKA HUD NONAKTIF" else "HUD INTERFACE DISABLED",
                style = MaterialTheme.typography.labelSmall,
                color = Hud.Amber,
            )
            Spacer(Modifier.height(20.dp))
            ActionButton(
                label = if (isIndonesian) "BUKA PANEL AKTIVASI" else "OPEN ACTIVATION PANEL",
                onClick = onOpenSetup,
            )
        }
    }
}

// ------------------------------------------------------------------ bagian

@Composable
private fun ReadoutRow(label: String, value: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Hud.TextMuted)
        Text(
            text = "${if (ok) "◆" else "◇"}  $value",
            style = MaterialTheme.typography.labelSmall,
            color = if (ok) Hud.Cyan else Hud.Amber,
        )
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    HudPanel(
        modifier = Modifier
            .fillMaxWidth()
            .tapOnly(onClick),
        accent = Hud.Cyan,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "▸  $label",
                style = MaterialTheme.typography.labelSmall,
                color = Hud.Cyan,
            )
        }
    }
}

/** Saklar bergaya HUD: rel bersudut potong dengan tuas geser. */
@Composable
private fun HudSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 38.dp else 4.dp,
        animationSpec = tween(260),
        label = "knob",
    )
    val accent = if (checked) Hud.Cyan else Hud.TextMuted

    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 36.dp)
            .tapOnly { onCheckedChange(!checked) },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cut = 8.dp.toPx()
            val path = androidx.compose.ui.graphics.Path().apply {
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
            drawPath(path, color = accent.copy(alpha = if (checked) 0.16f else 0.06f))
            drawPath(path, color = accent.copy(alpha = 0.6f), style = Stroke(width = 1.5f))
        }

        Box(
            modifier = Modifier
                .padding(start = knobOffset, top = 4.dp, bottom = 4.dp)
                .size(width = 34.dp, height = 28.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(color = accent.copy(alpha = 0.9f))
                drawRect(color = Hud.Background.copy(alpha = 0.5f), style = Stroke(width = 2f))
            }
        }

        Text(
            text = if (checked) "ON" else "OFF",
            style = MaterialTheme.typography.labelSmall,
            color = if (checked) Hud.Background else Hud.TextMuted,
            // Label diletakkan di sisi berlawanan dari tuas agar tidak tertimpa.
            modifier = Modifier
                .align(if (checked) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 11.dp),
        )
    }
}

/** Emblem heksagonal berputar di tengah layar aktivasi. */
@Composable
private fun Emblem(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "emblem")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (active) 14_000 else 26_000, easing = LinearEasing)),
        label = "spin",
    )
    val glow by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val color = if (active) Hud.Cyan else Hud.CyanDim

    Canvas(Modifier.size(128.dp).rotate(spin)) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2, size.height / 2)

        // Heksagon luar
        val hex = androidx.compose.ui.graphics.Path()
        for (i in 0 until 6) {
            val angle = Math.toRadians((60.0 * i) - 30.0)
            val x = center.x + r * kotlin.math.cos(angle).toFloat()
            val y = center.y + r * kotlin.math.sin(angle).toFloat()
            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
        }
        hex.close()
        drawPath(hex, color = color.copy(alpha = 0.5f), style = Stroke(width = 2f))

        // Busur bagian dalam
        for (i in 0 until 4) {
            drawArc(
                color = color.copy(alpha = 0.8f),
                startAngle = i * 90f + 12f,
                sweepAngle = 46f,
                useCenter = false,
                topLeft = Offset(center.x - r * 0.62f, center.y - r * 0.62f),
                size = Size(r * 1.24f, r * 1.24f),
                style = Stroke(width = 3f),
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = glow), Color.Transparent),
                center = center,
                radius = r * 0.45f,
            ),
            radius = r * 0.45f,
            center = center,
        )
    }
}

/** Latar belakang garis heksagon tipis. */
@Composable
private fun HexGrid(modifier: Modifier) {
    Canvas(modifier) {
        val step = 74f
        var y = 0f
        var row = 0
        while (y < size.height + step) {
            var x = if (row % 2 == 0) 0f else step / 2
            while (x < size.width + step) {
                drawCircle(
                    color = Hud.Grid,
                    radius = 1.6f,
                    center = Offset(x, y),
                )
                x += step
            }
            y += step * 0.72f
            row++
        }
    }
}

/** Urutan boot yang tampil setelah saklar dinyalakan. */
@Composable
private fun BootOverlay(isIndonesian: Boolean, onFinished: () -> Unit) {
    val steps = if (isIndonesian) {
        listOf(
            "MEMUAT MODUL HUD",
            "MENGKALIBRASI SENSOR PERANGKAT",
            "MENYIAPKAN ASISTEN SUARA",
            "MENGUNCI KONFIGURASI",
            "MEMULAI ULANG SISTEM",
        )
    } else {
        listOf(
            "LOADING HUD MODULES",
            "CALIBRATING DEVICE SENSORS",
            "PREPARING VOICE ASSISTANT",
            "LOCKING CONFIGURATION",
            "RESTARTING SYSTEM",
        )
    }

    var visibleSteps by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        steps.indices.forEach { index ->
            delay(480)
            visibleSteps = index + 1
        }
        delay(700)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Hud.Background.copy(alpha = 0.97f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = if (isIndonesian) "MENGAKTIFKAN ANTARMUKA" else "ACTIVATING INTERFACE",
                style = MaterialTheme.typography.labelSmall,
                color = Hud.Cyan,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .width(150.dp)
                    .height(1.dp)
                    .background(Hud.PanelBorder),
            )
            Spacer(Modifier.height(20.dp))

            steps.take(visibleSteps).forEach { step ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Text(
                        text = "▸ ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Hud.Cyan,
                    )
                    Text(
                        text = step,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Hud.TextPrimary,
                    )
                }
            }
        }
    }
}

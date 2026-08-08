package com.aldef.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldef.launcher.HudState
import com.aldef.launcher.ui.components.ArcReactor
import com.aldef.launcher.ui.components.HudDivider
import com.aldef.launcher.ui.components.HudPanel
import com.aldef.launcher.ui.components.StatusTile
import com.aldef.launcher.ui.components.swipeUpToOpen
import com.aldef.launcher.ui.components.tapOnly
import com.aldef.launcher.ui.theme.Hud

@Composable
fun HudScreen(
    state: HudState,
    onMicClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onGreetAgain: () -> Unit,
) {
    val id = state.language == "id"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Hud.Background)
            .swipeUpToOpen { onOpenDrawer() }
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(44.dp))

        // Bilah atas hanya berisi tombol pengaturan — tanpa teks merek.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "⚙",
                fontSize = 18.sp,
                color = Hud.CyanDim,
                modifier = Modifier
                    .clickable { onOpenSettings() }
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        HudDivider()
        Spacer(Modifier.height(24.dp))

        // ---- Jam + sapaan ---------------------------------------------------
        Text(
            text = state.time,
            style = MaterialTheme.typography.displayLarge,
            color = Hud.TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.date,
            style = MaterialTheme.typography.labelSmall,
            color = Hud.TextMuted,
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = state.greeting,
            style = MaterialTheme.typography.titleLarge,
            color = Hud.Cyan,
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.userName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = Hud.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.tapOnly { onGreetAgain() },
        )

        Spacer(Modifier.height(22.dp))
        HudDivider()
        Spacer(Modifier.height(18.dp))

        // ---- Empat kartu status ---------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusTile(
                icon = "⚡",
                title = "POWER",
                value = "${state.battery}%",
                sub = when {
                    state.charging && id -> "Mengisi"
                    state.charging -> "Charging"
                    id -> "Baterai"
                    else -> "Battery"
                },
                accent = when {
                    state.battery <= 15 && !state.charging -> Hud.Danger
                    state.charging -> Hud.Amber
                    else -> Hud.Cyan
                },
                modifier = Modifier.weight(1f),
            )
            StatusTile(
                icon = "🛰",
                title = "NETWORK",
                value = state.network,
                sub = if (state.network == "OFFLINE") {
                    if (id) "Terputus" else "Disconnected"
                } else {
                    if (id) "Terhubung" else "Connected"
                },
                accent = if (state.network == "OFFLINE") Hud.Danger else Hud.Cyan,
                modifier = Modifier.weight(1f),
            )
            StatusTile(
                icon = state.weatherIcon,
                title = "WEATHER",
                value = state.temperature?.let { "$it°" } ?: "…",
                sub = state.weatherText,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        StatusTile(
            icon = "📍",
            title = if (id) "LOKASI · GPS" else "LOCATION · GPS",
            value = state.locationPrimary,
            sub = buildString {
                append(state.locationSecondary)
                state.locationAccuracy?.let {
                    if (isNotEmpty()) append(" · ")
                    append(if (id) "akurasi ±$it m" else "±$it m")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))
        HudDivider()

        // ---- Arc reactor + transkrip -----------------------------------------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ArcReactor(
                    active = state.listening,
                    thinking = state.thinking,
                    modifier = Modifier.tapOnly { onMicClick() },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when {
                        state.listening && id -> "MENDENGARKAN…"
                        state.listening -> "LISTENING…"
                        state.thinking && id -> "MEMPROSES…"
                        state.thinking -> "PROCESSING…"
                        id -> "KETUK UNTUK BICARA"
                        else -> "TAP TO SPEAK"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.listening) Hud.Amber else Hud.TextMuted,
                )

                AnimatedVisibility(visible = state.transcript.isNotBlank()) {
                    Text(
                        text = "“${state.transcript}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hud.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
                    )
                }

                AnimatedVisibility(visible = state.reply.isNotBlank()) {
                    HudPanel(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = state.reply,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Hud.TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }

        // ---- Petunjuk laci aplikasi -------------------------------------------
        Box(
            modifier = Modifier
                .padding(bottom = 26.dp)
                .size(width = 140.dp, height = 40.dp)
                .tapOnly { onOpenDrawer() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(48.dp)
                        .height(2.dp)
                        .background(Hud.Cyan.copy(alpha = 0.6f), CircleShape),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (id) "GESER ATAS · APLIKASI" else "SWIPE UP · APPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Hud.TextMuted,
                )
            }
        }
    }
}

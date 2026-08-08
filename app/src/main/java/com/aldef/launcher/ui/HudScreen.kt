package com.aldef.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldef.launcher.HudState
import com.aldef.launcher.ui.components.ArcReactor
import com.aldef.launcher.ui.components.HudDivider
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

        // ---- Bilah merek --------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ALDEF LAUNCHER",
                style = MaterialTheme.typography.labelSmall,
                color = Hud.CyanDim,
            )
            Text(
                text = "⚙",
                fontSize = 18.sp,
                color = Hud.CyanDim,
                modifier = Modifier
                    .clickable { onOpenSettings() }
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(18.dp))
        HudDivider()
        Spacer(Modifier.height(22.dp))

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
            modifier = Modifier.clickable { onGreetAgain() },
        )

        Spacer(Modifier.height(22.dp))
        HudDivider()
        Spacer(Modifier.height(18.dp))

        // ---- Panel status ---------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusTile(
                icon = "⚡",
                title = "POWER",
                value = if (id) {
                    "Baterai ${state.battery}%" + if (state.charging) " ⚡" else ""
                } else {
                    "Battery ${state.battery}%" + if (state.charging) " ⚡" else ""
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
                accent = if (state.network == "OFFLINE") Hud.Danger else Hud.Cyan,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusTile(
                icon = "🧠",
                title = "AI STATUS",
                value = state.aiStatus,
                accent = if (state.aiStatus == "ONLINE") Hud.Cyan else Hud.Amber,
                modifier = Modifier.weight(1f),
            )
            StatusTile(
                icon = state.weatherIcon,
                title = "WEATHER",
                value = state.temperature?.let { "$it°  ${state.weatherText}" } ?: "…",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))

        StatusTile(
            icon = "📍",
            title = "LOCATION",
            value = state.city,
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
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .background(Hud.Panel, RoundedCornerShape(6.dp))
                            .border(1.dp, Hud.PanelBorder, RoundedCornerShape(6.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = state.reply,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Hud.TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // ---- Petunjuk laci aplikasi -------------------------------------------
        Box(
            modifier = Modifier
                .padding(bottom = 26.dp)
                .size(width = 120.dp, height = 40.dp)
                .clickable { onOpenDrawer() },
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

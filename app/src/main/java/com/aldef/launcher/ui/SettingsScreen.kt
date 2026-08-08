package com.aldef.launcher.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldef.launcher.HudState
import com.aldef.launcher.ai.ClaudeClient
import com.aldef.launcher.ui.components.HudDivider
import com.aldef.launcher.ui.theme.Hud

@Composable
fun SettingsScreen(
    state: HudState,
    currentApiKey: String,
    onUserName: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onSpeakOnHome: (Boolean) -> Unit,
    onApiKey: (String) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onReloadApps: () -> Unit,
    onClose: () -> Unit,
) {
    val id = state.language == "id"
    var name by remember { mutableStateOf(state.userName) }
    var apiKey by remember { mutableStateOf(currentApiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Hud.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(44.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (id) "PENGATURAN ALDEF" else "ALDEF SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                color = Hud.CyanDim,
            )
            Text(
                text = "✕",
                fontSize = 18.sp,
                color = Hud.CyanDim,
                modifier = Modifier
                    .clickable {
                        onUserName(name)
                        onApiKey(apiKey)
                        onClose()
                    }
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        HudDivider()
        Spacer(Modifier.height(20.dp))

        SectionLabel(if (id) "NAMA PANGGILAN" else "YOUR NAME")
        HudTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "DENI",
        )
        Note(
            if (id) {
                "Dipakai di layar utama dan saat Aldef menyapa Anda."
            } else {
                "Shown on the HUD and used when Aldef greets you."
            },
        )

        Spacer(Modifier.height(22.dp))

        SectionLabel(if (id) "BAHASA" else "LANGUAGE")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceChip("Bahasa Indonesia", selected = id) { onLanguage("id") }
            ChoiceChip("English", selected = !id) { onLanguage("en") }
        }

        Spacer(Modifier.height(22.dp))

        SectionLabel(if (id) "SAPAAN SUARA" else "VOICE GREETING")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (id) "Bicara saat membuka layar utama" else "Speak when Home opens",
                style = MaterialTheme.typography.bodyMedium,
                color = Hud.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.speakOnHome,
                onCheckedChange = onSpeakOnHome,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Hud.Cyan,
                    checkedTrackColor = Hud.PanelBorder,
                    uncheckedThumbColor = Hud.TextMuted,
                ),
            )
        }

        Spacer(Modifier.height(22.dp))

        SectionLabel("ANTHROPIC API KEY")
        HudTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            placeholder = "sk-ant-…",
            masked = true,
        )
        Note(
            if (id) {
                "Tanpa key, Aldef tetap jalan untuk perintah perangkat (buka aplikasi, " +
                    "baterai, cuaca, senter). Dengan key, pertanyaan bebas dijawab model " +
                    "${ClaudeClient.MODEL}. Key disimpan di ponsel ini saja — jangan pakai " +
                    "key produksi kalau ponsel dipakai bersama."
            } else {
                "Without a key, Aldef still handles device commands (open apps, battery, " +
                    "weather, torch). With a key, free-form questions go to ${ClaudeClient.MODEL}. " +
                    "The key is stored on this phone only — don't use a production key on a shared device."
            },
        )

        Spacer(Modifier.height(22.dp))
        HudDivider()
        Spacer(Modifier.height(20.dp))

        ActionRow(
            label = if (id) "Jadikan launcher utama" else "Set as default launcher",
            onClick = onSetDefaultLauncher,
        )
        Spacer(Modifier.height(10.dp))
        ActionRow(
            label = if (id) "Muat ulang daftar aplikasi" else "Reload app list",
            onClick = onReloadApps,
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = if (id) {
                "Perintah suara: \"buka whatsapp\", \"baterai\", \"cuaca\", \"senter\", " +
                    "\"jam berapa\", \"cari resep rendang\", \"telepon 08123…\". " +
                    "Selebihnya diteruskan ke AI."
            } else {
                "Voice commands: \"open whatsapp\", \"battery\", \"weather\", \"flashlight\", " +
                    "\"what time\", \"search for X\", \"call 555…\". Anything else goes to the AI."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Hud.TextMuted,
        )

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Hud.Cyan,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Hud.TextMuted,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun HudTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    masked: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Hud.Panel, RoundedCornerShape(4.dp))
            .border(1.dp, Hud.PanelBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Hud.TextMuted)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (masked) {
                PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            },
            textStyle = TextStyle(color = Hud.TextPrimary, fontSize = 14.sp),
            cursorBrush = SolidColor(Hud.Cyan),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Hud.Cyan.copy(alpha = 0.18f) else Hud.Panel,
                RoundedCornerShape(4.dp),
            )
            .border(
                1.dp,
                if (selected) Hud.Cyan else Hud.PanelBorder,
                RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Hud.Cyan else Hud.TextMuted,
        )
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Hud.Panel, RoundedCornerShape(4.dp))
            .border(1.dp, Hud.PanelBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(
            text = "▸  $label",
            style = MaterialTheme.typography.bodyMedium,
            color = Hud.Cyan,
        )
    }
}

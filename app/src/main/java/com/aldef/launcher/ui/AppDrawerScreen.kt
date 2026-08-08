package com.aldef.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldef.launcher.core.AppEntry
import com.aldef.launcher.ui.components.HudDivider
import com.aldef.launcher.ui.theme.Hud
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    apps: List<AppEntry>,
    loading: Boolean,
    isIndonesian: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUninstall: (AppEntry) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(apps, query) {
        if (query.isBlank()) {
            apps
        } else {
            val q = query.lowercase(Locale.getDefault())
            apps.filter {
                it.label.lowercase(Locale.getDefault()).contains(q) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Hud.Background)
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(44.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isIndonesian) "APLIKASI · ${apps.size}" else "APPLICATIONS · ${apps.size}",
                style = MaterialTheme.typography.labelSmall,
                color = Hud.CyanDim,
            )
            Text(
                text = "✕",
                fontSize = 18.sp,
                color = Hud.CyanDim,
                modifier = Modifier
                    .combinedClickable(onClick = onClose)
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Kolom pencarian
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Hud.Panel, RoundedCornerShape(4.dp))
                .border(1.dp, Hud.PanelBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (query.isEmpty()) {
                Text(
                    text = if (isIndonesian) "Cari aplikasi…" else "Search apps…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Hud.TextMuted,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = Hud.TextPrimary,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(Hud.Cyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))
        HudDivider()
        Spacer(Modifier.height(12.dp))

        when {
            loading -> CenteredNote(if (isIndonesian) "MEMUAT APLIKASI…" else "LOADING APPS…")
            filtered.isEmpty() -> CenteredNote(if (isIndonesian) "TIDAK ADA HASIL" else "NO RESULTS")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppTile(
                        app = app,
                        isIndonesian = isIndonesian,
                        onLaunch = { onLaunch(app) },
                        onAppInfo = { onAppInfo(app) },
                        onUninstall = { onUninstall(app) },
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: AppEntry,
    isIndonesian: Boolean,
    onLaunch: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = { menuOpen = true },
            )
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            val icon = app.icon
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Hud.Panel, RoundedCornerShape(12.dp))
                        .border(1.dp, Hud.PanelBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = app.label.take(1).uppercase(Locale.getDefault()),
                        color = Hud.Cyan,
                        fontSize = 18.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = Hud.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (isIndonesian) "Info aplikasi" else "App info") },
                onClick = { menuOpen = false; onAppInfo() },
            )
            DropdownMenuItem(
                text = { Text(if (isIndonesian) "Copot pemasangan" else "Uninstall") },
                onClick = { menuOpen = false; onUninstall() },
            )
        }
    }
}

@Composable
private fun CenteredNote(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Hud.TextMuted)
    }
}

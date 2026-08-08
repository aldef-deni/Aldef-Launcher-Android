package com.aldef.launcher.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap?,
)

/** Daftar aplikasi terpasang yang punya activity peluncur. */
class AppRepository(private val context: Context) {

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(intent, 0)

        resolved.asSequence()
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }.getOrNull() ?: pkg
                val icon = runCatching {
                    info.loadIcon(pm).toBitmap(width = 144, height = 144).asImageBitmap()
                }.getOrNull()
                AppEntry(label, pkg, icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }

    fun launch(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun openAppInfo(packageName: String) {
        runCatching {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun uninstall(packageName: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DELETE)
                    .setData(android.net.Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /**
     * Pencocokan longgar untuk perintah suara: "buka wa" harus menemukan
     * "WhatsApp". Kembalikan kandidat terbaik atau null.
     */
    fun findByFuzzyName(apps: List<AppEntry>, spoken: String): AppEntry? {
        val query = spoken.lowercase(Locale.getDefault()).trim()
        if (query.isEmpty()) return null

        apps.firstOrNull { it.label.equals(query, ignoreCase = true) }?.let { return it }
        apps.firstOrNull { it.label.lowercase(Locale.getDefault()).startsWith(query) }?.let { return it }
        apps.firstOrNull { it.label.lowercase(Locale.getDefault()).contains(query) }?.let { return it }
        apps.firstOrNull { it.packageName.lowercase(Locale.getDefault()).contains(query) }?.let { return it }

        // Cocokkan per kata: "google chrome" -> "chrome"
        val words = query.split(" ").filter { it.length >= 3 }
        return apps.firstOrNull { app ->
            val label = app.label.lowercase(Locale.getDefault())
            words.any { label.contains(it) }
        }
    }

    fun packageExists(packageName: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)

    @Suppress("unused")
    private fun unusedPm(): PackageManager = context.packageManager
}

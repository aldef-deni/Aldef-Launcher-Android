package com.aldef.launcher.core

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import kotlin.system.exitProcess

/** Apa yang benar-benar terjadi saat kita minta perangkat dinyalakan ulang. */
enum class RestartResult {
    /** Perangkat benar-benar reboot (hanya app sistem / perangkat root). */
    DEVICE_REBOOT,

    /** Proses aplikasi dimatikan lalu dijalankan ulang dengan state bersih. */
    PROCESS_RESTART,
}

/**
 * Menyalakan ulang sistem setelah antarmuka HUD diaktifkan.
 *
 * Catatan penting: `PowerManager.reboot()` memerlukan izin `android.permission.REBOOT`
 * yang bertanda tangan sistem. Aplikasi pihak ketiga biasa TIDAK akan pernah
 * mendapatkannya, jadi panggilan itu pasti melempar SecurityException di HP normal.
 * Karena itu kita selalu menyediakan jalur cadangan berupa restart proses penuh —
 * yang sebenarnya sudah cukup untuk memastikan HUD dimulai dari state bersih.
 */
object SystemRestarter {

    fun restart(context: Context, launchOnRestart: Class<*>): RestartResult {
        if (tryDeviceReboot(context)) return RestartResult.DEVICE_REBOOT

        restartProcess(context, launchOnRestart)
        return RestartResult.PROCESS_RESTART
    }

    /** Hanya berhasil pada ROM kustom / app sistem yang memegang izin REBOOT. */
    private fun tryDeviceReboot(context: Context): Boolean = runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        pm.reboot(null)
        true
    }.getOrDefault(false)

    /**
     * Menjadwalkan activity tujuan lalu mematikan proses. Android akan
     * menjalankan ulang aplikasi dari nol — semua service, ViewModel, TTS, dan
     * pengenal suara dibangun ulang, sehingga tidak ada state basi yang tertinggal.
     */
    private fun restartProcess(context: Context, target: Class<*>) {
        val intent = Intent(context, target).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
            )
            putExtra(EXTRA_AFTER_RESTART, true)
        }
        context.startActivity(intent)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(0)
        }, 400)
    }

    const val EXTRA_AFTER_RESTART = "aldef.after_restart"
}

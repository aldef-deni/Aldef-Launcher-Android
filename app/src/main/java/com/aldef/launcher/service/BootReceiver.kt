package com.aldef.launcher.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aldef.launcher.core.Prefs

/** Menghidupkan kembali service layar kunci setelah perangkat dinyalakan ulang. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs(context).lockScreenEnabled) LockScreenService.start(context)
    }
}

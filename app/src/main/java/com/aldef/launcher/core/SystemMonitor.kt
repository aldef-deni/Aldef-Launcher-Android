package com.aldef.launcher.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class BatteryInfo(val percent: Int, val charging: Boolean)

/** Membaca status baterai dan jaringan dari sistem. */
class SystemMonitor(private val context: Context) {

    fun readBattery(): BatteryInfo {
        val intent = ContextCompat.registerReceiver(
            context,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        ) ?: return BatteryInfo(0, false)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryInfo(percent, charging)
    }

    /** Label jaringan singkat untuk HUD: WI-FI, 5G, LTE, 3G, OFFLINE, ... */
    fun readNetworkLabel(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "UNKNOWN"
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "OFFLINE"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "LAN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellularLabel()
            else -> "OFFLINE"
        }
    }

    /**
     * Butuh izin READ_PHONE_STATE pada Android 10+. Jika ditolak, kembalikan
     * label generik daripada membuat aplikasi crash.
     */
    private fun cellularLabel(): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return "MOBILE"
        val type = try {
            @Suppress("DEPRECATION")
            tm.networkType
        } catch (_: SecurityException) {
            return "MOBILE"
        }
        return when (type) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            else -> "MOBILE"
        }
    }

    /** Mendaftarkan pemantau perubahan baterai; kembalikan fungsi untuk melepasnya. */
    fun observeBattery(onChange: (BatteryInfo) -> Unit): () -> Unit {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = onChange(readBattery())
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        return { runCatching { context.unregisterReceiver(receiver) } }
    }
}

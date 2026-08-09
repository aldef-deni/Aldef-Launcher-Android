package com.aldef.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aldef.launcher.LockScreenActivity
import com.aldef.launcher.R
import com.aldef.launcher.SetupActivity
import com.aldef.launcher.core.Prefs

/**
 * Menjaga layar kunci Aldef tetap siap.
 *
 * ACTION_SCREEN_OFF dan ACTION_SCREEN_ON hanya bisa didengar lewat receiver yang
 * didaftarkan saat berjalan — tidak bisa lewat manifest. Karena itu diperlukan
 * service latar depan; sekaligus supaya proses tidak dimatikan sistem saat
 * layar mati.
 *
 * Activity sengaja dijalankan pada SCREEN_OFF, bukan SCREEN_ON: saat itu proses
 * masih dianggap aktif, sehingga pembatasan "background activity start" pada
 * Android 10+ tidak menghalangi. Ketika layar menyala, activity-nya sudah siap
 * di belakang keyguard.
 */
class LockScreenService : Service() {

    private lateinit var prefs: Prefs

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!prefs.lockScreenEnabled) return
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> showLockScreen()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        startForeground(NOTIFICATION_ID, buildNotification())

        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showLockScreen() {
        runCatching {
            startActivity(
                Intent(this, LockScreenActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION,
                ),
            )
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Aldef Lock Screen",
                    NotificationManager.IMPORTANCE_MIN,
                ).apply { setShowBadge(false) },
            )
        }

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SetupActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aldef")
            .setContentText(
                if (prefs.isIndonesian) "Layar kunci Aldef aktif" else "Aldef lock screen active",
            )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(open)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "aldef_lock"
        private const val NOTIFICATION_ID = 4201

        fun start(context: Context) {
            val intent = Intent(context, LockScreenService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockScreenService::class.java))
        }
    }
}

package com.aldef.launcher

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.aldef.launcher.ui.LockScreen
import com.aldef.launcher.ui.theme.AldefTheme

/**
 * Layar kunci Aldef.
 *
 * Batasan penting: aplikasi pihak ketiga TIDAK bisa mengganti keyguard Android.
 * Activity ini hanya tampil *di atas* layar kunci sistem lewat `showWhenLocked`.
 * Supaya terasa seperti layar kunci sungguhan, kunci layar sistem sebaiknya
 * diatur ke "Geser"/"Tidak ada". Dengan sendirinya ini bukan pengaman — jangan
 * diandalkan untuk melindungi data.
 */
class LockScreenActivity : ComponentActivity() {

    private val vm: LockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showOverKeyguard()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AldefTheme {
                val state by vm.state.collectAsState()

                // Tombol Back tidak boleh membuka kunci — itu pintu belakang
                // yang paling sering dipakai orang untuk melewati layar kunci.
                BackHandler(enabled = true) { /* sengaja diabaikan */ }

                LockScreen(state = state, onUnlock = { unlock() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    private fun showOverKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Minta sistem menyingkirkan keyguard bila memang tidak dikunci dengan
     * PIN/pola; kalau dikunci, sistem sendiri yang menampilkan tantangannya.
     */
    private fun unlock() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && km?.isKeyguardLocked == true) {
            km.requestDismissKeyguard(this, null)
        }
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }
}

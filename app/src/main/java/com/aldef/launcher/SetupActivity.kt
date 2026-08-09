package com.aldef.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.aldef.launcher.core.Prefs
import com.aldef.launcher.core.RestartResult
import com.aldef.launcher.core.SystemRestarter
import com.aldef.launcher.ui.SetupScreen
import com.aldef.launcher.ui.theme.AldefTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Gerbang aktivasi. Inilah yang terbuka ketika ikon Aldef diketuk dari
 * launcher lain — MainActivity sengaja tidak lagi muncul di laci aplikasi
 * supaya HUD hanya berjalan sebagai layar utama.
 */
class SetupActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private val hudEnabled = MutableStateFlow(false)
    private val defaultLauncher = MutableStateFlow(false)
    private val booting = MutableStateFlow(false)
    private val userName = MutableStateFlow("")
    private val speakOnHome = MutableStateFlow(true)
    private val language = MutableStateFlow("id")
    private val askName = MutableStateFlow(false)

    /**
     * Urutan boot baru dijalankan setelah dialog izin selesai, supaya animasi
     * aktivasi tidak tertutup dialog sistem dan proses tidak dimatikan saat
     * pengguna masih menjawab permintaan izin.
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { booting.value = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        hudEnabled.value = prefs.hudEnabled
        userName.value = prefs.userName
        speakOnHome.value = prefs.speakOnHome
        language.value = prefs.language
        askName.value = !prefs.nameAsked

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AldefTheme {
                val enabled by hudEnabled.collectAsState()
                val isDefault by defaultLauncher.collectAsState()
                val isBooting by booting.collectAsState()
                val name by userName.collectAsState()
                val speak by speakOnHome.collectAsState()
                val lang by language.collectAsState()
                val needsName by askName.collectAsState()

                SetupScreen(
                    enabled = enabled,
                    isDefaultLauncher = isDefault,
                    isIndonesian = lang == "id",
                    booting = isBooting,
                    userName = name,
                    speakOnHome = speak,
                    askName = needsName,
                    onActivate = { activate() },
                    onDeactivate = {
                        prefs.hudEnabled = false
                        hudEnabled.value = false
                    },
                    onChooseLauncher = { openHomeSettings() },
                    onOpenHud = { openHud() },
                    onBootFinished = { restartSystem() },
                    onUserName = { renameUser(it) },
                    onNameConfirmed = { confirmName(it) },
                    onLanguage = {
                        prefs.language = it
                        language.value = it
                    },
                    onSpeakOnHome = {
                        prefs.speakOnHome = it
                        speakOnHome.value = it
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hudEnabled.value = prefs.hudEnabled
        defaultLauncher.value = isDefaultLauncher()
    }

    /**
     * Perubahan nama dari kolom konfigurasi. Sengaja TIDAK menyentuh
     * `nameAsked`: BasicTextField memanggil onValueChange sejak komposisi awal,
     * dan kalau jalur ini ikut menandai nama sudah ditanyakan, modal perkenalan
     * akan tertutup sebelum sempat terlihat.
     */
    private fun renameUser(value: String) {
        prefs.userName = value
        userName.value = prefs.userName
    }

    /** Hanya dari modal perkenalan — di sinilah modal ditandai selesai. */
    private fun confirmName(value: String) {
        if (value.isBlank()) return
        prefs.userName = value
        prefs.nameAsked = true
        userName.value = prefs.userName
        askName.value = false
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == packageName
    }

    /** Nyalakan HUD: simpan preferensi, minta izin, lalu jalankan urutan boot. */
    private fun activate() {
        prefs.hudEnabled = true
        hudEnabled.value = true

        val wanted = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
        )
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            booting.value = true
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun openHud() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun openHomeSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.ACTION_HOME_SETTINGS)
        } else {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(intent) }.isFailure) {
            runCatching {
                startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    /**
     * Reboot perangkat hanya mungkin untuk aplikasi sistem; pada HP biasa
     * SystemRestarter akan jatuh ke restart proses penuh, yang tetap memberi
     * HUD keadaan awal yang bersih.
     */
    private fun restartSystem() {
        when (SystemRestarter.restart(this, MainActivity::class.java)) {
            RestartResult.DEVICE_REBOOT -> Unit
            RestartResult.PROCESS_RESTART -> Toast.makeText(
                this,
                if (prefs.isIndonesian) {
                    "Aldef dimulai ulang. Tekan tombol Home lalu pilih Aldef untuk memasangnya permanen."
                } else {
                    "Aldef restarted. Press Home and pick Aldef to make it permanent."
                },
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}

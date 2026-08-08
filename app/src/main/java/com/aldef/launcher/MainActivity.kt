package com.aldef.launcher

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.aldef.launcher.ui.AppDrawerScreen
import com.aldef.launcher.ui.HudScreen
import com.aldef.launcher.ui.SettingsScreen
import com.aldef.launcher.ui.theme.AldefTheme

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            vm.refreshWeather()
        }
        vm.refreshSystem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        requestPermissionsIfNeeded()

        setContent {
            AldefTheme {
                val state by vm.state.collectAsState()

                // Tombol Back tidak boleh keluar dari launcher — cukup kembali ke HUD.
                BackHandler(enabled = true) {
                    if (state.screen != Screen.HUD) vm.show(Screen.HUD)
                }

                Box(Modifier.fillMaxSize()) {
                    when (state.screen) {
                        Screen.HUD -> HudScreen(
                            state = state,
                            onMicClick = { vm.toggleListening() },
                            onOpenDrawer = { vm.show(Screen.DRAWER) },
                            onOpenSettings = { vm.show(Screen.SETTINGS) },
                            onGreetAgain = { vm.speakGreeting() },
                        )

                        Screen.DRAWER -> AppDrawerScreen(
                            apps = state.apps,
                            loading = state.appsLoading,
                            isIndonesian = state.language == "id",
                            onLaunch = { vm.launchApp(it) },
                            onAppInfo = { vm.openAppInfo(it) },
                            onUninstall = { vm.uninstallApp(it) },
                            onClose = { vm.show(Screen.HUD) },
                        )

                        Screen.SETTINGS -> SettingsScreen(
                            state = state,
                            currentApiKey = vm.prefs.apiKey,
                            onUserName = { vm.updateUserName(it) },
                            onLanguage = { vm.updateLanguage(it) },
                            onSpeakOnHome = { vm.updateSpeakOnHome(it) },
                            onApiKey = { vm.updateApiKey(it) },
                            onSetDefaultLauncher = { openDefaultLauncherSettings() },
                            onReloadApps = { vm.loadApps() },
                            onClose = { vm.show(Screen.HUD) },
                        )
                    }
                }
            }
        }
    }

    /**
     * Ditekannya tombol Home saat launcher ini sudah aktif akan mengirim intent
     * baru ke activity yang sama (launchMode singleTask).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.onHomePressed()
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onPause() {
        super.onPause()
        vm.speaker.stop()
        vm.voiceInput.stop()
    }

    private fun requestPermissionsIfNeeded() {
        val wanted = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wanted += Manifest.permission.READ_PHONE_STATE
        }
        permissionLauncher.launch(wanted.toTypedArray())
    }

    /** Buka layar sistem untuk memilih launcher bawaan. */
    private fun openDefaultLauncherSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.ACTION_HOME_SETTINGS)
        } else {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(intent) }.isFailure) {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}

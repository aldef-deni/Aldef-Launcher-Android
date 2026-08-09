package com.aldef.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aldef.launcher.core.IndonesianTime
import com.aldef.launcher.core.Prefs
import com.aldef.launcher.core.ResolvedZone
import com.aldef.launcher.core.SystemMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LockState(
    val time: String = "--:--",
    val seconds: Int = 0,
    val dayName: String = "",
    val dateLine: String = "",
    val zoneLabel: String = "",
    val greeting: String = "",
    val userName: String = "",
    val battery: Int = 0,
    val charging: Boolean = false,
    val network: String = "…",
    val temperature: Int? = null,
    val weatherText: String = "",
    val weatherIcon: String = "🌤",
    val place: String = "",
    val isIndonesian: Boolean = true,
)

/**
 * State layar kunci. Sengaja jauh lebih ringan daripada LauncherViewModel:
 * tanpa TTS, tanpa pengenal suara, tanpa GPS, dan tanpa panggilan jaringan —
 * cuaca dan lokasi dibaca dari cache yang ditulis HUD. Layar kunci harus siap
 * dalam sekejap saat layar menyala dan tidak boleh menguras baterai.
 */
class LockViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val systemMonitor = SystemMonitor(app)
    // Koordinat terakhir dari HUD dipakai agar zona waktu layar kunci sama
    // persis dengan layar depan, tanpa perlu menyalakan GPS di sini.
    private var zone: ResolvedZone =
        IndonesianTime.resolve(prefs.cachedLatitude, prefs.cachedLongitude)

    private val _state = MutableStateFlow(LockState())
    val state: StateFlow<LockState> = _state.asStateFlow()

    private var unregisterBattery: (() -> Unit)? = null

    init {
        _state.update {
            it.copy(
                userName = prefs.userName,
                isIndonesian = prefs.isIndonesian,
                temperature = prefs.cachedTemperature,
                weatherText = prefs.cachedWeatherText,
                weatherIcon = prefs.cachedWeatherIcon,
                place = prefs.cachedPlace,
            )
        }

        unregisterBattery = systemMonitor.observeBattery { info ->
            _state.update { it.copy(battery = info.percent, charging = info.charging) }
        }
        refresh()
        startClock()
    }

    fun refresh() {
        val battery = systemMonitor.readBattery()
        _state.update {
            it.copy(
                battery = battery.percent,
                charging = battery.charging,
                network = systemMonitor.readNetworkLabel(),
            )
        }
    }

    private fun startClock() = viewModelScope.launch {
        while (true) {
            val now = Date()
            val locale = if (prefs.isIndonesian) Locale("in", "ID") else Locale.US

            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = zone.timeZone }
            val secFmt = SimpleDateFormat("ss", Locale.getDefault()).apply { timeZone = zone.timeZone }
            val dayFmt = SimpleDateFormat("EEEE", locale).apply { timeZone = zone.timeZone }
            val dateFmt = SimpleDateFormat("d MMMM yyyy", locale).apply { timeZone = zone.timeZone }

            _state.update {
                it.copy(
                    time = timeFmt.format(now),
                    seconds = secFmt.format(now).toIntOrNull() ?: 0,
                    dayName = dayFmt.format(now).uppercase(locale),
                    dateLine = dateFmt.format(now).uppercase(locale),
                    zoneLabel = zone.label,
                    greeting = IndonesianTime.greeting(zone, prefs.isIndonesian).uppercase(locale),
                )
            }
            delay(1_000)
        }
    }

    override fun onCleared() {
        super.onCleared()
        unregisterBattery?.invoke()
    }
}

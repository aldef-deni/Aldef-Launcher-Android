package com.aldef.launcher

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aldef.launcher.ai.Brain
import com.aldef.launcher.ai.BrainContext
import com.aldef.launcher.ai.BrainReply
import com.aldef.launcher.ai.ClaudeClient
import com.aldef.launcher.core.AppEntry
import com.aldef.launcher.core.AppRepository
import com.aldef.launcher.core.LocationRepository
import com.aldef.launcher.core.Place
import com.aldef.launcher.core.Prefs
import com.aldef.launcher.core.SystemMonitor
import com.aldef.launcher.core.WeatherRepository
import com.aldef.launcher.voice.Speaker
import com.aldef.launcher.voice.VoiceInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Screen { HUD, DRAWER, SETTINGS }

data class HudState(
    val time: String = "--:--",
    val date: String = "",
    val greeting: String = "",
    val userName: String = "DENI",
    val battery: Int = 0,
    val charging: Boolean = false,
    val network: String = "…",
    val temperature: Int? = null,
    val weatherIcon: String = "🌤",
    val weatherText: String = "…",
    val locationPrimary: String = "…",
    val locationSecondary: String = "",
    val locationAccuracy: Int? = null,
    val listening: Boolean = false,
    val thinking: Boolean = false,
    val transcript: String = "",
    val reply: String = "",
    val apps: List<AppEntry> = emptyList(),
    val appsLoading: Boolean = true,
    val screen: Screen = Screen.HUD,
    val language: String = "id",
    val speakOnHome: Boolean = true,
    val hasApiKey: Boolean = false,
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    private val systemMonitor = SystemMonitor(app)
    private val appRepository = AppRepository(app)
    private val locationRepository = LocationRepository(app)
    private val weatherRepository = WeatherRepository()
    private val brain = Brain(app, prefs, appRepository)

    val speaker = Speaker(app)
    val voiceInput = VoiceInput(app)

    private val _state = MutableStateFlow(HudState())
    val state: StateFlow<HudState> = _state.asStateFlow()

    private var unregisterBattery: (() -> Unit)? = null
    private var stopLocationUpdates: (() -> Unit)? = null
    private var greetedThisSession = false

    /** Lokasi yang dipakai untuk pengambilan cuaca terakhir. */
    private var weatherAnchor: Place? = null

    init {
        _state.update {
            it.copy(
                userName = prefs.userName,
                language = prefs.language,
                speakOnHome = prefs.speakOnHome,
                hasApiKey = prefs.apiKey.isNotBlank(),
            )
        }
        speaker.setLanguage(prefs.language)

        startClock()
        unregisterBattery = systemMonitor.observeBattery { info ->
            _state.update { it.copy(battery = info.percent, charging = info.charging) }
        }
        refreshSystem()
        loadApps()
        startLocationTracking()
    }

    // ------------------------------------------------------------------ UI

    fun show(screen: Screen) = _state.update { it.copy(screen = screen) }

    fun onHomePressed() {
        if (_state.value.screen != Screen.HUD) show(Screen.HUD)
    }

    fun onResume() {
        refreshSystem()
        if (!greetedThisSession && prefs.speakOnHome) {
            greetedThisSession = true
            viewModelScope.launch {
                delay(1_800) // beri waktu TTS siap dan cuaca termuat
                speakGreeting()
            }
        }
    }

    fun onDestroyView() {
        unregisterBattery?.invoke()
        stopLocationUpdates?.invoke()
        voiceInput.stop()
        speaker.shutdown()
    }

    // -------------------------------------------------------------- Data

    private fun startClock() = viewModelScope.launch {
        while (true) {
            val now = Date()
            val locale = if (prefs.isIndonesian) Locale("in", "ID") else Locale.US
            _state.update {
                it.copy(
                    time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                    date = SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(now).uppercase(locale),
                    greeting = greetingText(),
                )
            }
            delay(1_000)
        }
    }

    fun refreshSystem() {
        val battery = systemMonitor.readBattery()
        _state.update {
            it.copy(
                battery = battery.percent,
                charging = battery.charging,
                network = systemMonitor.readNetworkLabel(),
            )
        }
    }

    fun loadApps() = viewModelScope.launch {
        _state.update { it.copy(appsLoading = true) }
        val apps = appRepository.loadApps()
        _state.update { it.copy(apps = apps, appsLoading = false) }
    }

    // ---------------------------------------------------------- Lokasi

    /**
     * GPS dibaca ulang otomatis setiap 5 menit. Dua jalur dipakai bersamaan:
     * langganan LocationManager (fix datang sendiri saat berpindah) dan
     * penarikan berkala sebagai cadangan ketika GPS diam di dalam ruangan.
     */
    private fun startLocationTracking() {
        refreshLocationNow()

        stopLocationUpdates = locationRepository.startUpdates(LOCATION_INTERVAL_MS) { location ->
            viewModelScope.launch { applyLocation(locationRepository.describe(location)) }
        }

        viewModelScope.launch {
            while (true) {
                delay(LOCATION_INTERVAL_MS)
                refreshLocationNow()
            }
        }
    }

    fun refreshLocationNow() = viewModelScope.launch {
        applyLocation(locationRepository.currentPlace())
    }

    private suspend fun applyLocation(place: Place) {
        _state.update {
            it.copy(
                locationPrimary = place.primary,
                locationSecondary = place.secondary,
                locationAccuracy = place.accuracyM,
            )
        }

        // Ambil cuaca ulang hanya bila pindah cukup jauh atau belum ada data.
        val anchor = weatherAnchor
        val movedFar = anchor == null || distanceMeters(anchor, place) > WEATHER_REFRESH_DISTANCE_M
        if (movedFar || _state.value.temperature == null) {
            weatherAnchor = place
            fetchWeather(place)
        }
    }

    private suspend fun fetchWeather(place: Place) {
        // Jaringan sering belum siap tepat saat launcher dibuka — coba beberapa kali.
        repeat(3) { attempt ->
            val weather = weatherRepository.fetch(place)
            if (weather != null) {
                _state.update {
                    it.copy(
                        temperature = weather.temperatureC,
                        weatherIcon = weather.icon,
                        weatherText = if (prefs.isIndonesian) weather.descriptionId else weather.descriptionEn,
                    )
                }
                return
            }
            if (attempt < 2) delay(4_000)
        }
    }

    private fun distanceMeters(a: Place, b: Place): Float {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0]
    }

    // ---------------------------------------------------------- Aplikasi

    fun launchApp(entry: AppEntry) {
        if (appRepository.launch(entry.packageName)) show(Screen.HUD)
    }

    fun openAppInfo(entry: AppEntry) = appRepository.openAppInfo(entry.packageName)

    fun uninstallApp(entry: AppEntry) = appRepository.uninstall(entry.packageName)

    // ------------------------------------------------------------- Voice

    fun toggleListening() {
        if (_state.value.listening) {
            voiceInput.stop()
            _state.update { it.copy(listening = false) }
            return
        }
        speaker.stop()
        _state.update { it.copy(listening = true, transcript = "", reply = "") }

        voiceInput.start(
            languageCode = prefs.language,
            onPartial = { partial -> _state.update { it.copy(transcript = partial) } },
            onResult = { text ->
                _state.update { it.copy(listening = false, transcript = text) }
                submitCommand(text)
            },
            onError = { code ->
                _state.update { it.copy(listening = false, reply = voiceErrorText(code)) }
            },
        )
    }

    fun submitCommand(command: String) = viewModelScope.launch {
        val ctx = brainContext()
        when (val reply = brain.handle(command, ctx)) {
            is BrainReply.Local -> respond(reply.text)
            is BrainReply.NeedsAi -> askClaude(reply.prompt, ctx)
        }
    }

    private suspend fun askClaude(prompt: String, ctx: BrainContext) {
        val key = prefs.apiKey
        if (key.isBlank()) {
            respond(brain.offlineFallback())
            return
        }
        _state.update { it.copy(thinking = true) }
        val answer = runCatching {
            ClaudeClient(key).ask(brain.systemPrompt(ctx), prompt)
        }.getOrElse { error ->
            if (prefs.isIndonesian) {
                "Koneksi ke AI gagal: ${error.message ?: "kesalahan tidak diketahui"}"
            } else {
                "AI request failed: ${error.message ?: "unknown error"}"
            }
        }
        _state.update { it.copy(thinking = false) }
        respond(answer.ifBlank { if (prefs.isIndonesian) "Tidak ada jawaban." else "No answer." })
    }

    private fun respond(text: String) {
        _state.update { it.copy(reply = text) }
        speaker.speak(text)
    }

    fun speakGreeting() {
        val s = _state.value
        val where = listOf(s.locationPrimary, s.locationSecondary)
            .filter { it.isNotBlank() && it != "…" }
            .firstOrNull()
            ?: ""

        val text = if (prefs.isIndonesian) {
            buildString {
                append("${greetingText(spoken = true)}, ${prefs.userName}. ")
                append("Sekarang pukul ${s.time}. ")
                append("Baterai ${s.battery} persen. ")
                s.temperature?.let {
                    append("Cuaca ${s.weatherText.lowercase(Locale("in", "ID"))}, $it derajat")
                    if (where.isNotBlank()) append(" di $where")
                    append(". ")
                }
                append("Semua sistem siap.")
            }
        } else {
            buildString {
                append("${greetingText(spoken = true)}, ${prefs.userName}. ")
                append("It's ${s.time}. ")
                append("Battery at ${s.battery} percent. ")
                s.temperature?.let {
                    append("${s.weatherText}, $it degrees")
                    if (where.isNotBlank()) append(" in $where")
                    append(". ")
                }
                append("All systems online.")
            }
        }
        _state.update { it.copy(reply = text) }
        speaker.speak(text)
    }

    private fun brainContext(): BrainContext {
        val s = _state.value
        return BrainContext(
            battery = s.battery,
            charging = s.charging,
            network = s.network,
            temperature = s.temperature,
            weatherText = s.weatherText,
            city = listOf(s.locationPrimary, s.locationSecondary)
                .filter { it.isNotBlank() && it != "…" }
                .joinToString(", "),
            apps = s.apps,
        )
    }

    private fun greetingText(spoken: Boolean = false): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (prefs.isIndonesian) {
            val g = when (hour) {
                in 4..10 -> "Selamat pagi"
                in 11..14 -> "Selamat siang"
                in 15..18 -> "Selamat sore"
                else -> "Selamat malam"
            }
            if (spoken) g else g.uppercase(Locale("in", "ID"))
        } else {
            val g = when (hour) {
                in 4..11 -> "Good morning"
                in 12..17 -> "Good afternoon"
                in 18..21 -> "Good evening"
                else -> "Good night"
            }
            if (spoken) g else g.uppercase(Locale.US)
        }
    }

    private fun voiceErrorText(code: String): String = if (prefs.isIndonesian) {
        when (code) {
            "NO_PERMISSION" -> "Izin mikrofon belum diberikan."
            "NO_MATCH", "EMPTY" -> "Maaf, saya tidak menangkap suaranya."
            "NETWORK", "NETWORK_TIMEOUT" -> "Pengenalan suara butuh koneksi internet."
            "SPEECH_UNAVAILABLE" -> "Pengenalan suara tidak tersedia di perangkat ini."
            "TIMEOUT" -> "Tidak ada suara terdeteksi."
            else -> "Pengenalan suara gagal ($code)."
        }
    } else {
        when (code) {
            "NO_PERMISSION" -> "Microphone permission is not granted."
            "NO_MATCH", "EMPTY" -> "Sorry, I didn't catch that."
            "NETWORK", "NETWORK_TIMEOUT" -> "Speech recognition needs an internet connection."
            "SPEECH_UNAVAILABLE" -> "Speech recognition isn't available on this device."
            "TIMEOUT" -> "No speech detected."
            else -> "Speech recognition failed ($code)."
        }
    }

    // ---------------------------------------------------------- Settings

    fun updateUserName(name: String) {
        prefs.userName = name
        _state.update { it.copy(userName = prefs.userName) }
    }

    fun updateLanguage(code: String) {
        prefs.language = code
        speaker.setLanguage(code)
        _state.update { it.copy(language = code) }
        refreshLocationNow()
    }

    fun updateSpeakOnHome(enabled: Boolean) {
        prefs.speakOnHome = enabled
        _state.update { it.copy(speakOnHome = enabled) }
    }

    fun updateApiKey(key: String) {
        prefs.apiKey = key
        _state.update { it.copy(hasApiKey = key.isNotBlank()) }
    }

    override fun onCleared() {
        super.onCleared()
        onDestroyView()
    }

    private companion object {
        const val LOCATION_INTERVAL_MS = 5 * 60 * 1000L
        const val WEATHER_REFRESH_DISTANCE_M = 3_000f
    }
}

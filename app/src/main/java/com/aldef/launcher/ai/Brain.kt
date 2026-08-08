package com.aldef.launcher.ai

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.aldef.launcher.core.AppEntry
import com.aldef.launcher.core.AppRepository
import com.aldef.launcher.core.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Konteks yang dipakai otak untuk menjawab pertanyaan status. */
data class BrainContext(
    val battery: Int,
    val charging: Boolean,
    val network: String,
    val temperature: Int?,
    val weatherText: String,
    val city: String,
    val apps: List<AppEntry>,
)

sealed interface BrainReply {
    /** Sudah selesai ditangani secara lokal. */
    data class Local(val text: String) : BrainReply

    /** Perlu diteruskan ke Claude. */
    data class NeedsAi(val prompt: String) : BrainReply
}

/**
 * Router perintah. Perintah perangkat ditangani lokal (instan, tanpa kuota),
 * sisanya dilempar ke Claude.
 */
class Brain(
    private val context: Context,
    private val prefs: Prefs,
    private val appRepository: AppRepository,
) {

    private var torchOn = false

    fun handle(rawCommand: String, ctx: BrainContext): BrainReply {
        val cmd = rawCommand.lowercase(Locale.getDefault()).trim()
        val id = prefs.isIndonesian

        if (cmd.isEmpty()) return BrainReply.Local(if (id) "Saya tidak menangkap apa pun." else "I didn't catch that.")

        // --- Buka aplikasi -------------------------------------------------
        openAppPrefix(cmd)?.let { target ->
            val app = appRepository.findByFuzzyName(ctx.apps, target)
            return if (app != null && appRepository.launch(app.packageName)) {
                BrainReply.Local(if (id) "Membuka ${app.label}." else "Opening ${app.label}.")
            } else {
                BrainReply.Local(
                    if (id) "Aplikasi \"$target\" tidak ditemukan." else "App \"$target\" not found.",
                )
            }
        }

        // --- Status perangkat ----------------------------------------------
        if (cmd.containsAny("baterai", "batre", "battery", "daya")) {
            val state = when {
                ctx.charging && id -> "sedang mengisi daya"
                ctx.charging -> "and charging"
                id -> "tidak mengisi daya"
                else -> "not charging"
            }
            return BrainReply.Local(
                if (id) "Baterai ${ctx.battery} persen, $state." else "Battery at ${ctx.battery} percent, $state.",
            )
        }

        if (cmd.containsAny("jam berapa", "waktu", "what time", "the time", "pukul berapa")) {
            val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            return BrainReply.Local(if (id) "Sekarang pukul $now." else "It's $now.")
        }

        if (cmd.containsAny("tanggal", "hari ini", "what date", "today")) {
            val pattern = if (id) "EEEE, d MMMM yyyy" else "EEEE, MMMM d yyyy"
            val locale = if (id) Locale("in", "ID") else Locale.US
            val today = SimpleDateFormat(pattern, locale).format(Date())
            return BrainReply.Local(if (id) "Hari ini $today." else "Today is $today.")
        }

        if (cmd.containsAny("cuaca", "weather", "suhu", "temperature")) {
            val temp = ctx.temperature
            return BrainReply.Local(
                when {
                    temp == null && id -> "Data cuaca belum tersedia."
                    temp == null -> "Weather data isn't available yet."
                    id -> "Cuaca di ${ctx.city} ${ctx.weatherText}, $temp derajat."
                    else -> "It's ${ctx.weatherText.lowercase(Locale.US)} in ${ctx.city}, $temp degrees."
                },
            )
        }

        if (cmd.containsAny("jaringan", "sinyal", "network", "signal", "koneksi")) {
            return BrainReply.Local(
                if (id) "Jaringan aktif: ${ctx.network}." else "Network: ${ctx.network}.",
            )
        }

        // --- Aksi perangkat --------------------------------------------------
        if (cmd.containsAny("senter", "flashlight", "torch", "lampu")) {
            val on = !torchOn
            return if (setTorch(on)) {
                BrainReply.Local(
                    when {
                        on && id -> "Senter dinyalakan."
                        on -> "Flashlight on."
                        id -> "Senter dimatikan."
                        else -> "Flashlight off."
                    },
                )
            } else {
                BrainReply.Local(if (id) "Senter tidak tersedia." else "Flashlight unavailable.")
            }
        }

        if (cmd.containsAny("kamera", "camera", "foto")) {
            return openIntent(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                if (id) "Membuka kamera." else "Opening camera.",
                if (id) "Kamera tidak tersedia." else "Camera unavailable.",
            )
        }

        if (cmd.containsAny("wifi", "wi-fi")) {
            return openIntent(
                Intent(Settings.ACTION_WIFI_SETTINGS),
                if (id) "Membuka pengaturan Wi-Fi." else "Opening Wi-Fi settings.",
                if (id) "Tidak bisa membuka pengaturan." else "Can't open settings.",
            )
        }

        if (cmd.containsAny("bluetooth")) {
            return openIntent(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                if (id) "Membuka pengaturan Bluetooth." else "Opening Bluetooth settings.",
                if (id) "Tidak bisa membuka pengaturan." else "Can't open settings.",
            )
        }

        if (cmd.containsAny("pengaturan", "setelan", "settings")) {
            return openIntent(
                Intent(Settings.ACTION_SETTINGS),
                if (id) "Membuka pengaturan." else "Opening settings.",
                if (id) "Tidak bisa membuka pengaturan." else "Can't open settings.",
            )
        }

        if (cmd.containsAny("alarm")) {
            return openIntent(
                Intent(AlarmClock.ACTION_SHOW_ALARMS),
                if (id) "Membuka alarm." else "Opening alarms.",
                if (id) "Aplikasi alarm tidak ditemukan." else "No alarm app found.",
            )
        }

        callTarget(cmd)?.let { number ->
            return openIntent(
                Intent(Intent.ACTION_DIAL).setData(Uri.parse("tel:$number")),
                if (id) "Menyiapkan panggilan." else "Opening the dialer.",
                if (id) "Tidak bisa membuka telepon." else "Can't open the dialer.",
            )
        }

        searchTarget(cmd)?.let { query ->
            return openIntent(
                Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))),
                if (id) "Mencari $query." else "Searching for $query.",
                if (id) "Tidak ada peramban." else "No browser available.",
            )
        }

        // --- Sisanya: serahkan ke Claude -------------------------------------
        return BrainReply.NeedsAi(rawCommand.trim())
    }

    fun systemPrompt(ctx: BrainContext): String {
        val id = prefs.isIndonesian
        val language = if (id) "Bahasa Indonesia" else "English"
        return buildString {
            append("Kamu adalah ALDEF, asisten AI yang tertanam di launcher ponsel Android milik ")
            append(prefs.userName)
            append(". Gaya bicaramu tenang, presisi, dan sopan seperti J.A.R.V.I.S. ")
            append("Jawab selalu dalam $language. ")
            append("Jawabanmu dibacakan lewat text-to-speech, jadi tulis 1-3 kalimat pendek ")
            append("tanpa markdown, tanpa daftar bernomor, tanpa emoji, dan tanpa simbol yang aneh dibaca. ")
            append("Kalau kamu tidak tahu jawabannya, katakan terus terang.\n\n")
            append("Status perangkat saat ini:\n")
            append("- Baterai: ${ctx.battery}% (${if (ctx.charging) "mengisi" else "tidak mengisi"})\n")
            append("- Jaringan: ${ctx.network}\n")
            append("- Lokasi: ${ctx.city}\n")
            ctx.temperature?.let { append("- Cuaca: ${ctx.weatherText}, $it derajat Celsius\n") }
            append("- Waktu: ${SimpleDateFormat("HH:mm, EEEE d MMMM yyyy", Locale.getDefault()).format(Date())}\n")
        }
    }

    fun offlineFallback(): String = if (prefs.isIndonesian) {
        "Perintah itu butuh koneksi AI. Buka Pengaturan Aldef dan isi API key Anthropic untuk mengaktifkannya."
    } else {
        "That needs the AI backend. Open Aldef settings and add your Anthropic API key to enable it."
    }

    // ---------------------------------------------------------------------

    private fun setTorch(on: Boolean): Boolean {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        return runCatching {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cm.setTorchMode(cameraId, on)
            torchOn = on
            true
        }.getOrDefault(false)
    }

    private fun openIntent(intent: Intent, success: String, failure: String): BrainReply.Local {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return BrainReply.Local(
            if (runCatching { context.startActivity(intent) }.isSuccess) success else failure,
        )
    }

    private fun openAppPrefix(cmd: String): String? {
        val prefixes = listOf("buka aplikasi ", "buka ", "jalankan ", "open ", "launch ", "start ")
        val prefix = prefixes.firstOrNull { cmd.startsWith(it) } ?: return null
        val rest = cmd.removePrefix(prefix).trim()
        // "buka wifi" bukan permintaan aplikasi — biarkan handler lain menangani.
        if (rest.isEmpty() || rest.containsAny("wifi", "wi-fi", "bluetooth", "pengaturan", "settings", "senter", "flashlight")) {
            return null
        }
        return rest
    }

    private fun callTarget(cmd: String): String? {
        val prefixes = listOf("telepon ", "hubungi ", "call ", "dial ")
        val prefix = prefixes.firstOrNull { cmd.startsWith(it) } ?: return null
        return cmd.removePrefix(prefix).trim().ifEmpty { null }
    }

    private fun searchTarget(cmd: String): String? {
        val prefixes = listOf("cari ", "carikan ", "search for ", "search ", "google ")
        val prefix = prefixes.firstOrNull { cmd.startsWith(it) } ?: return null
        return cmd.removePrefix(prefix).trim().ifEmpty { null }
    }

    private fun String.containsAny(vararg keys: String): Boolean = keys.any { this.contains(it) }
}

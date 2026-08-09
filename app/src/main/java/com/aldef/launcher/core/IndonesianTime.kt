package com.aldef.launcher.core

import java.util.Calendar
import java.util.TimeZone

data class ResolvedZone(val timeZone: TimeZone, val label: String)

/**
 * Menentukan zona waktu Indonesia (WIB / WITA / WIT) yang dipakai jam dan sapaan.
 *
 * Urutan penentuan:
 *  1. Zona bawaan perangkat, bila sudah zona Indonesia — ini paling akurat karena
 *     sistem sudah memperhitungkan batas provinsi yang sebenarnya.
 *  2. Bila tidak, koordinat GPS dipakai selama titiknya masih di dalam Indonesia.
 *  3. Selain itu, zona perangkat dipakai apa adanya (mis. pengguna sedang di luar negeri).
 */
object IndonesianTime {

    private val INDONESIAN_ZONES = mapOf(
        "Asia/Jakarta" to "WIB",
        "Asia/Pontianak" to "WIB",
        "Asia/Makassar" to "WITA",
        "Asia/Ujung_Pandang" to "WITA",
        "Asia/Jayapura" to "WIT",
    )

    fun resolve(latitude: Double?, longitude: Double?): ResolvedZone {
        val device = TimeZone.getDefault()
        INDONESIAN_ZONES[device.id]?.let { return ResolvedZone(device, it) }

        if (latitude != null && longitude != null && isInsideIndonesia(latitude, longitude)) {
            // Pembagian berdasarkan bujur. Batas resmi WIB/WITA berkelok mengikuti
            // batas provinsi, jadi titik dekat perbatasan (mis. Kalimantan Selatan)
            // bisa meleset satu jam. Jalur ini hanya terpakai kalau zona perangkat
            // bukan zona Indonesia, yang jarang terjadi.
            return when {
                longitude < 114.8 -> ResolvedZone(TimeZone.getTimeZone("Asia/Jakarta"), "WIB")
                longitude < 134.0 -> ResolvedZone(TimeZone.getTimeZone("Asia/Makassar"), "WITA")
                else -> ResolvedZone(TimeZone.getTimeZone("Asia/Jayapura"), "WIT")
            }
        }

        return ResolvedZone(device, "")
    }

    private fun isInsideIndonesia(lat: Double, lon: Double): Boolean =
        lat in -11.5..6.5 && lon in 94.5..141.5

    /** Jam 0-23 pada zona yang sudah ditentukan. */
    fun hourOfDay(zone: ResolvedZone): Int =
        Calendar.getInstance(zone.timeZone).get(Calendar.HOUR_OF_DAY)

    /**
     * Sapaan mengikuti kebiasaan Indonesia:
     * pagi 04–10, siang 11–14, sore 15–17, malam 18–03.
     */
    fun greeting(zone: ResolvedZone, indonesian: Boolean): String {
        val hour = hourOfDay(zone)
        return if (indonesian) {
            when (hour) {
                in 4..10 -> "Selamat pagi"
                in 11..14 -> "Selamat siang"
                in 15..17 -> "Selamat sore"
                else -> "Selamat malam"
            }
        } else {
            when (hour) {
                in 4..10 -> "Good morning"
                in 11..14 -> "Good afternoon"
                in 15..17 -> "Good evening"
                else -> "Good night"
            }
        }
    }
}

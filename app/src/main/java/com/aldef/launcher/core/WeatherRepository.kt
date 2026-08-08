package com.aldef.launcher.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Weather(val temperatureC: Int, val icon: String, val descriptionId: String, val descriptionEn: String)

/**
 * Open-Meteo: gratis, tanpa API key, tanpa registrasi.
 * Dipanggil lewat HttpURLConnection agar tidak menambah dependensi HTTP baru.
 */
class WeatherRepository {

    suspend fun fetch(place: Place): Weather? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,weather_code&timezone=auto"

        val body = runCatching {
            (URL(url).openConnection() as HttpURLConnection).run {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                try {
                    if (responseCode !in 200..299) return@run null
                    inputStream.bufferedReader().use { it.readText() }
                } finally {
                    disconnect()
                }
            }
        }.getOrNull() ?: return@withContext null

        runCatching {
            val current = JSONObject(body).getJSONObject("current")
            val temp = current.getDouble("temperature_2m")
            val code = current.getInt("weather_code")
            describe(code, Math.round(temp).toInt())
        }.getOrNull()
    }

    /** Kode cuaca WMO -> ikon + keterangan. */
    private fun describe(code: Int, temp: Int): Weather = when (code) {
        0 -> Weather(temp, "☀", "Cerah", "Clear")
        1, 2 -> Weather(temp, "🌤", "Cerah berawan", "Partly cloudy")
        3 -> Weather(temp, "☁", "Berawan", "Cloudy")
        45, 48 -> Weather(temp, "🌫", "Berkabut", "Foggy")
        51, 53, 55, 56, 57 -> Weather(temp, "🌦", "Gerimis", "Drizzle")
        61, 63, 65, 66, 67 -> Weather(temp, "🌧", "Hujan", "Rain")
        71, 73, 75, 77 -> Weather(temp, "🌨", "Salju", "Snow")
        80, 81, 82 -> Weather(temp, "🌧", "Hujan deras", "Heavy showers")
        95, 96, 99 -> Weather(temp, "⛈", "Badai petir", "Thunderstorm")
        else -> Weather(temp, "🌤", "Berawan", "Cloudy")
    }
}

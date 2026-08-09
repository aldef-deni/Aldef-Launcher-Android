package com.aldef.launcher.core

import android.content.Context

/** Penyimpanan sederhana untuk preferensi pengguna. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("aldef", Context.MODE_PRIVATE)

    var userName: String
        get() = sp.getString(KEY_NAME, "DENI") ?: "DENI"
        set(v) = sp.edit().putString(KEY_NAME, v.uppercase()).apply()

    /** "id" atau "en" */
    var language: String
        get() = sp.getString(KEY_LANG, "id") ?: "id"
        set(v) = sp.edit().putString(KEY_LANG, v).apply()

    var speakOnHome: Boolean
        get() = sp.getBoolean(KEY_SPEAK, true)
        set(v) = sp.edit().putBoolean(KEY_SPEAK, v).apply()

    var apiKey: String
        get() = sp.getString(KEY_API, "") ?: ""
        set(v) = sp.edit().putString(KEY_API, v.trim()).apply()

    /** True setelah pengguna mengisi namanya di modal perkenalan. */
    var nameAsked: Boolean
        get() = sp.getBoolean(KEY_NAME_ASKED, false)
        set(v) = sp.edit().putBoolean(KEY_NAME_ASKED, v).apply()

    /** Saklar utama antarmuka HUD. Mati sampai pengguna mengaktifkannya. */
    var hudEnabled: Boolean
        get() = sp.getBoolean(KEY_HUD, false)
        set(v) = sp.edit().putBoolean(KEY_HUD, v).apply()

    /** Layar kunci Aldef (lapisan di atas keyguard, bukan pengganti keamanan). */
    var lockScreenEnabled: Boolean
        get() = sp.getBoolean(KEY_LOCK, false)
        set(v) = sp.edit().putBoolean(KEY_LOCK, v).apply()

    /**
     * Cuaca dan lokasi terakhir dari HUD. Layar kunci membacanya agar bisa
     * tampil seketika tanpa menunggu GPS atau jaringan.
     */
    fun cacheAmbient(
        temperature: Int?,
        weatherText: String,
        weatherIcon: String,
        place: String,
        latitude: Double,
        longitude: Double,
    ) {
        sp.edit()
            .putInt(KEY_TEMP, temperature ?: Int.MIN_VALUE)
            .putString(KEY_WEATHER, weatherText)
            .putString(KEY_WEATHER_ICON, weatherIcon)
            .putString(KEY_PLACE, place)
            .putFloat(KEY_LAT, latitude.toFloat())
            .putFloat(KEY_LON, longitude.toFloat())
            .apply()
    }

    /** Koordinat terakhir; layar kunci memakainya untuk menentukan WIB/WITA/WIT. */
    val cachedLatitude: Double?
        get() = sp.getFloat(KEY_LAT, Float.NaN).takeIf { !it.isNaN() }?.toDouble()
    val cachedLongitude: Double?
        get() = sp.getFloat(KEY_LON, Float.NaN).takeIf { !it.isNaN() }?.toDouble()

    val cachedTemperature: Int?
        get() = sp.getInt(KEY_TEMP, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
    val cachedWeatherText: String get() = sp.getString(KEY_WEATHER, "") ?: ""
    val cachedWeatherIcon: String get() = sp.getString(KEY_WEATHER_ICON, "🌤") ?: "🌤"
    val cachedPlace: String get() = sp.getString(KEY_PLACE, "") ?: ""

    val isIndonesian: Boolean get() = language == "id"

    private companion object {
        const val KEY_NAME = "user_name"
        const val KEY_LANG = "language"
        const val KEY_SPEAK = "speak_on_home"
        const val KEY_API = "anthropic_api_key"
        const val KEY_HUD = "hud_enabled"
        const val KEY_NAME_ASKED = "name_asked"
        const val KEY_LOCK = "lock_screen_enabled"
        const val KEY_TEMP = "cached_temperature"
        const val KEY_WEATHER = "cached_weather"
        const val KEY_WEATHER_ICON = "cached_weather_icon"
        const val KEY_PLACE = "cached_place"
        const val KEY_LAT = "cached_lat"
        const val KEY_LON = "cached_lon"
    }
}

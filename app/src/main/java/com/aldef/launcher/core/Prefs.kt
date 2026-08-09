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

    val isIndonesian: Boolean get() = language == "id"

    private companion object {
        const val KEY_NAME = "user_name"
        const val KEY_LANG = "language"
        const val KEY_SPEAK = "speak_on_home"
        const val KEY_API = "anthropic_api_key"
        const val KEY_HUD = "hud_enabled"
        const val KEY_NAME_ASKED = "name_asked"
    }
}

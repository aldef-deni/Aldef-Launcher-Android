package com.aldef.launcher.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Pembungkus TextToSpeech dengan antrean sederhana. */
class Speaker(context: Context) {

    private var ready = false
    private var pending: String? = null
    private var onDone: (() -> Unit)? = null

    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            pending?.let { speak(it); pending = null }
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                onDone?.invoke()
            }

            @Deprecated("Diganti onError(String, Int) pada API 21+")
            override fun onError(utteranceId: String?) {
                onDone?.invoke()
            }
        })
    }

    fun setLanguage(code: String) {
        val locale = if (code == "id") Locale("in", "ID") else Locale.US
        runCatching {
            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.US
            }
        }
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (text.isBlank()) return
        onDone = onFinished
        if (!ready) {
            pending = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aldef-${System.currentTimeMillis()}")
    }

    fun stop() {
        runCatching { tts.stop() }
    }

    fun shutdown() {
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
    }
}

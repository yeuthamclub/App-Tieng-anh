package com.yeuthamclub.tienganh

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Reads English aloud with the phone's built-in text-to-speech (free, works offline). */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    var ready = false
        private set

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.US)
        ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        tts.setSpeechRate(0.85f)
    }

    fun say(text: String, slow: Boolean = false) {
        if (!ready) return
        tts.setSpeechRate(if (slow) 0.5f else 0.85f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    fun stop() {
        tts.stop()
    }

    fun shutdown() = tts.shutdown()
}

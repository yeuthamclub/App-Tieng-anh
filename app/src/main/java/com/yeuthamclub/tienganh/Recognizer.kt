package com.yeuthamclub.tienganh

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Listens to one utterance with the phone's built-in speech recognizer (free). */
class Recognizer(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(onResult: (List<String>) -> Unit, onError: (String) -> Unit) {
        stop()
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                stop()
                onResult(texts)
            }

            override fun onError(error: Int) {
                stop()
                onError(message(error))
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        sr.startListening(intent)
    }

    fun stop() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun message(error: Int) = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Máy chưa nghe rõ, bấm micro và nói lại nhé."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "App cần quyền dùng micro."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Không có mạng. Tải gói nhận dạng giọng nói tiếng Anh (offline) trong cài đặt Google để dùng không cần mạng."
        else -> "Lỗi nhận dạng giọng nói (mã $error), thử lại nhé."
    }
}

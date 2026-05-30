package com.agentvoice.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    private val textToSpeech = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            textToSpeech.language = Locale.getDefault()
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) {
            return
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agentvoice-response")
    }

    fun stop() {
        textToSpeech.stop()
    }

    fun shutdown() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}

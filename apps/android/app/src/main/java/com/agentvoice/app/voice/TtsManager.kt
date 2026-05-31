package com.agentvoice.app.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val completionCallbacks = ConcurrentHashMap<String, TtsCallbacks>()
    private val textToSpeech = TextToSpeech(context.applicationContext, this)

    init {
        textToSpeech.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    start(utteranceId)
                }

                override fun onDone(utteranceId: String?) {
                    complete(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    complete(utteranceId)
                }
            }
        )
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            textToSpeech.language = Locale.getDefault()
        }
    }

    fun speak(
        text: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!ready || text.isBlank()) {
            onDone?.invoke()
            return
        }

        val utteranceId = "agentvoice-response-${System.currentTimeMillis()}"
        if (onStart != null || onDone != null) {
            completionCallbacks[utteranceId] = TtsCallbacks(onStart, onDone)
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        completionCallbacks.clear()
        textToSpeech.stop()
    }

    fun shutdown() {
        completionCallbacks.clear()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun complete(utteranceId: String?) {
        if (utteranceId == null) {
            return
        }

        val callback = completionCallbacks.remove(utteranceId)?.onDone ?: return
        mainHandler.post { callback() }
    }

    private fun start(utteranceId: String?) {
        if (utteranceId == null) {
            return
        }

        val callback = completionCallbacks[utteranceId]?.onStart ?: return
        mainHandler.post { callback() }
    }

    private data class TtsCallbacks(
        val onStart: (() -> Unit)?,
        val onDone: (() -> Unit)?
    )
}

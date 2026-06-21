package com.agentvoice.app.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.File

class VoiceClipRecorder(
    private val context: Context,
    private val listener: Listener
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var stopping = false
    private var startedAtMs = 0L
    private var lastVoiceAtMs = 0L
    private var heardVoice = false

    private val endpointRunnable = object : Runnable {
        override fun run() {
            val activeRecorder = recorder ?: return
            if (stopping) {
                return
            }

            val now = SystemClock.elapsedRealtime()
            val amplitude = runCatching { activeRecorder.maxAmplitude }.getOrDefault(0)
            if (amplitude >= VOICE_AMPLITUDE_THRESHOLD) {
                heardVoice = true
                lastVoiceAtMs = now
            }

            val elapsedMs = now - startedAtMs
            val silenceMs = now - lastVoiceAtMs
            val shouldStopAfterVoice =
                heardVoice &&
                    elapsedMs >= MIN_RECORDING_MS &&
                    silenceMs >= SILENCE_AFTER_VOICE_MS
            val shouldStopAfterQuiet =
                !heardVoice && elapsedMs >= NO_SPEECH_TIMEOUT_MS

            if (shouldStopAfterVoice || shouldStopAfterQuiet) {
                stop()
            } else {
                handler.postDelayed(this, AMPLITUDE_POLL_MS)
            }
        }
    }

    interface Listener {
        fun onRecordingStarted()
        fun onRecordingFinished(file: File)
        fun onRecordingError(message: String)
    }

    fun start(maxDurationMs: Int = DEFAULT_MAX_DURATION_MS) {
        if (recorder != null) {
            return
        }

        val file = File.createTempFile("agentvoice-voice-", ".m4a", context.cacheDir)
        outputFile = file

        val activeRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder = activeRecorder
        stopping = false
        heardVoice = false

        try {
            activeRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            activeRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            activeRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            activeRecorder.setAudioSamplingRate(16_000)
            activeRecorder.setAudioEncodingBitRate(64_000)
            activeRecorder.setOutputFile(file.absolutePath)
            activeRecorder.setMaxDuration(maxDurationMs)
            activeRecorder.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stop()
                }
            }
            activeRecorder.prepare()
            activeRecorder.start()
            startedAtMs = SystemClock.elapsedRealtime()
            lastVoiceAtMs = startedAtMs
            handler.postDelayed(endpointRunnable, AMPLITUDE_POLL_MS)
            listener.onRecordingStarted()
        } catch (error: Exception) {
            cleanup(deleteFile = true)
            listener.onRecordingError(error.message ?: "Unable to record audio.")
        }
    }

    fun stop() {
        val activeRecorder = recorder ?: return
        if (stopping) {
            return
        }

        stopping = true
        val file = outputFile
        try {
            activeRecorder.stop()
            cleanup(deleteFile = false)
            if (file != null && file.exists() && file.length() > 0) {
                listener.onRecordingFinished(file)
            } else {
                file?.delete()
                listener.onRecordingError("No audio was recorded.")
            }
        } catch (error: Exception) {
            cleanup(deleteFile = true)
            listener.onRecordingError(error.message ?: "Unable to finish recording.")
        }
    }

    fun cancel() {
        cleanup(deleteFile = true)
    }

    fun destroy() {
        cleanup(deleteFile = true)
    }

    private fun cleanup(deleteFile: Boolean) {
        handler.removeCallbacks(endpointRunnable)
        runCatching {
            recorder?.reset()
            recorder?.release()
        }
        recorder = null
        stopping = false
        heardVoice = false

        if (deleteFile) {
            outputFile?.delete()
        }
        outputFile = null
    }

    companion object {
        const val DEFAULT_MAX_DURATION_MS = 60_000
        private const val MIN_RECORDING_MS = 1_100L
        private const val NO_SPEECH_TIMEOUT_MS = 3_000L
        private const val SILENCE_AFTER_VOICE_MS = 2_500L
        private const val AMPLITUDE_POLL_MS = 120L
        private const val VOICE_AMPLITUDE_THRESHOLD = 1_200
    }
}

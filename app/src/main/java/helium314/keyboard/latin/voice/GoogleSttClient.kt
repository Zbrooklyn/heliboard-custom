// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Google on-device speech-to-text using Android's SpeechRecognizer API.
 * No API key needed — uses the device's built-in Google speech recognition.
 *
 * Operates in persistent voice mode: the recognizer stays alive, accumulating
 * and auto-submitting text chunks on silence. Voice mode only exits when
 * [stopAndFinalize] is called explicitly by the user (mic tap / keyboard button).
 *
 * Two callback types:
 * - [ChunkCallback] — fired on auto-submit after silence. Text is delivered
 *   but the recognizer keeps listening for more speech.
 * - [ResultCallback] — fired on user-initiated stop. Final delivery, session ends.
 *
 * Must be called from the main thread.
 */
class GoogleSttClient(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isListening = false
    @Volatile
    private var userStopped = false

    // Accumulated text from the current speech segment
    private val accumulatedText = StringBuilder()

    // Callbacks
    private var currentOnResult: ResultCallback? = null
    private var currentOnChunk: ChunkCallback? = null
    private var currentOnError: ErrorCallback? = null
    private var currentOnPartial: PartialCallback? = null

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /** Counts consecutive silence restarts (no speech detected). Reset when speech is received. */
    private var silenceRestartCount = 0

    /** Auto-submit chunk after silence — delivers text but keeps listening. */
    private val autoSubmitRunnable = Runnable {
        if (!userStopped && accumulatedText.isNotEmpty()) {
            Log.d(TAG, "Auto-submitting chunk after silence (${AUTO_SUBMIT_SILENCE_MS / 1000}s)")
            submitChunkAndContinue()
        }
    }

    /** Restart runnable — separated so it can be cancelled independently. */
    private val restartRunnable = Runnable {
        if (!userStopped) {
            startRecognizer()
        }
    }

    companion object {
        private const val TAG = "GoogleSttClient"
        private const val MAX_ACCUMULATED_CHARS = 10000

        /** Delay before restarting recognizer to let it fully shut down. */
        private const val RESTART_DELAY_MS = 200L

        /** Silence before Google considers speech complete (default ~1.5s → 4s). */
        private const val COMPLETE_SILENCE_MS = 4000L
        /** Silence that might mean speech is complete (default ~1s → 3s). */
        private const val POSSIBLY_COMPLETE_SILENCE_MS = 3000L
        /** Minimum speech duration before silence detection kicks in. */
        private const val MINIMUM_LENGTH_MS = 2000L

        /** After receiving results, if no new speech for this long, auto-submit the chunk. */
        private const val AUTO_SUBMIT_SILENCE_MS = 5000L
        /** Max consecutive silence cycles before auto-submitting a chunk. */
        private const val MAX_SILENCE_RESTARTS = 2

        private fun errorCodeToString(error: Int): String = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error ($error)"
        }

        /** Only truly fatal errors should exit voice mode. Everything else auto-restarts. */
        private fun isFatalError(error: Int): Boolean = when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> true
            else -> false
        }
    }

    /** Called on user-initiated stop — final delivery, session ends. */
    fun interface ResultCallback {
        fun onResult(text: String?)
    }

    /** Called on auto-submit after silence — text delivered, keeps listening. */
    fun interface ChunkCallback {
        fun onChunk(text: String)
    }

    fun interface ErrorCallback {
        fun onError(errorMessage: String)
    }

    fun interface PartialCallback {
        fun onPartial(text: String)
    }

    /** Check if speech recognition is available on this device. */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    // Language and offline preference
    private var requestedLocale: java.util.Locale? = null
    private var requestedOffline: Boolean = false

    /**
     * Start persistent voice mode. Auto-submits text chunks on silence via [onChunk].
     * Only exits when [stopAndFinalize] is called, delivering any remaining text via [onResult].
     */
    fun startListening(
        onResult: ResultCallback,
        onChunk: ChunkCallback? = null,
        onError: ErrorCallback,
        onListeningStarted: (() -> Unit)? = null,
        onPartial: PartialCallback? = null,
        locale: java.util.Locale? = null,
        preferOffline: Boolean = false
    ) {
        if (isListening) {
            Log.w(TAG, "Already listening, ignoring startListening()")
            return
        }

        userStopped = false
        silenceRestartCount = 0
        accumulatedText.clear()
        currentOnResult = onResult
        currentOnChunk = onChunk
        currentOnError = onError
        currentOnPartial = onPartial
        requestedLocale = locale
        requestedOffline = preferOffline

        startRecognizer(onListeningStarted)
    }

    private fun startRecognizer(onListeningStarted: (() -> Unit)? = null) {
        if (userStopped) return

        destroyRecognizer()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e)
            currentOnError?.onError("Speech recognition not available")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            requestedLocale?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it.toLanguageTag()) }
            if (requestedOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            // Extend silence timeouts for dictation
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", COMPLETE_SILENCE_MS)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", POSSIBLY_COMPLETE_SILENCE_MS)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", MINIMUM_LENGTH_MS)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onListeningStarted?.invoke()
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
                // User started talking — cancel auto-submit and reset silence count
                mainHandler.removeCallbacks(autoSubmitRunnable)
                silenceRestartCount = 0
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
            }

            override fun onError(error: Int) {
                isListening = false
                if (userStopped) return

                val msg = errorCodeToString(error)
                Log.d(TAG, "Recognition cycle ended: $msg (code=$error)")

                if (isFatalError(error)) {
                    // Truly fatal — report to caller and shut down
                    Log.w(TAG, "Fatal error (code=$error): $msg")
                    cancelAllPending()
                    currentOnError?.onError("$msg (code=$error)")
                    destroyRecognizer()
                    return
                }

                // Everything else is recoverable — auto-restart silently
                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_NO_MATCH) {
                    silenceRestartCount++
                }

                // Hit silence limit with accumulated text — auto-submit chunk, keep going
                if (silenceRestartCount >= MAX_SILENCE_RESTARTS && accumulatedText.isNotEmpty()) {
                    Log.d(TAG, "Max silence restarts — auto-submitting chunk")
                    submitChunkAndContinue()
                    return
                }

                Log.d(TAG, "Auto-restarting (code=$error, silenceCount=$silenceRestartCount)")
                scheduleRestart()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                if (userStopped) {
                    // Grab final text and deliver via stopAndFinalize path
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        if (accumulatedText.isNotEmpty()) accumulatedText.append(" ")
                        accumulatedText.append(text.trim())
                    }
                    deliverFinalResult()
                    return
                }

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    silenceRestartCount = 0
                    if (accumulatedText.length < MAX_ACCUMULATED_CHARS) {
                        if (accumulatedText.isNotEmpty()) accumulatedText.append(" ")
                        accumulatedText.append(text.trim())
                    }
                    Log.d(TAG, "Accumulated: ${accumulatedText.length} chars")
                } else {
                    silenceRestartCount++
                }

                // Start auto-submit timer — if no new speech, submit chunk and keep listening
                mainHandler.removeCallbacks(autoSubmitRunnable)
                if (accumulatedText.isNotEmpty()) {
                    mainHandler.postDelayed(autoSubmitRunnable, AUTO_SUBMIT_SILENCE_MS)
                }

                // Check if we've exceeded silence restarts with text
                if (silenceRestartCount >= MAX_SILENCE_RESTARTS && accumulatedText.isNotEmpty()) {
                    Log.d(TAG, "Max silence restarts from empty results — auto-submitting chunk")
                    submitChunkAndContinue()
                    return
                }

                // Auto-restart for continuous recording
                Log.d(TAG, "Auto-restarting for continuous recording")
                scheduleRestart()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    val full = if (accumulatedText.isNotEmpty()) {
                        "${accumulatedText} $partial"
                    } else {
                        partial
                    }
                    currentOnPartial?.onPartial(full)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            currentOnError?.onError("Failed to start speech recognition")
            destroyRecognizer()
        }
    }

    /**
     * Auto-submit the current accumulated text as a chunk, then keep listening.
     * Does NOT exit voice mode.
     */
    private fun submitChunkAndContinue() {
        cancelAllPending()
        val chunk = accumulatedText.toString().trim()
        accumulatedText.clear()
        silenceRestartCount = 0

        if (chunk.isNotEmpty()) {
            Log.d(TAG, "Chunk submitted: ${chunk.take(50)}")
            currentOnChunk?.onChunk(chunk)
        }

        // Keep listening — restart the recognizer
        scheduleRestart()
    }

    private fun scheduleRestart() {
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed(restartRunnable, RESTART_DELAY_MS)
    }

    private fun cancelAllPending() {
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.removeCallbacks(autoSubmitRunnable)
    }

    /**
     * User-initiated stop. Delivers any remaining accumulated text via [ResultCallback]
     * and shuts down the session. This is the ONLY way to exit voice mode.
     */
    fun stopAndFinalize() {
        userStopped = true
        cancelAllPending()
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) { }
            // onResults/onError will fire and see userStopped=true → deliverFinalResult()
        } else {
            deliverFinalResult()
        }
    }

    /** Deliver final result and clean up. Only called on user-initiated stop. */
    private fun deliverFinalResult() {
        cancelAllPending()
        val result = accumulatedText.toString().trim().ifEmpty { null }
        Log.d(TAG, "Final result: ${result?.take(50) ?: "(empty)"}")
        currentOnResult?.onResult(result)
        accumulatedText.clear()
        currentOnResult = null
        currentOnChunk = null
        currentOnError = null
        currentOnPartial = null
        destroyRecognizer()
    }

    /** Stop listening — legacy method, calls stopAndFinalize. */
    fun stopListening() {
        stopAndFinalize()
    }

    /** Cancel and release resources without delivering results. */
    fun cancel() {
        userStopped = true
        isListening = false
        cancelAllPending()
        accumulatedText.clear()
        currentOnResult = null
        currentOnChunk = null
        currentOnError = null
        currentOnPartial = null
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) { }
        destroyRecognizer()
    }

    fun isCurrentlyListening(): Boolean = isListening || (!userStopped && accumulatedText.isNotEmpty())

    private fun destroyRecognizer() {
        isListening = false
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) { }
        speechRecognizer = null
    }
}

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
 * Operates in continuous mode: when the recognizer auto-stops (silence timeout,
 * end of utterance), it accumulates the result and auto-restarts. Recording stops
 * when [stopAndFinalize] is called explicitly, or automatically after a silence
 * period following accumulated speech (Samsung-like auto-submit behavior).
 *
 * Must be called from the main thread.
 */
class GoogleSttClient(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isListening = false
    @Volatile
    private var userStopped = false
    @Volatile
    private var finalized = false

    // Accumulated text from multiple recognition sessions
    private val accumulatedText = StringBuilder()

    // Callbacks — stored for auto-restart
    private var currentOnResult: ResultCallback? = null
    private var currentOnError: ErrorCallback? = null
    private var currentOnPartial: PartialCallback? = null

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /** Counts consecutive silence restarts (no speech detected). Reset when speech is received. */
    private var silenceRestartCount = 0

    /** Auto-finalize after silence — delivers accumulated text without user tapping mic. */
    private val autoFinalizeRunnable = Runnable {
        if (!userStopped && !finalized && accumulatedText.isNotEmpty()) {
            Log.d(TAG, "Auto-finalizing after silence (${AUTO_FINALIZE_SILENCE_MS / 1000}s)")
            safeFinalize()
        }
    }

    /** Restart runnable — separated so it can be cancelled independently. */
    private val restartRunnable = Runnable {
        if (!userStopped && !finalized) {
            startRecognizer()
        }
    }

    companion object {
        private const val TAG = "GoogleSttClient"
        private const val MAX_ACCUMULATED_CHARS = 10000 // ~2000 words

        /** Delay before restarting recognizer to let it fully shut down. */
        private const val RESTART_DELAY_MS = 200L

        /** Silence before Google considers speech complete (default ~1.5s → 4s). */
        private const val COMPLETE_SILENCE_MS = 4000L
        /** Silence that might mean speech is complete (default ~1s → 3s). */
        private const val POSSIBLY_COMPLETE_SILENCE_MS = 3000L
        /** Minimum speech duration before silence detection kicks in. */
        private const val MINIMUM_LENGTH_MS = 2000L

        /** After receiving results, if no new speech for this long, auto-submit. */
        private const val AUTO_FINALIZE_SILENCE_MS = 5000L
        /** Max consecutive silence cycles before auto-finalizing (prevents endless restart loop). */
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

        /** Errors that should trigger auto-restart instead of surfacing to the user. */
        private fun isRecoverableError(error: Int): Boolean = when (error) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_CLIENT -> true
            else -> false
        }
    }

    fun interface ResultCallback {
        fun onResult(text: String?)
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

    /**
     * Start continuous listening. Results accumulate until [stopAndFinalize] is called
     * or auto-finalize triggers after silence.
     * Partial results are delivered via [onPartial] for live preview.
     */
    // Language and offline preference — set by startListening(), used by startRecognizer()
    private var requestedLocale: java.util.Locale? = null
    private var requestedOffline: Boolean = false

    fun startListening(
        onResult: ResultCallback,
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
        finalized = false
        silenceRestartCount = 0
        accumulatedText.clear()
        currentOnResult = onResult
        currentOnError = onError
        currentOnPartial = onPartial
        requestedLocale = locale
        requestedOffline = preferOffline

        startRecognizer(onListeningStarted)
    }

    private fun startRecognizer(onListeningStarted: (() -> Unit)? = null) {
        // Don't start if already finalized or user stopped
        if (finalized || userStopped) return

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
            // Extend silence timeouts for dictation — Google's defaults (~1.5s) are too
            // aggressive for natural speech with pauses. These give the user time to think.
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
                // User started talking again — cancel auto-finalize and reset silence count
                mainHandler.removeCallbacks(autoFinalizeRunnable)
                silenceRestartCount = 0
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended (will auto-restart if user hasn't stopped)")
            }

            override fun onError(error: Int) {
                isListening = false
                if (finalized || userStopped) return

                val msg = errorCodeToString(error)
                Log.d(TAG, "Recognition cycle ended: $msg (code=$error)")

                if (userStopped) {
                    // User already stopped — deliver accumulated results
                    safeFinalize()
                    return
                }

                // Recoverable errors — auto-restart unless we've hit the silence limit
                if (isRecoverableError(error)) {
                    // Count silence-type restarts (no speech detected)
                    if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_NO_MATCH) {
                        silenceRestartCount++
                    }

                    // If we've hit the silence limit and have accumulated text, auto-submit
                    if (silenceRestartCount >= MAX_SILENCE_RESTARTS && accumulatedText.isNotEmpty()) {
                        Log.d(TAG, "Max silence restarts ($MAX_SILENCE_RESTARTS) reached — auto-finalizing")
                        safeFinalize()
                        return
                    }

                    // If we've hit silence limit with NO text, just stop cleanly
                    if (silenceRestartCount >= MAX_SILENCE_RESTARTS) {
                        Log.d(TAG, "Max silence restarts with no text — stopping")
                        safeFinalize()
                        return
                    }

                    Log.d(TAG, "Auto-restarting after recoverable error (code=$error, silenceCount=$silenceRestartCount)")
                    scheduleRestart()
                    return
                }

                // Real errors — report to caller with error code for debugging
                Log.w(TAG, "Non-recoverable error (code=$error): $msg")
                currentOnError?.onError("$msg (code=$error)")
                destroyRecognizer()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                if (finalized) return

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    // Got real speech — reset silence counter
                    silenceRestartCount = 0
                    if (accumulatedText.length < MAX_ACCUMULATED_CHARS) {
                        if (accumulatedText.isNotEmpty()) accumulatedText.append(" ")
                        accumulatedText.append(text.trim())
                    }
                    Log.d(TAG, "Accumulated: ${accumulatedText.length} chars")
                } else {
                    // Empty result counts as silence
                    silenceRestartCount++
                }

                if (userStopped) {
                    safeFinalize()
                } else {
                    // Schedule auto-finalize timer — if no new speech within the timeout,
                    // auto-submit the accumulated text (Samsung-like behavior)
                    mainHandler.removeCallbacks(autoFinalizeRunnable)
                    if (accumulatedText.isNotEmpty()) {
                        mainHandler.postDelayed(autoFinalizeRunnable, AUTO_FINALIZE_SILENCE_MS)
                    }

                    // Check if we've exceeded silence restarts (empty results loop)
                    if (silenceRestartCount >= MAX_SILENCE_RESTARTS && accumulatedText.isNotEmpty()) {
                        Log.d(TAG, "Max silence restarts from empty results — auto-finalizing")
                        safeFinalize()
                        return
                    }

                    // Auto-restart for continuous recording
                    Log.d(TAG, "Auto-restarting for continuous recording")
                    scheduleRestart()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    // Show accumulated + current partial
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

    /** Schedule a delayed restart, cancelling any previously pending one. */
    private fun scheduleRestart() {
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed(restartRunnable, RESTART_DELAY_MS)
    }

    /** Cancel all pending restarts and auto-finalize timers. */
    private fun cancelAllPending() {
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.removeCallbacks(autoFinalizeRunnable)
    }

    /**
     * User-initiated stop. Stops the recognizer and delivers all accumulated text.
     * If the recognizer is between sessions (auto-restart gap), delivers immediately.
     */
    fun stopAndFinalize() {
        userStopped = true
        cancelAllPending()
        if (isListening) {
            // Recognizer is active — stopListening() will trigger onResults/onError
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) { }
        } else {
            // Recognizer is between sessions (restart gap) — deliver now
            safeFinalize()
        }
    }

    /** Thread-safe finalize — prevents double-delivery. */
    private fun safeFinalize() {
        if (finalized) return
        finalized = true
        cancelAllPending()
        val result = accumulatedText.toString().trim().ifEmpty { null }
        Log.d(TAG, "Final result: ${result?.take(50) ?: "(empty)"}")
        currentOnResult?.onResult(result)
        accumulatedText.clear()
        currentOnResult = null
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
        finalized = true
        isListening = false
        cancelAllPending()
        accumulatedText.clear()
        currentOnResult = null
        currentOnError = null
        currentOnPartial = null
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) { }
        destroyRecognizer()
    }

    fun isCurrentlyListening(): Boolean = isListening || (!userStopped && !finalized && accumulatedText.isNotEmpty())

    private fun destroyRecognizer() {
        isListening = false
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) { }
        speechRecognizer = null
    }
}

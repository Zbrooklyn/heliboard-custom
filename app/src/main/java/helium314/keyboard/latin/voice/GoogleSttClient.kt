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
 * end of utterance), it accumulates the result and auto-restarts. Recording only
 * stops when [stopAndFinalize] is called explicitly by the user.
 *
 * Must be called from the main thread.
 */
class GoogleSttClient(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isListening = false
    @Volatile
    private var userStopped = false

    // Accumulated text from multiple recognition sessions
    private val accumulatedText = StringBuilder()

    // Callbacks — stored for auto-restart
    private var currentOnResult: ResultCallback? = null
    private var currentOnError: ErrorCallback? = null
    private var currentOnPartial: PartialCallback? = null

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
     * Start continuous listening. Results accumulate until [stopAndFinalize] is called.
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
        accumulatedText.clear()
        currentOnResult = onResult
        currentOnError = onError
        currentOnPartial = onPartial
        requestedLocale = locale
        requestedOffline = preferOffline

        startRecognizer(onListeningStarted)
    }

    private fun startRecognizer(onListeningStarted: (() -> Unit)? = null) {
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
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onListeningStarted?.invoke()
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended (will auto-restart if user hasn't stopped)")
            }

            override fun onError(error: Int) {
                isListening = false
                val msg = errorCodeToString(error)
                Log.d(TAG, "Recognition cycle ended: $msg (code=$error)")

                if (userStopped) {
                    // User already stopped — deliver accumulated results
                    finalizeResults()
                    return
                }

                // On silence timeout or no match, auto-restart
                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_NO_MATCH) {
                    Log.d(TAG, "Auto-restarting after silence")
                    startRecognizer()
                    return
                }

                // Real errors — report to caller
                currentOnError?.onError(msg)
                destroyRecognizer()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    if (accumulatedText.length < MAX_ACCUMULATED_CHARS) {
                        if (accumulatedText.isNotEmpty()) accumulatedText.append(" ")
                        accumulatedText.append(text.trim())
                    }
                    Log.d(TAG, "Accumulated: ${accumulatedText.length} chars")
                }

                if (userStopped) {
                    // User already stopped — deliver accumulated results
                    finalizeResults()
                } else {
                    // Auto-restart for continuous recording
                    Log.d(TAG, "Auto-restarting for continuous recording")
                    startRecognizer()
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

    /**
     * User-initiated stop. Stops the recognizer and delivers all accumulated text.
     * If the recognizer is between sessions (auto-restart gap), delivers immediately.
     */
    fun stopAndFinalize() {
        userStopped = true
        if (isListening) {
            // Recognizer is active — stopListening() will trigger onResults/onError
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) { }
        } else {
            // Recognizer is between sessions — deliver now
            finalizeResults()
        }
    }

    private fun finalizeResults() {
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
        isListening = false
        accumulatedText.clear()
        currentOnResult = null
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

    companion object {
        private const val TAG = "GoogleSttClient"
        private const val MAX_ACCUMULATED_CHARS = 10000 // ~2000 words

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
    }
}

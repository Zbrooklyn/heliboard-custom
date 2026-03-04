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
 * Must be called from the main thread.
 */
class GoogleSttClient(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isListening = false

    fun interface ResultCallback {
        fun onResult(text: String?)
    }

    fun interface ErrorCallback {
        fun onError(errorMessage: String)
    }

    /** Check if speech recognition is available on this device. */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start listening for speech. Calls [onResult] with transcribed text or null.
     * Calls [onError] on failure. Must be called on the main thread.
     */
    fun startListening(
        onResult: ResultCallback,
        onError: ErrorCallback,
        onListeningStarted: (() -> Unit)? = null
    ) {
        if (isListening) {
            Log.w(TAG, "Already listening, ignoring startListening()")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e)
            onError.onError("Speech recognition not available")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
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

            override fun onRmsChanged(rmsdB: Float) {
                // Could use for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val msg = errorCodeToString(error)
                Log.e(TAG, "Recognition error: $msg (code=$error)")
                onError.onError(msg)
                destroyRecognizer()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                Log.d(TAG, "Result: ${text?.take(50) ?: "(empty)"}")
                onResult.onResult(text)
                destroyRecognizer()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Could show partial transcription for live feedback
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            onError.onError("Failed to start speech recognition")
            destroyRecognizer()
        }
    }

    /** Stop listening and cancel any in-progress recognition. */
    fun stopListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) { }
    }

    /** Cancel and release resources. */
    fun cancel() {
        isListening = false
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) { }
        destroyRecognizer()
    }

    fun isCurrentlyListening(): Boolean = isListening

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) { }
        speechRecognizer = null
    }

    companion object {
        private const val TAG = "GoogleSttClient"

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

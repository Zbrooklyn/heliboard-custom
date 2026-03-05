package helium314.keyboard.latin.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.whispercpp.whisper.WhisperContext
import helium314.keyboard.latin.ai.WhisperCloudClient
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.io.File

class VoiceInputManager(
    private val context: Context,
    private val onResult: ResultCallback,
    private val onStateChange: StateCallback,
    private val onPartialResult: PartialResultCallback? = null
) {
    enum class VoiceState { IDLE, LISTENING, TRANSCRIBING, ERROR }

    fun interface ResultCallback {
        fun onResult(text: String)
    }

    fun interface StateCallback {
        fun onStateChange(state: VoiceState)
    }

    fun interface PartialResultCallback {
        fun onPartialResult(text: String)
    }

    /** Callback for errors that should show specific messages (not generic "Voice input error"). */
    fun interface ErrorDetailCallback {
        fun onErrorDetail(message: String)
    }

    private val recorder = Recorder()
    @Volatile
    private var whisperContext: WhisperContext? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var state = VoiceState.IDLE
    private val mainHandler = Handler(Looper.getMainLooper())

    // Google STT client — lazily initialized
    private var googleSttClient: GoogleSttClient? = null

    /** Optional callback for detailed error messages (set by LatinIME). */
    var onErrorDetail: ErrorDetailCallback? = null

    val currentState: VoiceState get() = state

    companion object {
        private const val TAG = "VoiceInputManager"
        /** Max recording duration in milliseconds (5 minutes). */
        private const val MAX_RECORDING_DURATION_MS = 5 * 60 * 1000L
        /** Max local transcription timeout in milliseconds (60 seconds). */
        private const val LOCAL_TRANSCRIBE_TIMEOUT_MS = 60_000L
    }

    @JvmOverloads
    fun loadModel(modelPath: String, onLoaded: Runnable? = null) {
        scope.launch {
            try {
                Log.d(TAG, "Loading whisper model from: $modelPath")
                val startMs = System.currentTimeMillis()
                whisperContext = WhisperContext.createContextFromFile(modelPath)
                val elapsed = System.currentTimeMillis() - startMs
                Log.d(TAG, "Whisper model loaded successfully in ${elapsed}ms")
                withContext(Dispatchers.Main) {
                    onLoaded?.run()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load whisper model", e)
                withContext(Dispatchers.Main) {
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                    onLoaded?.run()
                }
            }
        }
    }

    fun isModelLoaded(): Boolean = whisperContext != null

    /** Unload the whisper model to free memory. Call when keyboard hides. */
    fun unloadModel() {
        if (whisperContext == null) return
        // Don't unload during active recording or transcription
        if (state == VoiceState.LISTENING || state == VoiceState.TRANSCRIBING) {
            Log.d(TAG, "Skipping unload — voice pipeline is active (state=$state)")
            return
        }
        val ctx = whisperContext
        whisperContext = null
        scope.launch {
            try {
                ctx?.release()
                Log.d(TAG, "Whisper model unloaded to save memory")
            } catch (e: Exception) {
                Log.e(TAG, "Error unloading model", e)
            }
        }
    }

    /** Get current STT mode: "local", "cloud", or "google". */
    fun getSttMode(): String {
        val prefs = context.prefs()
        return prefs.getString(Settings.PREF_STT_MODE, Defaults.PREF_STT_MODE) ?: Defaults.PREF_STT_MODE
    }

    /** Returns true if cloud STT mode is selected. */
    fun isCloudMode(): Boolean = getSttMode() == "cloud"

    /** Returns true if Google STT mode is selected. */
    fun isGoogleMode(): Boolean = getSttMode() == "google"

    /** Only local mode needs a downloaded whisper model. */
    fun needsLocalModel(): Boolean = getSttMode() == "local"

    fun getModelFile(): File? {
        return ModelManager.getActiveModelFile(context)
    }

    fun isRecording(): Boolean = state == VoiceState.LISTENING

    fun toggleRecording() {
        when (state) {
            VoiceState.IDLE -> startRecording()
            VoiceState.LISTENING -> stopAndTranscribe()
            VoiceState.ERROR -> {
                // Recover from error — reset and allow a fresh start
                state = VoiceState.IDLE
                onStateChange.onStateChange(state)
                startRecording()
            }
            VoiceState.TRANSCRIBING -> { /* ignore during active transcription */ }
        }
    }

    private fun startRecording() {
        val mode = getSttMode()
        if (mode == "google") {
            startGoogleStt()
        } else {
            startWhisperRecording()
        }
    }

    /** Start recording for local or cloud whisper modes. */
    private fun startWhisperRecording() {
        state = VoiceState.LISTENING
        onStateChange.onStateChange(state)
        scope.launch {
            recorder.startRecording { e ->
                Log.e(TAG, "Recording error", e)
                scope.launch(Dispatchers.Main) {
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                }
            }
        }
        // Auto-stop after max recording duration (5 minutes)
        mainHandler.postDelayed(maxRecordingRunnable, MAX_RECORDING_DURATION_MS)
    }

    private val maxRecordingRunnable = Runnable {
        if (state == VoiceState.LISTENING && !isGoogleMode()) {
            Log.d(TAG, "Max recording duration reached (${MAX_RECORDING_DURATION_MS / 1000}s), auto-stopping")
            mainHandler.post {
                Toast.makeText(context, "Max recording length reached", Toast.LENGTH_SHORT).show()
            }
            stopAndTranscribe()
        }
    }

    /** Start Google SpeechRecognizer in continuous mode. Must run on main thread. */
    private fun startGoogleStt() {
        // Always create a fresh client to avoid stale isListening state
        googleSttClient?.cancel()
        val client = GoogleSttClient(context).also { googleSttClient = it }

        if (!client.isAvailable()) {
            Log.e(TAG, "Google speech recognition not available on this device")
            state = VoiceState.ERROR
            onStateChange.onStateChange(state)
            return
        }

        // SpeechRecognizer must be started from main thread
        mainHandler.post {
            client.startListening(
                onResult = { text ->
                    // Only called when user explicitly stops via stopAndFinalize()
                    state = VoiceState.IDLE
                    if (text.isNullOrBlank()) {
                        onResult.onResult("")
                    } else {
                        onResult.onResult(text.trim())
                    }
                    onStateChange.onStateChange(state)
                },
                onError = { errorMessage ->
                    Log.e(TAG, "Google STT error: $errorMessage")
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                },
                onListeningStarted = {
                    // State already set to LISTENING before this callback
                },
                onPartial = onPartialResult?.let { callback ->
                    GoogleSttClient.PartialCallback { text -> callback.onPartialResult(text) }
                }
            )
            state = VoiceState.LISTENING
            onStateChange.onStateChange(state)
        }
    }

    private fun stopAndTranscribe() {
        // Cancel the max recording timer
        mainHandler.removeCallbacks(maxRecordingRunnable)

        val mode = getSttMode()
        if (mode == "google") {
            stopGoogleStt()
        } else {
            stopWhisperAndTranscribe()
        }
    }

    /** Stop Google STT — finalizes all accumulated text and delivers via callback. */
    private fun stopGoogleStt() {
        state = VoiceState.TRANSCRIBING
        onStateChange.onStateChange(state)
        mainHandler.post {
            googleSttClient?.stopAndFinalize()
        }
    }

    /** Stop recording and transcribe using local or cloud whisper. */
    private fun stopWhisperAndTranscribe() {
        state = VoiceState.TRANSCRIBING
        onStateChange.onStateChange(state)
        scope.launch {
            try {
                val samples = recorder.stopRecording()
                if (samples.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        // Show "No speech detected" instead of silently swallowing
                        Toast.makeText(context, "No speech detected", Toast.LENGTH_SHORT).show()
                        state = VoiceState.IDLE
                        onStateChange.onStateChange(state)
                    }
                    return@launch
                }

                if (isCloudMode()) {
                    transcribeCloud(samples)
                } else {
                    transcribeLocal(samples)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed", e)
                withContext(Dispatchers.Main) {
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                }
            }
        }
    }

    /** Transcribe using local whisper.cpp model with timeout. */
    private suspend fun transcribeLocal(samples: ShortArray) {
        val floats = FloatArray(samples.size) { samples[it] / 32768.0f }
        try {
            val text = withTimeout(LOCAL_TRANSCRIBE_TIMEOUT_MS) {
                whisperContext?.transcribeData(floats, printTimestamp = false)
            }
            withContext(Dispatchers.Main) {
                if (text.isNullOrBlank()) {
                    onResult.onResult("")
                } else {
                    onResult.onResult(text.trim())
                }
                state = VoiceState.IDLE
                onStateChange.onStateChange(state)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Local transcription timed out after ${LOCAL_TRANSCRIBE_TIMEOUT_MS / 1000}s")
            withContext(Dispatchers.Main) {
                onErrorDetail?.onErrorDetail("Transcription timed out — try a shorter recording")
                state = VoiceState.ERROR
                onStateChange.onStateChange(state)
            }
        }
    }

    /** Transcribe using OpenAI Whisper cloud API with proper error reporting. */
    private suspend fun transcribeCloud(samples: ShortArray) {
        val prefs = context.prefs()
        val apiKey = prefs.getString(Settings.PREF_OPENAI_API_KEY, Defaults.PREF_OPENAI_API_KEY)
        // API key already validated in handleVoiceInput() — but guard just in case
        if (apiKey.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                onErrorDetail?.onErrorDetail("Set your OpenAI API key in Voice & AI settings")
                state = VoiceState.ERROR
                onStateChange.onStateChange(state)
            }
            return
        }

        val result = WhisperCloudClient.transcribe(apiKey, samples)
        withContext(Dispatchers.Main) {
            when (result) {
                is WhisperCloudClient.TranscribeResult.Success -> {
                    onResult.onResult(result.text)
                    state = VoiceState.IDLE
                    onStateChange.onStateChange(state)
                }
                is WhisperCloudClient.TranscribeResult.Empty -> {
                    onResult.onResult("")
                    state = VoiceState.IDLE
                    onStateChange.onStateChange(state)
                }
                is WhisperCloudClient.TranscribeResult.ApiError -> {
                    onErrorDetail?.onErrorDetail(result.message)
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                }
                is WhisperCloudClient.TranscribeResult.NetworkError -> {
                    onErrorDetail?.onErrorDetail(result.message)
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                }
            }
        }
    }

    /** Cycle through STT modes: local → cloud → google → local. Returns the new mode name. */
    fun toggleSttMode(): String {
        val prefs = context.prefs()
        val current = prefs.getString(Settings.PREF_STT_MODE, Defaults.PREF_STT_MODE)
        val newMode = when (current) {
            "local" -> "cloud"
            "cloud" -> "google"
            "google" -> "local"
            else -> "local"
        }
        prefs.edit().putString(Settings.PREF_STT_MODE, newMode).apply()
        Log.d(TAG, "STT mode toggled: $current → $newMode")
        return newMode
    }

    /** Get display label for an STT mode. */
    fun getSttModeLabel(mode: String): String = when (mode) {
        "local" -> "Local STT"
        "cloud" -> "Cloud STT"
        "google" -> "Google STT"
        else -> "Local STT"
    }

    fun release() {
        mainHandler.removeCallbacks(maxRecordingRunnable)
        // Cancel Google STT if active
        googleSttClient?.cancel()
        googleSttClient = null
        // Capture and null out the context first to prevent further use
        val ctx = whisperContext
        whisperContext = null
        recorder.release()
        scope.cancel()
        // Release whisper context on a fresh coroutine — never block the calling thread.
        if (ctx != null) {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    ctx.release()
                } catch (_: Exception) { }
            }
        }
    }
}

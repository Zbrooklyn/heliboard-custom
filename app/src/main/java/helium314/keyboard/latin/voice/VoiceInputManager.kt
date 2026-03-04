package helium314.keyboard.latin.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class VoiceInputManager(
    private val context: Context,
    private val onResult: ResultCallback,
    private val onStateChange: StateCallback
) {
    enum class VoiceState { IDLE, LISTENING, TRANSCRIBING, ERROR }

    fun interface ResultCallback {
        fun onResult(text: String)
    }

    fun interface StateCallback {
        fun onStateChange(state: VoiceState)
    }

    private val recorder = Recorder()
    @Volatile
    private var whisperContext: WhisperContext? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var state = VoiceState.IDLE
    private val mainHandler = Handler(Looper.getMainLooper())

    // Google STT client — lazily initialized
    private var googleSttClient: GoogleSttClient? = null

    val currentState: VoiceState get() = state

    fun loadModel(modelPath: String) {
        scope.launch {
            try {
                Log.d(TAG, "Loading whisper model from: $modelPath")
                whisperContext = WhisperContext.createContextFromFile(modelPath)
                Log.d(TAG, "Whisper model loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load whisper model", e)
                withContext(Dispatchers.Main) {
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
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
            else -> { /* ignore during transcription */ }
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
    }

    /** Start Google SpeechRecognizer — handles its own audio. Must run on main thread. */
    private fun startGoogleStt() {
        val client = googleSttClient ?: GoogleSttClient(context).also { googleSttClient = it }

        if (!client.isAvailable()) {
            Log.e(TAG, "Google speech recognition not available on this device")
            state = VoiceState.ERROR
            onStateChange.onStateChange(state)
            mainHandler.postDelayed({
                state = VoiceState.IDLE
                onStateChange.onStateChange(state)
            }, 2000)
            return
        }

        // SpeechRecognizer must be started from main thread
        mainHandler.post {
            client.startListening(
                onResult = { text ->
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
                    mainHandler.postDelayed({
                        state = VoiceState.IDLE
                        onStateChange.onStateChange(state)
                    }, 2000)
                },
                onListeningStarted = {
                    // State already set to LISTENING before this callback
                }
            )
            state = VoiceState.LISTENING
            onStateChange.onStateChange(state)
        }
    }

    private fun stopAndTranscribe() {
        val mode = getSttMode()
        if (mode == "google") {
            stopGoogleStt()
        } else {
            stopWhisperAndTranscribe()
        }
    }

    /** Stop Google STT — the recognizer will deliver results via its callback. */
    private fun stopGoogleStt() {
        state = VoiceState.TRANSCRIBING
        onStateChange.onStateChange(state)
        mainHandler.post {
            googleSttClient?.stopListening()
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
                        state = VoiceState.IDLE
                        onStateChange.onStateChange(state)
                    }
                    return@launch
                }

                val text = if (isCloudMode()) {
                    transcribeCloud(samples)
                } else {
                    transcribeLocal(samples)
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
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed", e)
                withContext(Dispatchers.Main) {
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                }
                // Show error state for 2s so user can see it, then auto-recover
                delay(2000)
                withContext(Dispatchers.Main) {
                    state = VoiceState.IDLE
                    onStateChange.onStateChange(state)
                }
            }
        }
    }

    /** Transcribe using local whisper.cpp model. */
    private suspend fun transcribeLocal(samples: ShortArray): String? {
        val floats = FloatArray(samples.size) { samples[it] / 32768.0f }
        return whisperContext?.transcribeData(floats, printTimestamp = false)
    }

    /** Transcribe using OpenAI Whisper cloud API. */
    private suspend fun transcribeCloud(samples: ShortArray): String? {
        val prefs = context.prefs()
        val apiKey = prefs.getString(Settings.PREF_OPENAI_API_KEY, Defaults.PREF_OPENAI_API_KEY)
        if (apiKey.isNullOrEmpty()) {
            Log.e(TAG, "Cloud STT: No OpenAI API key configured")
            withContext(Dispatchers.Main) {
                state = VoiceState.ERROR
                onStateChange.onStateChange(state)
            }
            return null
        }
        return WhisperCloudClient.transcribe(apiKey, samples)
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

    companion object {
        private const val TAG = "VoiceInputManager"
    }
}

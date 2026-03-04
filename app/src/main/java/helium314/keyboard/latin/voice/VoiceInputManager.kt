package helium314.keyboard.latin.voice

import android.content.Context
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
import kotlinx.coroutines.runBlocking
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
    private var whisperContext: WhisperContext? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var state = VoiceState.IDLE

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
        scope.launch {
            try {
                whisperContext?.release()
                whisperContext = null
                Log.d(TAG, "Whisper model unloaded to save memory")
            } catch (e: Exception) {
                Log.e(TAG, "Error unloading model", e)
                whisperContext = null
            }
        }
    }

    /** Returns true if cloud STT mode is selected and an OpenAI API key is configured. */
    fun isCloudMode(): Boolean {
        val prefs = context.prefs()
        val sttMode = prefs.getString(Settings.PREF_STT_MODE, Defaults.PREF_STT_MODE)
        return sttMode == "cloud"
    }

    /** Cloud mode doesn't need a local model — only needs an API key. */
    fun needsLocalModel(): Boolean = !isCloudMode()

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

    private fun stopAndTranscribe() {
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

    /** Toggle between local and cloud STT mode. Returns the new mode name. */
    fun toggleSttMode(): String {
        val prefs = context.prefs()
        val current = prefs.getString(Settings.PREF_STT_MODE, Defaults.PREF_STT_MODE)
        val newMode = if (current == "cloud") "local" else "cloud"
        prefs.edit().putString(Settings.PREF_STT_MODE, newMode).apply()
        Log.d(TAG, "STT mode toggled: $current → $newMode")
        return newMode
    }

    fun release() {
        // Launch release on whisper's own scope, then cancel ours
        val ctx = whisperContext
        whisperContext = null
        if (ctx != null) {
            try {
                runBlocking { ctx.release() }
            } catch (_: Exception) { }
        }
        scope.cancel()
        recorder.release()
    }

    companion object {
        private const val TAG = "VoiceInputManager"
    }
}

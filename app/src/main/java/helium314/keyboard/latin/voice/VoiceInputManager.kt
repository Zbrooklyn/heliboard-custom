package helium314.keyboard.latin.voice

import android.content.Context
import android.util.Log
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

    fun getModelFile(): File? {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return null
        // Return the first .bin model file found
        return modelsDir.listFiles()
            ?.filter { it.name.endsWith(".bin") && !it.name.endsWith(".tmp") }
            ?.firstOrNull()
    }

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
                state = VoiceState.ERROR
                onStateChange.onStateChange(state)
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
                val floats = FloatArray(samples.size) { samples[it] / 32768.0f }
                val text = whisperContext?.transcribeData(floats, printTimestamp = false) ?: ""
                withContext(Dispatchers.Main) {
                    onResult.onResult(text.trim())
                    state = VoiceState.IDLE
                    onStateChange.onStateChange(state)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed", e)
                withContext(Dispatchers.Main) {
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                    // Auto-recover to IDLE after error
                    state = VoiceState.IDLE
                    onStateChange.onStateChange(state)
                }
            }
        }
    }

    fun release() {
        scope.cancel()
        recorder.release()
        runBlocking {
            try {
                whisperContext?.release()
            } catch (_: Exception) { }
        }
    }

    companion object {
        private const val TAG = "VoiceInputManager"
    }
}

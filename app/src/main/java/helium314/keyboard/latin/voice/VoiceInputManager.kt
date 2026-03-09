package helium314.keyboard.latin.voice

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.whispercpp.whisper.WhisperContext
import helium314.keyboard.latin.ai.WhisperCloudClient
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SecurePrefs
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
    @Volatile
    private var state = VoiceState.IDLE
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    // Google STT client — lazily initialized
    private var googleSttClient: GoogleSttClient? = null

    /** Optional callback for detailed error messages (set by LatinIME). */
    var onErrorDetail: ErrorDetailCallback? = null

    val currentState: VoiceState get() = state

    companion object {
        private const val TAG = "VoiceInputManager"
        /** Max recording duration in milliseconds (5 minutes). */
        private const val MAX_RECORDING_DURATION_MS = 5 * 60 * 1000L
        /** Max local transcription timeout in milliseconds (3 minutes).
         *  Some devices need 30x real-time for whisper.cpp inference. */
        private const val LOCAL_TRANSCRIBE_TIMEOUT_MS = 180_000L

        /** Known Whisper hallucination phrases — filter these from local transcription results. */
        private val HALLUCINATION_PHRASES = setOf(
            "thank you for watching",
            "thanks for watching",
            "thank you for listening",
            "thanks for listening",
            "please subscribe",
            "like and subscribe",
            "subscribe",
            "the end",
            "bye",
            "bye bye",
            "goodbye",
            "you",
            "...",
            "subtitles by",
            "translated by",
            "amara.org"
        )

        /** Returns true if the text is a known Whisper hallucination. */
        private fun isHallucination(text: String): Boolean {
            val normalized = text.trim().lowercase()
                .replace(Regex("[.!?,;:\\s]+$"), "") // strip trailing punctuation
                .trim()
            return normalized.isEmpty() || HALLUCINATION_PHRASES.contains(normalized)
        }
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

    /** Cancel any active recording without transcribing. Used when keyboard dismisses. */
    fun cancelRecording() {
        if (state == VoiceState.LISTENING) {
            mainHandler.removeCallbacks(maxRecordingRunnable)
            abandonAudioFocus()
            releaseWakeLock()
            scope.launch {
                recorder.stopRecording() // discard samples
            }
            state = VoiceState.IDLE
            onStateChange.onStateChange(state)
        } else if (state == VoiceState.TRANSCRIBING) {
            // Can't cancel whisper mid-transcription — just reset state.
            // The transcription result will be discarded when it arrives
            // because the InputConnection is already gone.
            state = VoiceState.IDLE
            onStateChange.onStateChange(state)
        }
        // Cancel Google STT if active
        googleSttClient?.cancel()
        googleSttClient = null
    }

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
        requestAudioFocus()
        acquireWakeLock()
        state = VoiceState.LISTENING
        onStateChange.onStateChange(state)
        scope.launch {
            recorder.startRecording { e ->
                Log.e(TAG, "Recording error", e)
                abandonAudioFocus()
                releaseWakeLock()
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
        // Do NOT request audio focus for Google STT — SpeechRecognizer manages
        // its own audio focus internally. Requesting it ourselves causes a conflict:
        // Google grabs focus → our listener fires AUDIOFOCUS_LOSS → we auto-stop.
        acquireWakeLock()
        // Always create a fresh client to avoid stale isListening state
        googleSttClient?.cancel()
        val client = GoogleSttClient(context).also { googleSttClient = it }

        if (!client.isAvailable()) {
            Log.e(TAG, "Google speech recognition not available on this device")
            releaseWakeLock()
            state = VoiceState.ERROR
            onStateChange.onStateChange(state)
            return
        }

        // Get the current keyboard language so Google STT uses it instead of device default
        val currentLocale = try {
            helium314.keyboard.latin.RichInputMethodManager.getInstance().currentSubtypeLocale
        } catch (_: Exception) { null }

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
                    releaseWakeLock()
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                },
                onListeningStarted = {
                    // State already set to LISTENING before this callback
                },
                onPartial = onPartialResult?.let { callback ->
                    GoogleSttClient.PartialCallback { text -> callback.onPartialResult(text) }
                },
                locale = currentLocale
            )
            state = VoiceState.LISTENING
            onStateChange.onStateChange(state)
        }
    }

    // Note: In split-screen mode, if the user switches focus during recording,
    // getCurrentInputConnection() in LatinIME may return the connection for the
    // newly focused window. The transcription result could be committed to the
    // wrong window. This is an Android framework limitation — no reliable fix
    // exists without tracking the original InputConnection at recording start.
    private fun stopAndTranscribe() {
        // Release audio focus now that recording is stopping
        abandonAudioFocus()
        releaseWakeLock()
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

                // Guard against very short recordings — Whisper hallucinates on < 0.5s audio
                val MIN_SAMPLES = 8000 // 0.5 seconds at 16kHz
                if (samples.size < MIN_SAMPLES) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Recording too short", Toast.LENGTH_SHORT).show()
                        state = VoiceState.IDLE
                        onStateChange.onStateChange(state)
                    }
                    return@launch
                }

                // Check audio energy — reject near-silent recordings (likely no speech)
                var sumSquares = 0.0
                for (s in samples) {
                    sumSquares += s.toDouble() * s.toDouble()
                }
                val rmsEnergy = kotlin.math.sqrt(sumSquares / samples.size)
                Log.d(TAG, "RMS energy: ${"%.1f".format(rmsEnergy)} (${samples.size} samples)")
                if (rmsEnergy < 50.0) { // ~50 RMS threshold — low to avoid rejecting quiet speech
                    withContext(Dispatchers.Main) {
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
        // Capture whisperContext in a local val before use — another thread could null
        // it out between the null-check and the method call.
        val ctx = whisperContext ?: run {
            withContext(Dispatchers.Main) {
                onErrorDetail?.onErrorDetail("Voice model not loaded")
                state = VoiceState.ERROR
                onStateChange.onStateChange(state)
            }
            return
        }
        try {
            val text = withTimeout(LOCAL_TRANSCRIBE_TIMEOUT_MS) {
                ctx.transcribeData(floats, printTimestamp = false)
            }
            withContext(Dispatchers.Main) {
                if (text.isNullOrBlank() || isHallucination(text)) {
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
        val securePrefs = SecurePrefs.get(context)
        val apiKey = securePrefs.getString(Settings.PREF_OPENAI_API_KEY, Defaults.PREF_OPENAI_API_KEY)
        // API key already validated in handleVoiceInput() — but guard just in case
        if (apiKey.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                onErrorDetail?.onErrorDetail("Set your OpenAI API key in Voice & AI settings")
                state = VoiceState.ERROR
                onStateChange.onStateChange(state)
            }
            return
        }

        // Block cloud STT in privacy-sensitive contexts
        val ims = context as? InputMethodService
        val ei = ims?.currentInputEditorInfo
        if (ei != null) {
            val variation = ei.inputType and InputType.TYPE_MASK_VARIATION
            val isPassword = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            val noLearning = (ei.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0

            if (isPassword || noLearning) {
                Log.d(TAG, "Cloud STT blocked — sensitive field (password=$isPassword, noLearning=$noLearning)")
                withContext(Dispatchers.Main) {
                    onErrorDetail?.onErrorDetail("Cloud STT disabled for sensitive fields — switch to Local mode")
                    state = VoiceState.ERROR
                    onStateChange.onStateChange(state)
                }
                return
            }
        }

        // Check incognito mode
        if (prefs.getBoolean(Settings.PREF_ALWAYS_INCOGNITO_MODE, Defaults.PREF_ALWAYS_INCOGNITO_MODE)) {
            Log.d(TAG, "Cloud STT blocked — incognito mode active")
            withContext(Dispatchers.Main) {
                onErrorDetail?.onErrorDetail("Cloud STT disabled in incognito mode — switch to Local mode")
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

    private fun requestAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val request = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        // Another app grabbed audio focus — stop recording
                        if (state == VoiceState.LISTENING) {
                            mainHandler.post { stopAndTranscribe() }
                        }
                    }
                }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        if (state == VoiceState.LISTENING) {
                            mainHandler.post { stopAndTranscribe() }
                        }
                    }
                },
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun abandonAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    @android.annotation.SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "WhisperClick:VoiceRecording")
            wakeLock?.acquire()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release wake lock", e)
        }
    }

    fun release() {
        abandonAudioFocus()
        releaseWakeLock()
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

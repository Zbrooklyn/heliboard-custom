// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whispercpp.whisper.WhisperContext
import com.whispercpp.whisper.WhisperCpuConfig
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.ai.ApiKeyValidator
import helium314.keyboard.latin.voice.ActivityLog
import helium314.keyboard.latin.voice.ModelManager
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.SettingsWithoutKey
import helium314.keyboard.settings.preferences.ListPreference
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.TextInputPreference

@Composable
fun VoiceAIScreen(onClickBack: () -> Unit) {
    val prefs = LocalContext.current.prefs()
    // Force recomposition when prefs change
    val b = (LocalContext.current as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")

    val items = listOf(
        // Voice Input section
        R.string.voice_ai_category_voice,
        Settings.PREF_STT_MODE,
        Settings.PREF_AI_ACTIVE_MODEL,
        // AI Rewrite section
        R.string.voice_ai_category_rewrite,
        Settings.PREF_AI_PROVIDER,
        Settings.PREF_GEMINI_API_KEY,
        Settings.PREF_OPENAI_API_KEY,
        SettingsWithoutKey.TEST_API_KEY,
        // Diagnostics section
        R.string.voice_ai_category_diagnostics,
        SettingsWithoutKey.VOICE_BENCHMARK,
        SettingsWithoutKey.ACTIVITY_LOG,
    )

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_voice_ai),
        settings = items,
    )
}

fun createVoiceAISettings(context: Context) = listOf(
    Setting(context, Settings.PREF_STT_MODE, R.string.voice_ai_stt_mode, R.string.voice_ai_stt_mode_summary) {
        ListPreference(
            setting = it,
            items = listOf(
                context.getString(R.string.voice_ai_stt_local) to "local",
                context.getString(R.string.voice_ai_stt_cloud) to "cloud",
            ),
            default = Defaults.PREF_STT_MODE,
        )
    },
    Setting(context, Settings.PREF_AI_ACTIVE_MODEL, R.string.voice_ai_active_model, R.string.voice_ai_active_model_summary) {
        val models = helium314.keyboard.latin.voice.ModelManager.availableModels
        ListPreference(
            setting = it,
            items = models.map { m -> "${m.name} (${m.sizeMb}MB)" to m.fileName },
            default = helium314.keyboard.latin.voice.ModelManager.defaultModel.fileName,
        )
    },
    Setting(context, Settings.PREF_AI_PROVIDER, R.string.voice_ai_provider, R.string.voice_ai_provider_summary) {
        ListPreference(
            setting = it,
            items = listOf(
                "Gemini (Google)" to "gemini",
                "OpenAI (GPT)" to "openai",
            ),
            default = Defaults.PREF_AI_PROVIDER,
        )
    },
    Setting(context, Settings.PREF_GEMINI_API_KEY, R.string.voice_ai_gemini_key) {
        TextInputPreference(setting = it, default = Defaults.PREF_GEMINI_API_KEY)
    },
    Setting(context, Settings.PREF_OPENAI_API_KEY, R.string.voice_ai_openai_key) {
        TextInputPreference(setting = it, default = Defaults.PREF_OPENAI_API_KEY)
    },
    Setting(context, SettingsWithoutKey.TEST_API_KEY, R.string.voice_ai_test_api_key) {
        TestApiKeyPreference()
    },
    Setting(context, SettingsWithoutKey.VOICE_BENCHMARK, R.string.voice_ai_benchmark) {
        VoiceBenchmarkPreference()
    },
    Setting(context, SettingsWithoutKey.ACTIVITY_LOG, R.string.voice_ai_activity_log) {
        ActivityLogPreference()
    },
)

private fun copyToClipboard(context: Context, label: String, text: String, toastRes: Int) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, toastRes, Toast.LENGTH_SHORT).show()
}

@Composable
private fun VoiceBenchmarkPreference() {
    val context = LocalContext.current
    var benchmarkResult by remember { mutableStateOf<String?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    Column {
        Preference(
            name = stringResource(R.string.voice_ai_benchmark),
            description = if (isRunning) stringResource(R.string.voice_ai_benchmark_running)
                else stringResource(R.string.voice_ai_benchmark_description),
            onClick = {
                if (isRunning) return@Preference
                isRunning = true
                benchmarkResult = null
                Thread {
                    val result = runVoiceBenchmark(context)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        benchmarkResult = result
                        isRunning = false
                    }
                }.start()
            }
        )
        if (benchmarkResult != null) {
            Text(
                text = benchmarkResult!!,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    copyToClipboard(context, "Benchmark", benchmarkResult!!, R.string.voice_ai_benchmark_copied)
                }) {
                    Text(stringResource(R.string.voice_ai_benchmark_copy))
                }
                TextButton(onClick = {
                    shareText(context, "WhisperClick Benchmark", benchmarkResult!!)
                }) {
                    Text(stringResource(R.string.voice_ai_share))
                }
            }
        }
    }
}

@Composable
private fun ActivityLogPreference() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }

    Column {
        Preference(
            name = stringResource(R.string.voice_ai_activity_log),
            description = stringResource(R.string.voice_ai_activity_log_description),
            onClick = {
                logText = ActivityLog.getLog()
                expanded = !expanded
            }
        )
        if (expanded) {
            if (logText.isEmpty()) {
                Text(
                    text = stringResource(R.string.voice_ai_log_empty),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = logText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    if (logText.isNotEmpty()) {
                        copyToClipboard(context, "Activity Log", logText, R.string.voice_ai_log_copied)
                    }
                }) {
                    Text(stringResource(R.string.voice_ai_log_copy))
                }
                TextButton(onClick = {
                    if (logText.isNotEmpty()) {
                        shareText(context, "WhisperClick Activity Log", logText)
                    }
                }) {
                    Text(stringResource(R.string.voice_ai_share))
                }
                TextButton(onClick = {
                    ActivityLog.clear()
                    logText = ""
                    Toast.makeText(context, R.string.voice_ai_log_cleared, Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.voice_ai_log_clear))
                }
            }
        }
    }
}

private fun shareText(context: Context, subject: String, text: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(intent, subject))
}

@Composable
private fun TestApiKeyPreference() {
    val context = LocalContext.current
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Column {
        Preference(
            name = stringResource(R.string.voice_ai_test_api_key),
            description = when {
                isTesting -> stringResource(R.string.voice_ai_test_api_key_testing)
                testResult != null -> testResult!!
                else -> stringResource(R.string.voice_ai_test_api_key_description)
            },
            onClick = {
                if (isTesting) return@Preference
                isTesting = true
                testResult = null
                val prefs = context.prefs()
                val provider = prefs.getString(Settings.PREF_AI_PROVIDER, Defaults.PREF_AI_PROVIDER) ?: "gemini"
                val apiKey = if (provider == "openai")
                    prefs.getString(Settings.PREF_OPENAI_API_KEY, "") ?: ""
                else
                    prefs.getString(Settings.PREF_GEMINI_API_KEY, "") ?: ""

                Thread {
                    val result = kotlinx.coroutines.runBlocking {
                        if (provider == "openai") ApiKeyValidator.validateOpenAI(apiKey)
                        else ApiKeyValidator.validateGemini(apiKey)
                    }
                    val message = when (result) {
                        is ApiKeyValidator.Result.Valid -> context.getString(R.string.voice_ai_test_api_key_valid)
                        is ApiKeyValidator.Result.Invalid -> result.message
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        testResult = message
                        isTesting = false
                    }
                }.start()
            }
        )
    }
}

private fun runVoiceBenchmark(context: Context): String {
    val sb = StringBuilder()
    val threadCount = WhisperCpuConfig.preferredThreadCount

    // System info
    try {
        val sysInfo = WhisperContext.getSystemInfo()
        sb.appendLine("=== System Info ===")
        sb.appendLine(sysInfo)
    } catch (e: Exception) {
        sb.appendLine("System info: ${e.message}")
    }

    sb.appendLine()
    sb.appendLine("=== CPU Config ===")
    sb.appendLine("Threads: $threadCount")
    sb.appendLine("Processors: ${Runtime.getRuntime().availableProcessors()}")
    sb.appendLine("ABI: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")

    // Model info
    sb.appendLine()
    sb.appendLine("=== Model ===")
    val activeModel = ModelManager.getActiveModelName(context)
    val modelFile = ModelManager.getActiveModelFile(context)
    sb.appendLine("Active: $activeModel")
    if (modelFile != null && modelFile.exists()) {
        sb.appendLine("Size: ${modelFile.length() / 1024 / 1024}MB")
    } else {
        sb.appendLine("Status: Not downloaded")
    }

    // Downloaded models
    val downloaded = ModelManager.getDownloadedModels(context)
    sb.appendLine("Downloaded: ${downloaded.joinToString()}")

    // Memory info
    sb.appendLine()
    sb.appendLine("=== Memory ===")
    val rt = Runtime.getRuntime()
    sb.appendLine("Heap max: ${rt.maxMemory() / 1024 / 1024}MB")
    sb.appendLine("Heap used: ${(rt.totalMemory() - rt.freeMemory()) / 1024 / 1024}MB")

    // Native benchmarks (only if model is loaded)
    if (modelFile != null && modelFile.exists()) {
        sb.appendLine()
        sb.appendLine("=== Inference Benchmark ===")
        try {
            // Load model, run benchmark, release
            val startLoad = System.currentTimeMillis()
            val whisperCtx = WhisperContext.createContextFromFile(modelFile.absolutePath)
            val loadTime = System.currentTimeMillis() - startLoad
            sb.appendLine("Model load: ${loadTime}ms")

            // Transcribe 3 seconds of silence to measure baseline overhead
            val silentAudio = FloatArray(16000 * 3) // 3s at 16kHz
            val startTranscribe = System.currentTimeMillis()
            kotlinx.coroutines.runBlocking {
                whisperCtx.transcribeData(silentAudio, numThreads = threadCount)
            }
            val transcribeTime = System.currentTimeMillis() - startTranscribe
            sb.appendLine("3s silence transcribe: ${transcribeTime}ms")
            sb.appendLine("Real-time factor: ${"%.2f".format(transcribeTime / 3000.0)}x")

            // Release
            kotlinx.coroutines.runBlocking { whisperCtx.release() }
        } catch (e: Exception) {
            sb.appendLine("Benchmark error: ${e.message}")
        }
    }

    // API usage stats
    sb.appendLine()
    sb.appendLine("=== API Usage (this session) ===")
    sb.appendLine(ActivityLog.getUsageStats())

    return sb.toString()
}

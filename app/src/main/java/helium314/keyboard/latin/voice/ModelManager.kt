// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.util.Log
import helium314.keyboard.latin.utils.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class WhisperModel(
    val name: String,
    val fileName: String,
    val url: String,
    val sizeMb: Int,
    val tier: String  // "Tiny", "Base", "Small", "Medium"
)

data class DownloadProgress(
    val model: WhisperModel? = null,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val isDownloading: Boolean = false,
)

/**
 * Manages whisper.cpp models: 7 available models, download with resume/cancel/retry,
 * delete, and active model selection.
 */
object ModelManager {
    private const val TAG = "ModelManager"
    private const val HF_BASE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"
    private const val PREF_ACTIVE_MODEL = "active_voice_model"
    private const val MAX_RETRIES = 3
    private val RETRY_DELAYS_MS = longArrayOf(2000, 4000, 8000)

    // Q5_1 quantized models — 60% smaller, faster inference, near-identical accuracy
    // Large-v3-turbo: 6x faster than Large-v3, near-Medium accuracy, 4-layer decoder
    val availableModels = listOf(
        WhisperModel("Tiny English (Q5)", "ggml-tiny.en-q5_1.bin", "$HF_BASE/ggml-tiny.en-q5_1.bin", 31, "Tiny"),
        WhisperModel("Base English (Q5)", "ggml-base.en-q5_1.bin", "$HF_BASE/ggml-base.en-q5_1.bin", 57, "Base"),
        WhisperModel("Small English (Q5)", "ggml-small.en-q5_1.bin", "$HF_BASE/ggml-small.en-q5_1.bin", 181, "Small"),
        WhisperModel("Medium English (Q5)", "ggml-medium.en-q5_1.bin", "$HF_BASE/ggml-medium.en-q5_1.bin", 514, "Medium"),
        WhisperModel("Large v3 Turbo (Q5)", "ggml-large-v3-turbo-q5_0.bin", "$HF_BASE/ggml-large-v3-turbo-q5_0.bin", 574, "Large"),
        WhisperModel("Tiny Multilingual (Q5)", "ggml-tiny-q5_1.bin", "$HF_BASE/ggml-tiny-q5_1.bin", 31, "Tiny"),
        WhisperModel("Base Multilingual (Q5)", "ggml-base-q5_1.bin", "$HF_BASE/ggml-base-q5_1.bin", 57, "Base"),
        WhisperModel("Small Multilingual (Q5)", "ggml-small-q5_1.bin", "$HF_BASE/ggml-small-q5_1.bin", 181, "Small"),
    )

    /** Default model — downloaded automatically on first voice use. */
    val defaultModel = availableModels[0] // Tiny English

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress

    @Volatile
    private var cancelRequested = false

    fun getModelsDir(context: Context): File {
        return File(context.filesDir, "models").also { it.mkdirs() }
    }

    fun getDownloadedModels(context: Context): List<String> {
        val dir = getModelsDir(context)
        return dir.listFiles()
            ?.filter { it.name.endsWith(".bin") && !it.name.endsWith(".tmp") }
            ?.map { it.name }
            ?: emptyList()
    }

    fun isModelDownloaded(context: Context, model: WhisperModel): Boolean {
        return File(getModelsDir(context), model.fileName).exists()
    }

    fun getActiveModelName(context: Context): String {
        return context.prefs().getString(PREF_ACTIVE_MODEL, defaultModel.fileName) ?: defaultModel.fileName
    }

    fun setActiveModel(context: Context, modelFileName: String) {
        context.prefs().edit().putString(PREF_ACTIVE_MODEL, modelFileName).apply()
    }

    /** Get the active model file, or the default model file, or any downloaded model. */
    fun getActiveModelFile(context: Context): File? {
        val activeFileName = getActiveModelName(context)
        val activeFile = File(getModelsDir(context), activeFileName)
        if (activeFile.exists()) return activeFile

        // Fallback: any downloaded .bin model
        return getModelsDir(context).listFiles()
            ?.filter { it.name.endsWith(".bin") && !it.name.endsWith(".tmp") }
            ?.firstOrNull()
    }

    fun deleteModel(context: Context, fileName: String): Boolean {
        val file = File(getModelsDir(context), fileName)
        val deleted = if (file.exists()) file.delete() else false
        // Also clean up temp file
        val tmpFile = File(getModelsDir(context), "$fileName.tmp")
        if (tmpFile.exists()) tmpFile.delete()
        // If we deleted the active model, reset to default
        if (deleted && getActiveModelName(context) == fileName) {
            setActiveModel(context, defaultModel.fileName)
        }
        return deleted
    }

    fun cancelDownload() {
        cancelRequested = true
    }

    /**
     * Download a model with resume support, retry with exponential backoff,
     * and proper HTTP connection cleanup.
     */
    suspend fun downloadModel(context: Context, model: WhisperModel): Boolean = withContext(Dispatchers.IO) {
        cancelRequested = false
        _progress.value = DownloadProgress(model, 0, 0, true)
        Log.d(TAG, "Downloading ${model.name} (${model.sizeMb}MB)...")

        val destFile = File(getModelsDir(context), model.fileName)
        val tempFile = File(getModelsDir(context), "${model.fileName}.tmp")

        for (attempt in 0 until MAX_RETRIES) {
            if (cancelRequested) {
                _progress.value = DownloadProgress()
                return@withContext false
            }

            var connection: HttpURLConnection? = null
            try {
                val url = URL(model.url)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                // Resume support
                if (tempFile.exists() && tempFile.length() > 0) {
                    connection.setRequestProperty("Range", "bytes=${tempFile.length()}-")
                }

                connection.connect()
                val responseCode = connection.responseCode

                val totalBytes = if (responseCode == 206) {
                    val range = connection.getHeaderField("Content-Range") ?: ""
                    range.substringAfter("/", "").toLongOrNull()
                        ?: (tempFile.length() + connection.contentLength.toLong())
                } else {
                    if (tempFile.exists()) tempFile.delete()
                    connection.contentLength.toLong()
                }

                _progress.value = DownloadProgress(model, if (responseCode == 206) tempFile.length() else 0, totalBytes, true)

                val input = connection.inputStream
                val output = if (responseCode == 206) {
                    java.io.FileOutputStream(tempFile, true)
                } else {
                    tempFile.outputStream()
                }

                val buffer = ByteArray(8192)
                var downloaded = if (responseCode == 206) tempFile.length() else 0L
                output.use { out ->
                    while (true) {
                        if (cancelRequested) {
                            Log.d(TAG, "Download cancelled")
                            _progress.value = DownloadProgress()
                            return@withContext false
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        _progress.value = DownloadProgress(model, downloaded, totalBytes, true)
                    }
                }
                input.close()

                if (!tempFile.renameTo(destFile)) {
                    throw IOException("Failed to move downloaded file to ${destFile.absolutePath}")
                }
                _progress.value = DownloadProgress()
                Log.d(TAG, "${model.name} downloaded successfully")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Download attempt ${attempt + 1}/$MAX_RETRIES failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    val delay = RETRY_DELAYS_MS[attempt]
                    Log.d(TAG, "Retrying in ${delay}ms...")
                    kotlinx.coroutines.delay(delay)
                } else {
                    Log.e(TAG, "All $MAX_RETRIES download attempts failed for ${model.name}")
                    _progress.value = DownloadProgress()
                    return@withContext false
                }
            } finally {
                connection?.disconnect()
            }
        }
        _progress.value = DownloadProgress()
        false
    }

    /**
     * Blocking download for Java interop — downloads the default model.
     * Call from a background thread only.
     */
    @JvmStatic
    fun downloadDefaultModelBlocking(context: Context, onProgress: (Int) -> Unit): Boolean {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            cancelRequested = false
            val model = defaultModel
            val destFile = File(getModelsDir(context), model.fileName)
            val tempFile = File(getModelsDir(context), "${model.fileName}.tmp")

            for (attempt in 0 until MAX_RETRIES) {
                if (cancelRequested) return@runBlocking false

                var connection: HttpURLConnection? = null
                try {
                    val url = URL(model.url)
                    connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000

                    if (tempFile.exists() && tempFile.length() > 0) {
                        connection.setRequestProperty("Range", "bytes=${tempFile.length()}-")
                    }

                    connection.connect()
                    val responseCode = connection.responseCode

                    val totalBytes = if (responseCode == 206) {
                        val range = connection.getHeaderField("Content-Range") ?: ""
                        range.substringAfter("/", "").toLongOrNull()
                            ?: (tempFile.length() + connection.contentLength.toLong())
                    } else {
                        if (tempFile.exists()) tempFile.delete()
                        connection.contentLength.toLong()
                    }

                    val input = connection.inputStream
                    val output = if (responseCode == 206) {
                        java.io.FileOutputStream(tempFile, true)
                    } else {
                        tempFile.outputStream()
                    }

                    val buffer = ByteArray(8192)
                    var downloaded = if (responseCode == 206) tempFile.length() else 0L
                    output.use { out ->
                        while (true) {
                            if (cancelRequested) return@runBlocking false
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                onProgress(((downloaded * 100) / totalBytes).toInt())
                            }
                        }
                    }
                    input.close()

                    if (!tempFile.renameTo(destFile)) {
                        throw IOException("Failed to rename temp file")
                    }
                    return@runBlocking true

                } catch (e: Exception) {
                    Log.e(TAG, "Blocking download attempt ${attempt + 1}/$MAX_RETRIES failed: ${e.message}")
                    if (attempt < MAX_RETRIES - 1) {
                        Thread.sleep(RETRY_DELAYS_MS[attempt])
                    }
                } finally {
                    connection?.disconnect()
                }
            }
            false
        }
    }
}

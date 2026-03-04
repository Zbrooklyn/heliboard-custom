package helium314.keyboard.latin.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {
    private const val TAG = "ModelDownloader"
    private const val HF_BASE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"

    // Default model: Tiny English Q5_1 — 31MB, fastest inference
    const val DEFAULT_MODEL_NAME = "ggml-tiny.en-q5_1.bin"
    private const val DEFAULT_MODEL_URL = "$HF_BASE/$DEFAULT_MODEL_NAME"

    private const val MAX_RETRIES = 3
    private val RETRY_DELAYS_MS = longArrayOf(2000, 4000, 8000)

    @Volatile
    private var cancelRequested = false

    fun getModelsDir(context: Context): File {
        return File(context.filesDir, "models").also { it.mkdirs() }
    }

    fun getDefaultModelFile(context: Context): File {
        return File(getModelsDir(context), DEFAULT_MODEL_NAME)
    }

    @JvmStatic
    fun isModelDownloaded(context: Context): Boolean {
        return getDefaultModelFile(context).exists()
    }

    fun cancelDownload() {
        cancelRequested = true
    }

    /**
     * Callback for download progress percentage.
     * Java-friendly alternative to kotlin lambda.
     */
    fun interface ProgressCallback {
        fun onProgress(percent: Int)
    }

    /**
     * Blocking download method for easy Java interop.
     * Must be called from a background thread.
     * Retries up to 3 times with exponential backoff (2s/4s/8s).
     */
    @JvmStatic
    fun downloadDefaultModelBlocking(context: Context, onProgressPercent: ProgressCallback?): Boolean {
        cancelRequested = false
        Log.d(TAG, "Downloading default model (blocking): $DEFAULT_MODEL_NAME")

        val destFile = getDefaultModelFile(context)
        val tempFile = File(getModelsDir(context), "$DEFAULT_MODEL_NAME.tmp")

        for (attempt in 0..MAX_RETRIES) {
            if (cancelRequested) return false
            if (attempt > 0) {
                Log.d(TAG, "Retry attempt $attempt/$MAX_RETRIES after ${RETRY_DELAYS_MS[attempt - 1]}ms")
                Thread.sleep(RETRY_DELAYS_MS[attempt - 1])
            }

            var connection: HttpURLConnection? = null
            try {
                val url = URL(DEFAULT_MODEL_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                if (tempFile.exists()) {
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
                var lastReportedPct = -1
                output.use { out ->
                    while (true) {
                        if (cancelRequested) {
                            Log.d(TAG, "Download cancelled")
                            return@use
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val pct = (downloaded * 100 / totalBytes).toInt()
                            if (pct != lastReportedPct) {
                                lastReportedPct = pct
                                onProgressPercent?.onProgress(pct)
                            }
                        }
                    }
                }
                input.close()

                if (cancelRequested) return false

                if (!tempFile.renameTo(destFile)) {
                    throw java.io.IOException("Failed to rename temp file")
                }
                Log.d(TAG, "Model downloaded successfully")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Download attempt ${attempt + 1} failed: ${e.message}")
                if (attempt == MAX_RETRIES) {
                    Log.e(TAG, "All download attempts exhausted")
                    return false
                }
            } finally {
                connection?.disconnect()
            }
        }
        return false
    }

    /**
     * Suspend download with retry support.
     * Retries up to 3 times with exponential backoff (2s/4s/8s).
     */
    suspend fun downloadDefaultModel(
        context: Context,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        cancelRequested = false
        onProgress(DownloadProgress(bytesDownloaded = 0, totalBytes = 0, isDownloading = true))
        Log.d(TAG, "Downloading default model: $DEFAULT_MODEL_NAME")

        val destFile = getDefaultModelFile(context)
        val tempFile = File(getModelsDir(context), "$DEFAULT_MODEL_NAME.tmp")

        for (attempt in 0..MAX_RETRIES) {
            if (cancelRequested) {
                onProgress(DownloadProgress())
                return@withContext false
            }
            if (attempt > 0) {
                Log.d(TAG, "Retry attempt $attempt/$MAX_RETRIES after ${RETRY_DELAYS_MS[attempt - 1]}ms")
                delay(RETRY_DELAYS_MS[attempt - 1])
            }

            var connection: HttpURLConnection? = null
            try {
                val url = URL(DEFAULT_MODEL_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                // Resume support
                if (tempFile.exists()) {
                    connection.setRequestProperty("Range", "bytes=${tempFile.length()}-")
                }

                connection.connect()
                val responseCode = connection.responseCode

                val totalBytes = if (responseCode == 206) {
                    val range = connection.getHeaderField("Content-Range") ?: ""
                    val total = range.substringAfter("/", "").toLongOrNull()
                        ?: (tempFile.length() + connection.contentLength.toLong())
                    total
                } else {
                    if (tempFile.exists()) tempFile.delete()
                    connection.contentLength.toLong()
                }

                onProgress(DownloadProgress(bytesDownloaded = tempFile.length(), totalBytes = totalBytes, isDownloading = true))

                val input = connection.inputStream
                val output = if (responseCode == 206) {
                    java.io.FileOutputStream(tempFile, true)
                } else {
                    tempFile.outputStream()
                }

                val buffer = ByteArray(8192)
                var downloaded = tempFile.length()
                output.use { out ->
                    while (true) {
                        if (cancelRequested) {
                            Log.d(TAG, "Download cancelled")
                            onProgress(DownloadProgress())
                            return@withContext false
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        onProgress(DownloadProgress(bytesDownloaded = downloaded, totalBytes = totalBytes, isDownloading = true))
                    }
                }
                input.close()

                if (!tempFile.renameTo(destFile)) {
                    throw java.io.IOException("Failed to move downloaded file to ${destFile.absolutePath}")
                }
                onProgress(DownloadProgress())
                Log.d(TAG, "Model downloaded successfully")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Download attempt ${attempt + 1} failed: ${e.message}")
                if (attempt == MAX_RETRIES) {
                    Log.e(TAG, "All download attempts exhausted")
                    onProgress(DownloadProgress())
                    return@withContext false
                }
            } finally {
                connection?.disconnect()
            }
        }
        onProgress(DownloadProgress())
        false
    }
}

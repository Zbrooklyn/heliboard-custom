package helium314.keyboard.latin.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class DownloadProgress(
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val isDownloading: Boolean = false
)

object ModelDownloader {
    private const val TAG = "ModelDownloader"
    private const val HF_BASE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"

    // Default model: Tiny English Q5_1 — 31MB, fastest inference
    const val DEFAULT_MODEL_NAME = "ggml-tiny.en-q5_1.bin"
    private const val DEFAULT_MODEL_URL = "$HF_BASE/$DEFAULT_MODEL_NAME"

    @Volatile
    private var cancelRequested = false

    fun getModelsDir(context: Context): File {
        return File(context.filesDir, "models").also { it.mkdirs() }
    }

    fun getDefaultModelFile(context: Context): File {
        return File(getModelsDir(context), DEFAULT_MODEL_NAME)
    }

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
     */
    @JvmStatic
    fun downloadDefaultModelBlocking(context: Context, onProgressPercent: ProgressCallback?): Boolean {
        cancelRequested = false
        Log.d(TAG, "Downloading default model (blocking): $DEFAULT_MODEL_NAME")

        val destFile = getDefaultModelFile(context)
        val tempFile = File(getModelsDir(context), "$DEFAULT_MODEL_NAME.tmp")

        return try {
            val url = URL(DEFAULT_MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            false
        }
    }

    suspend fun downloadDefaultModel(
        context: Context,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        cancelRequested = false
        onProgress(DownloadProgress(0, 0, true))
        Log.d(TAG, "Downloading default model: $DEFAULT_MODEL_NAME")

        val destFile = getDefaultModelFile(context)
        val tempFile = File(getModelsDir(context), "$DEFAULT_MODEL_NAME.tmp")

        try {
            val url = URL(DEFAULT_MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
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

            onProgress(DownloadProgress(tempFile.length(), totalBytes, true))

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
                    onProgress(DownloadProgress(downloaded, totalBytes, true))
                }
            }
            input.close()

            if (!tempFile.renameTo(destFile)) {
                throw java.io.IOException("Failed to move downloaded file to ${destFile.absolutePath}")
            }
            onProgress(DownloadProgress())
            Log.d(TAG, "Model downloaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            onProgress(DownloadProgress())
            false
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import android.util.Log
import helium314.keyboard.latin.voice.WavEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cloud speech-to-text using OpenAI Whisper API.
 * Sends recorded audio as WAV to api.openai.com/v1/audio/transcriptions.
 */
object WhisperCloudClient {
    private const val TAG = "WhisperCloudClient"
    private const val API_URL = "https://api.openai.com/v1/audio/transcriptions"

    /** Result of a cloud transcription attempt. */
    sealed class TranscribeResult {
        data class Success(val text: String) : TranscribeResult()
        data class Empty(val message: String = "No speech detected") : TranscribeResult()
        data class ApiError(val code: Int, val message: String) : TranscribeResult()
        data class NetworkError(val message: String) : TranscribeResult()
    }

    /**
     * Transcribe raw PCM audio samples using OpenAI Whisper API.
     * @param apiKey OpenAI API key
     * @param samples 16-bit PCM samples at 16 kHz mono
     * @return TranscribeResult with success text or specific error info
     */
    suspend fun transcribe(apiKey: String, samples: ShortArray): TranscribeResult = withContext(Dispatchers.IO) {
        val wavBytes = WavEncoder.encodeWaveBytes(samples)
        val boundary = "----HeliBoard${System.currentTimeMillis()}"

        val url = URL(API_URL)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            DataOutputStream(connection.outputStream).use { out ->
                // file field
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n")
                out.writeBytes("Content-Type: audio/wav\r\n\r\n")
                out.write(wavBytes)
                out.writeBytes("\r\n")

                // model field
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"model\"\r\n\r\n")
                out.writeBytes("whisper-1\r\n")

                // response_format field
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n")
                out.writeBytes("json\r\n")

                out.writeBytes("--$boundary--\r\n")
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val text = json.getString("text").trim()
                if (text.isEmpty()) TranscribeResult.Empty()
                else TranscribeResult.Success(text)
            } else {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "no body"
                } catch (_: Exception) { "unreadable" }
                Log.e(TAG, "HTTP $responseCode: $errorBody")

                val userMessage = when (responseCode) {
                    401 -> "Invalid OpenAI API key"
                    429 -> "API quota exceeded — check your OpenAI billing"
                    500, 502, 503 -> "OpenAI server error — try again"
                    else -> "Cloud transcription failed (HTTP $responseCode)"
                }
                TranscribeResult.ApiError(responseCode, userMessage)
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Transcription timed out: ${e.message}")
            TranscribeResult.NetworkError("Request timed out — check your connection")
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "DNS resolution failed: ${e.message}")
            TranscribeResult.NetworkError("No internet connection")
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed: ${e.javaClass.simpleName}: ${e.message}")
            TranscribeResult.NetworkError("Network error: ${e.javaClass.simpleName}")
        } finally {
            connection.disconnect()
        }
    }
}

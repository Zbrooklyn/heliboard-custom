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

    /**
     * Transcribe raw PCM audio samples using OpenAI Whisper API.
     * @param apiKey OpenAI API key
     * @param samples 16-bit PCM samples at 16 kHz mono
     * @return transcribed text, or null on failure
     */
    suspend fun transcribe(apiKey: String, samples: ShortArray): String? = withContext(Dispatchers.IO) {
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
                json.getString("text").trim().ifEmpty { null }
            } else {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "no body"
                } catch (_: Exception) { "unreadable" }
                Log.e(TAG, "HTTP $responseCode: $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        } finally {
            connection.disconnect()
        }
    }
}

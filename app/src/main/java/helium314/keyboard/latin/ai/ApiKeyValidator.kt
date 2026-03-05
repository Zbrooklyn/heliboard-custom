package helium314.keyboard.latin.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiKeyValidator {

    sealed class Result {
        data object Valid : Result()
        data class Invalid(val message: String) : Result()
    }

    /**
     * Validate an OpenAI key by sending a minimal chat completion request (max_tokens=1).
     * This works with all key types including project-scoped keys (sk-proj-...) which
     * may not have permission to list models via GET /v1/models.
     */
    suspend fun validateOpenAI(apiKey: String): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.Invalid("Key is empty")
        if (!apiKey.startsWith("sk-")) return@withContext Result.Invalid("Key should start with sk-")
        val conn = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            // Minimal request — costs ~2 tokens
            OutputStreamWriter(conn.outputStream).use {
                it.write("""{"model":"gpt-4o-mini","max_tokens":1,"messages":[{"role":"user","content":"hi"}]}""")
            }
            val code = conn.responseCode
            if (code == 200) Result.Valid
            else {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) { "" }
                val msg = when (code) {
                    401 -> "Invalid API key"
                    403 -> "Key lacks permission — check your OpenAI project settings"
                    429 -> "Rate limited or quota exceeded — check your OpenAI billing"
                    else -> "HTTP $code"
                }
                Result.Invalid("$msg $errorBody".trim())
            }
        } catch (e: Exception) {
            Result.Invalid(e.message ?: "Connection failed")
        } finally {
            conn.disconnect()
        }
    }

    /** Quick local format check — no network call. */
    fun isValidOpenAIKey(key: String): Boolean =
        key.isNotBlank() && key.startsWith("sk-") && key.length >= 20

    /** Quick local format check — no network call. */
    fun isValidGeminiKey(key: String): Boolean =
        key.isNotBlank() && key.length >= 20

    suspend fun validateGemini(apiKey: String): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.Invalid("Key is empty")
        // Gemini API accepts the key as a query parameter
        val conn = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val code = conn.responseCode
            if (code == 200) Result.Valid
            else {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) { "" }
                Result.Invalid("HTTP $code — check your key. $errorBody".trim())
            }
        } catch (e: Exception) {
            Result.Invalid(e.message ?: "Connection failed")
        } finally {
            conn.disconnect()
        }
    }
}

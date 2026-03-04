package helium314.keyboard.latin.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ApiKeyValidator {

    sealed class Result {
        data object Valid : Result()
        data class Invalid(val message: String) : Result()
    }

    suspend fun validateOpenAI(apiKey: String): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.Invalid("Key is empty")
        if (!apiKey.startsWith("sk-")) return@withContext Result.Invalid("Key should start with sk-")
        val conn = URL("https://api.openai.com/v1/models").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
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

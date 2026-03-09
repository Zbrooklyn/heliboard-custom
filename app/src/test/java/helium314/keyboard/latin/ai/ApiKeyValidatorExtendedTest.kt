// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Extended edge-case tests for [ApiKeyValidator].
 *
 * The base [ApiKeyValidatorTest] covers: empty, blank, valid gemini format,
 * valid openai sk- prefix, and very short keys. This class adds boundary
 * and format-specific cases that are not already covered.
 */
class ApiKeyValidatorExtendedTest {

    // ---- OpenAI format checks ----

    @Test
    fun `openai key with sk-proj prefix is valid`() {
        val key = "sk-proj-" + "a".repeat(40)
        assertTrue(ApiKeyValidator.isValidOpenAIKey(key),
            "Project-scoped keys (sk-proj-...) should pass format validation")
    }

    @Test
    fun `openai key without sk prefix is invalid`() {
        val key = "pk-" + "a".repeat(40)
        assertFalse(ApiKeyValidator.isValidOpenAIKey(key),
            "Keys not starting with sk- should fail format validation")
    }

    @Test
    fun `openai key exactly 20 chars is valid`() {
        // "sk-" (3 chars) + 17 chars = 20 total
        val key = "sk-" + "a".repeat(17)
        assertEquals(20, key.length)
        assertTrue(ApiKeyValidator.isValidOpenAIKey(key),
            "Key at exactly the minimum length (20) should be valid")
    }

    @Test
    fun `openai key 19 chars is invalid`() {
        // "sk-" (3 chars) + 16 chars = 19 total
        val key = "sk-" + "a".repeat(16)
        assertEquals(19, key.length)
        assertFalse(ApiKeyValidator.isValidOpenAIKey(key),
            "Key one char below minimum length should be invalid")
    }

    // ---- Gemini format checks ----

    @Test
    fun `gemini key exactly 20 chars is valid`() {
        val key = "a".repeat(20)
        assertEquals(20, key.length)
        assertTrue(ApiKeyValidator.isValidGeminiKey(key),
            "Gemini key at exactly the minimum length (20) should be valid")
    }

    @Test
    fun `gemini key 19 chars is invalid`() {
        val key = "a".repeat(19)
        assertEquals(19, key.length)
        assertFalse(ApiKeyValidator.isValidGeminiKey(key),
            "Gemini key one char below minimum length should be invalid")
    }

    // ---- Whitespace edge cases ----

    @Test
    fun `key with only whitespace is invalid`() {
        val whitespace = "\t\n  "
        assertFalse(ApiKeyValidator.isValidOpenAIKey(whitespace),
            "Tab/newline/space-only string should fail OpenAI validation")
        assertFalse(ApiKeyValidator.isValidGeminiKey(whitespace),
            "Tab/newline/space-only string should fail Gemini validation")
    }

    // ---- Result sealed class checks ----

    @Test
    fun `Result sealed class Valid is singleton`() {
        val a = ApiKeyValidator.Result.Valid
        val b = ApiKeyValidator.Result.Valid
        assertSame(a, b, "Result.Valid is a data object and should be referentially equal")
    }

    @Test
    fun `Result Invalid holds message`() {
        val result = ApiKeyValidator.Result.Invalid("bad key")
        assertIs<ApiKeyValidator.Result.Invalid>(result)
        assertEquals("bad key", result.message,
            "Result.Invalid should preserve the error message")
    }

    // ---- Unicode edge case ----

    @Test
    fun `openai key with unicode is technically valid if format matches`() {
        // The format validator only checks: isNotBlank, startsWith("sk-"), length >= 20
        // It does not restrict character set, so unicode chars pass if format matches.
        val key = "sk-" + "\u00e9\u00f1\u00fc\u00e4\u00f6".repeat(4) // 3 + 20 = 23 chars
        assertTrue(key.length >= 20, "Test key should meet minimum length")
        assertTrue(key.startsWith("sk-"), "Test key should have sk- prefix")
        assertTrue(ApiKeyValidator.isValidOpenAIKey(key),
            "Format validator does not restrict charset; unicode key matching format should pass")
    }
}

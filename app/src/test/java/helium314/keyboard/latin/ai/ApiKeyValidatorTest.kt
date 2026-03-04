// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiKeyValidatorTest {

    @Test
    fun `empty key is invalid`() {
        assertFalse(ApiKeyValidator.isValidGeminiKey(""))
        assertFalse(ApiKeyValidator.isValidOpenAIKey(""))
    }

    @Test
    fun `blank key is invalid`() {
        assertFalse(ApiKeyValidator.isValidGeminiKey("   "))
        assertFalse(ApiKeyValidator.isValidOpenAIKey("   "))
    }

    @Test
    fun `gemini key with valid format accepted`() {
        // Gemini keys are typically 39 chars starting with AIza
        assertTrue(ApiKeyValidator.isValidGeminiKey("AIzaSyA" + "a".repeat(32)))
    }

    @Test
    fun `openai key with sk- prefix accepted`() {
        assertTrue(ApiKeyValidator.isValidOpenAIKey("sk-" + "a".repeat(45)))
    }

    @Test
    fun `very short keys are invalid`() {
        assertFalse(ApiKeyValidator.isValidGeminiKey("abc"))
        assertFalse(ApiKeyValidator.isValidOpenAIKey("sk-"))
    }
}

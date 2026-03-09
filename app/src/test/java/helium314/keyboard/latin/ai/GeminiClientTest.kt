// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GeminiClientTest {

    // -- Reflection helpers to access private methods on the GeminiClient object --

    private fun callParseResponse(json: String): String? {
        val method = GeminiClient::class.java.getDeclaredMethod("parseResponse", String::class.java)
        method.isAccessible = true
        return method.invoke(GeminiClient, json) as? String
    }

    private fun callParseVariants(response: String, original: String): RewriteVariants {
        val method = GeminiClient::class.java.getDeclaredMethod(
            "parseVariants", String::class.java, String::class.java
        )
        method.isAccessible = true
        return method.invoke(GeminiClient, response, original) as RewriteVariants
    }

    private fun callFallbackVariants(original: String): RewriteVariants {
        val method = GeminiClient::class.java.getDeclaredMethod("fallbackVariants", String::class.java)
        method.isAccessible = true
        return method.invoke(GeminiClient, original) as RewriteVariants
    }

    // ---- parseResponse tests ----

    @Test
    fun `parseResponse extracts text from valid Gemini response`() {
        val json = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "Hello world"
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()

        val result = callParseResponse(json)
        assertNotNull(result)
        assertEquals("Hello world", result)
    }

    @Test
    fun `parseResponse trims whitespace from text`() {
        val json = """{"candidates":[{"content":{"parts":[{"text":"  spaced out  "}]}}]}"""

        val result = callParseResponse(json)
        assertNotNull(result)
        assertEquals("spaced out", result)
    }

    @Test
    fun `parseResponse returns null for empty text`() {
        val json = """{"candidates":[{"content":{"parts":[{"text":""}]}}]}"""

        val result = callParseResponse(json)
        assertNull(result)
    }

    @Test
    fun `parseResponse returns null for whitespace-only text`() {
        val json = """{"candidates":[{"content":{"parts":[{"text":"   "}]}}]}"""

        // trim() produces "", ifEmpty returns null
        val result = callParseResponse(json)
        assertNull(result)
    }

    @Test
    fun `parseResponse returns null for malformed JSON`() {
        val result = callParseResponse("not valid json")
        assertNull(result)
    }

    @Test
    fun `parseResponse returns null for missing candidates`() {
        val json = """{"result": "no candidates here"}"""
        val result = callParseResponse(json)
        assertNull(result)
    }

    @Test
    fun `parseResponse returns null for empty candidates array`() {
        val json = """{"candidates":[]}"""
        val result = callParseResponse(json)
        assertNull(result)
    }

    // ---- parseVariants tests ----

    @Test
    fun `parseVariants extracts all 5 styles`() {
        val response = """
            {
                "clean": "Hello there.",
                "professional": "Good day.",
                "casual": "Hey!",
                "concise": "Hi.",
                "emojify": "Hello! :wave:"
            }
        """.trimIndent()

        val result = callParseVariants(response, "hello ther")
        assertEquals("Hello there.", result.clean)
        assertEquals("Good day.", result.professional)
        assertEquals("Hey!", result.casual)
        assertEquals("Hi.", result.concise)
        assertEquals("Hello! :wave:", result.emojify)
    }

    @Test
    fun `parseVariants strips markdown code fences`() {
        val response = "```json\n" +
            """{"clean":"Fixed.","professional":"Greetings.","casual":"Yo!","concise":"Hi.","emojify":"Hi! :)"}""" +
            "\n```"

        val result = callParseVariants(response, "original")
        assertEquals("Fixed.", result.clean)
        assertEquals("Greetings.", result.professional)
        assertEquals("Yo!", result.casual)
        assertEquals("Hi.", result.concise)
        assertEquals("Hi! :)", result.emojify)
    }

    @Test
    fun `parseVariants uses original for missing keys`() {
        val original = "my original text"
        // JSON is missing "casual" and "emojify" keys
        val response = """
            {
                "clean": "My original text.",
                "professional": "Respectfully, my original text.",
                "concise": "text"
            }
        """.trimIndent()

        val result = callParseVariants(response, original)
        assertEquals("My original text.", result.clean)
        assertEquals("Respectfully, my original text.", result.professional)
        assertEquals(original, result.casual)
        assertEquals("text", result.concise)
        assertEquals(original, result.emojify)
    }

    @Test
    fun `parseVariants returns fallback for invalid JSON`() {
        val original = "keep me"
        val result = callParseVariants("totally not json {{{{", original)
        assertEquals(original, result.clean)
        assertEquals(original, result.professional)
        assertEquals(original, result.casual)
        assertEquals(original, result.concise)
        assertEquals(original, result.emojify)
    }

    // ---- fallbackVariants test ----

    @Test
    fun `fallbackVariants returns original for all fields`() {
        val original = "unchanged text"
        val result = callFallbackVariants(original)
        assertEquals(original, result.clean)
        assertEquals(original, result.professional)
        assertEquals(original, result.casual)
        assertEquals(original, result.concise)
        assertEquals(original, result.emojify)
    }

    // ---- name property test ----

    @Test
    fun `name property is Gemini`() {
        assertEquals("Gemini", GeminiClient.name)
    }
}

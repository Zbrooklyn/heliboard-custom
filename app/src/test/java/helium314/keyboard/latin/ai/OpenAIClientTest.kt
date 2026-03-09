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
class OpenAIClientTest {

    // -- Reflection helpers to access private methods on the OpenAIClient object --

    private fun callParseResponse(json: String): String? {
        val method = OpenAIClient::class.java.getDeclaredMethod("parseResponse", String::class.java)
        method.isAccessible = true
        return method.invoke(OpenAIClient, json) as? String
    }

    private fun callParseVariants(response: String, original: String): RewriteVariants {
        val method = OpenAIClient::class.java.getDeclaredMethod(
            "parseVariants", String::class.java, String::class.java
        )
        method.isAccessible = true
        return method.invoke(OpenAIClient, response, original) as RewriteVariants
    }

    private fun callFallbackVariants(original: String): RewriteVariants {
        val method = OpenAIClient::class.java.getDeclaredMethod("fallbackVariants", String::class.java)
        method.isAccessible = true
        return method.invoke(OpenAIClient, original) as RewriteVariants
    }

    // ---- parseResponse tests ----

    @Test
    fun `parseResponse extracts content from valid response`() {
        val json = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "Hello world"
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
    fun `parseResponse trims whitespace from content`() {
        val json = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "  trimmed text  "
                        }
                    }
                ]
            }
        """.trimIndent()

        val result = callParseResponse(json)
        assertNotNull(result)
        assertEquals("trimmed text", result)
    }

    @Test
    fun `parseResponse returns null for malformed JSON`() {
        val result = callParseResponse("this is not json at all")
        assertNull(result)
    }

    @Test
    fun `parseResponse returns null for empty choices`() {
        val json = """{"choices": []}"""
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
        // JSON is missing the "emojify" key
        val response = """
            {
                "clean": "My original text.",
                "professional": "My original text, respectfully.",
                "casual": "my original text lol",
                "concise": "text"
            }
        """.trimIndent()

        val result = callParseVariants(response, original)
        assertEquals("My original text.", result.clean)
        assertEquals("My original text, respectfully.", result.professional)
        assertEquals("my original text lol", result.casual)
        assertEquals("text", result.concise)
        // Missing key falls back to original
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

    // ---- fallbackVariants tests ----

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
    fun `name property is OpenAI`() {
        assertEquals("OpenAI", OpenAIClient.name)
    }
}

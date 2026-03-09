// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the Whisper hallucination detection logic in [VoiceInputManager].
 *
 * The `isHallucination` method is private on the companion object, so we use
 * reflection to access it for testing.
 */
class HallucinationFilterTest {

    private fun isHallucination(text: String): Boolean {
        val companion = VoiceInputManager::class.java.getDeclaredClasses()
            .first { it.simpleName == "Companion" }
        val method = companion.getDeclaredMethod("isHallucination", String::class.java)
        method.isAccessible = true
        val companionInstance = VoiceInputManager::class.java
            .getDeclaredField("Companion")
            .apply { isAccessible = true }
            .get(null)
        return method.invoke(companionInstance, text) as Boolean
    }

    @Test
    fun `empty string is hallucination`() {
        assertTrue(isHallucination(""), "Empty string should be detected as hallucination")
    }

    @Test
    fun `blank string is hallucination`() {
        assertTrue(isHallucination("   "), "Blank/whitespace-only string should be hallucination")
        assertTrue(isHallucination("\t\n"), "Tab and newline should be hallucination")
    }

    @Test
    fun `known phrase — thank you for watching`() {
        assertTrue(isHallucination("thank you for watching"),
            "Exact known phrase should be hallucination")
    }

    @Test
    fun `known phrase with trailing punctuation`() {
        assertTrue(isHallucination("Thank you for watching!!!"),
            "Trailing exclamation marks should be stripped")
        assertTrue(isHallucination("thanks for listening..."),
            "Trailing dots should be stripped")
        assertTrue(isHallucination("please subscribe!?!"),
            "Mixed trailing punctuation should be stripped")
        assertTrue(isHallucination("bye."),
            "Single trailing period should be stripped")
    }

    @Test
    fun `known phrase with mixed case`() {
        assertTrue(isHallucination("SUBSCRIBE"), "Uppercase should match")
        assertTrue(isHallucination("Subscribe"), "Title case should match")
        assertTrue(isHallucination("ThAnK yOu FoR wAtChInG"), "Mixed case should match")
    }

    @Test
    fun `known phrase with trailing whitespace`() {
        assertTrue(isHallucination("  bye  "), "Leading/trailing spaces should be trimmed")
        assertTrue(isHallucination("\tsubscribe\t"), "Tabs should be trimmed")
        assertTrue(isHallucination("  the end  "), "Phrase with surrounding spaces should match")
    }

    @Test
    fun `real speech is not hallucination`() {
        assertFalse(isHallucination("Hello, how are you doing today"),
            "Normal speech should not be a hallucination")
        assertFalse(isHallucination("I need to schedule a meeting for tomorrow"),
            "Normal sentence should not be a hallucination")
        assertFalse(isHallucination("Please send me the report by Friday"),
            "Normal request should not be a hallucination")
    }

    @Test
    fun `single word you is hallucination`() {
        assertTrue(isHallucination("you"), "'you' is a known hallucination phrase")
        assertTrue(isHallucination("You"), "Capitalized 'You' should also match")
        assertTrue(isHallucination("  you  "), "'you' with whitespace should match")
    }

    @Test
    fun `ellipsis is hallucination`() {
        assertTrue(isHallucination("..."), "Literal ellipsis is in the hallucination set")
    }

    @Test
    fun `short real text is not hallucination`() {
        assertFalse(isHallucination("yes"), "'yes' is not in the hallucination set")
        assertFalse(isHallucination("no"), "'no' is not in the hallucination set")
        assertFalse(isHallucination("ok"), "'ok' is not in the hallucination set")
        assertFalse(isHallucination("hello"), "'hello' is not in the hallucination set")
    }
}

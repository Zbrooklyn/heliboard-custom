// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RewriteProviderTest {

    private val sampleVariants = RewriteVariants(
        clean = "Hello there",
        professional = "Good day",
        casual = "Hey",
        concise = "Hi",
        emojify = "Hello! \uD83D\uDC4B"
    )

    @Test
    fun `toList returns all 5 variants in order`() {
        val list = sampleVariants.toList()
        assertEquals(5, list.size)
        assertEquals("Clean", list[0].first)
        assertEquals("Professional", list[1].first)
        assertEquals("Casual", list[2].first)
        assertEquals("Concise", list[3].first)
        assertEquals("Emojify", list[4].first)
    }

    @Test
    fun `toList pairs match fields`() {
        val list = sampleVariants.toList()
        assertEquals(sampleVariants.clean, list.first { it.first == "Clean" }.second)
        assertEquals(sampleVariants.professional, list.first { it.first == "Professional" }.second)
        assertEquals(sampleVariants.casual, list.first { it.first == "Casual" }.second)
        assertEquals(sampleVariants.concise, list.first { it.first == "Concise" }.second)
        assertEquals(sampleVariants.emojify, list.first { it.first == "Emojify" }.second)
    }

    @Test
    fun `data class equality works`() {
        val copy = RewriteVariants(
            clean = "Hello there",
            professional = "Good day",
            casual = "Hey",
            concise = "Hi",
            emojify = "Hello! \uD83D\uDC4B"
        )
        assertEquals(sampleVariants, copy)
        assertEquals(sampleVariants.hashCode(), copy.hashCode())
    }

    @Test
    fun `data class copy modifies single field`() {
        val modified = sampleVariants.copy(clean = "Updated")
        assertEquals("Updated", modified.clean)
        assertEquals(sampleVariants.professional, modified.professional)
        assertEquals(sampleVariants.casual, modified.casual)
        assertEquals(sampleVariants.concise, modified.concise)
        assertEquals(sampleVariants.emojify, modified.emojify)
        assertNotEquals(sampleVariants, modified)
    }

    @Test
    fun `mock provider rewriteText returns expected result`() = runTest {
        val provider = TestRewriteProvider()
        val result = provider.rewriteText("key123", "Hello world", "professional")
        assertEquals("rewritten: Hello world (professional)", result)
    }

    @Test
    fun `mock provider rewriteAll returns expected variants`() = runTest {
        val provider = TestRewriteProvider()
        val result = provider.rewriteAll("key123", "Hello world")
        assertEquals("clean: Hello world", result.clean)
        assertEquals("pro: Hello world", result.professional)
        assertEquals("casual: Hello world", result.casual)
        assertEquals("short: Hello world", result.concise)
        assertEquals("emoji: Hello world", result.emojify)
    }

    private class TestRewriteProvider : RewriteProvider {
        override val name = "Test"

        override suspend fun rewriteText(apiKey: String, originalText: String, style: String): String {
            return "rewritten: $originalText ($style)"
        }

        override suspend fun rewriteAll(apiKey: String, originalText: String): RewriteVariants {
            return RewriteVariants(
                clean = "clean: $originalText",
                professional = "pro: $originalText",
                casual = "casual: $originalText",
                concise = "short: $originalText",
                emojify = "emoji: $originalText"
            )
        }
    }
}

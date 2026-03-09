// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RewriteHelperTest {

    private val expectedVariants = RewriteVariants(
        clean = "clean: Hello world",
        professional = "pro: Hello world",
        casual = "casual: Hello world",
        concise = "short: Hello world",
        emojify = "emoji: Hello world"
    )

    @Test
    fun `rewriteAllBlocking delegates to provider rewriteAll`() {
        val provider = TestProvider()
        RewriteHelper.rewriteAllBlocking(provider, "key", "Hello world")
        assertEquals("Hello world", provider.lastRewriteAllText)
    }

    @Test
    fun `rewriteSingleBlocking delegates to provider rewriteText`() {
        val provider = TestProvider()
        RewriteHelper.rewriteSingleBlocking(provider, "key", "Hello world", "casual")
        assertEquals("Hello world", provider.lastRewriteText)
    }

    @Test
    fun `rewriteAllBlocking returns provider result`() {
        val provider = TestProvider()
        val result = RewriteHelper.rewriteAllBlocking(provider, "key", "Hello world")
        assertEquals(expectedVariants, result)
    }

    @Test
    fun `rewriteSingleBlocking returns provider result`() {
        val provider = TestProvider()
        val result = RewriteHelper.rewriteSingleBlocking(provider, "key", "Hello world", "professional")
        assertEquals("rewritten: Hello world (professional)", result)
    }

    @Test
    fun `rewriteSingleBlocking passes style through`() {
        val provider = TestProvider()
        RewriteHelper.rewriteSingleBlocking(provider, "key", "text", "concise")
        assertEquals("concise", provider.lastStyle)

        RewriteHelper.rewriteSingleBlocking(provider, "key", "text", "emojify")
        assertEquals("emojify", provider.lastStyle)
    }

    @Test
    fun `provider exception propagates through blocking wrapper`() {
        val provider = ThrowingProvider()
        assertFailsWith<IllegalStateException> {
            RewriteHelper.rewriteAllBlocking(provider, "key", "text")
        }
        assertFailsWith<IllegalStateException> {
            RewriteHelper.rewriteSingleBlocking(provider, "key", "text", "clean")
        }
    }

    private class TestProvider : RewriteProvider {
        override val name = "Test"
        var lastStyle: String? = null
        var lastRewriteText: String? = null
        var lastRewriteAllText: String? = null

        override suspend fun rewriteText(apiKey: String, originalText: String, style: String): String {
            lastRewriteText = originalText
            lastStyle = style
            return "rewritten: $originalText ($style)"
        }

        override suspend fun rewriteAll(apiKey: String, originalText: String): RewriteVariants {
            lastRewriteAllText = originalText
            return RewriteVariants(
                clean = "clean: $originalText",
                professional = "pro: $originalText",
                casual = "casual: $originalText",
                concise = "short: $originalText",
                emojify = "emoji: $originalText"
            )
        }
    }

    private class ThrowingProvider : RewriteProvider {
        override val name = "Throwing"

        override suspend fun rewriteText(apiKey: String, originalText: String, style: String): String {
            throw IllegalStateException("API unavailable")
        }

        override suspend fun rewriteAll(apiKey: String, originalText: String): RewriteVariants {
            throw IllegalStateException("API unavailable")
        }
    }
}

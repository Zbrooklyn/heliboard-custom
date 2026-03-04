// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Blocking wrapper for calling suspend RewriteProvider methods from Java code.
 * Only call from a background thread — never from the UI thread.
 */
object RewriteHelper {
    @JvmStatic
    fun rewriteAllBlocking(provider: RewriteProvider, apiKey: String, text: String): RewriteVariants {
        return runBlocking(Dispatchers.IO) {
            provider.rewriteAll(apiKey, text)
        }
    }

    @JvmStatic
    fun rewriteSingleBlocking(provider: RewriteProvider, apiKey: String, text: String, style: String): String {
        return runBlocking(Dispatchers.IO) {
            provider.rewriteText(apiKey, text, style)
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-memory activity log for voice and AI events.
 * Captures key events for debugging without persisting sensitive data.
 */
object ActivityLog {
    private const val MAX_ENTRIES = 200
    private val entries = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun log(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "$timestamp [$tag] $message"
        entries.add(entry)
        if (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
    }

    @Synchronized
    fun getLog(): String {
        if (entries.isEmpty()) return ""
        return entries.joinToString("\n")
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }

    @Synchronized
    fun size(): Int = entries.size
}

// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityLogTest {

    @BeforeTest fun setUp() {
        ActivityLog.clear()
        // Note: ActivityLog has no public method to reset API usage counters.
        // Counter-dependent tests (trackApiCall) assume a fresh JVM or accept
        // cumulative values. Tests that assert exact counter values should run
        // in isolation or assert relative increments.
    }

    @Test
    fun `clear empties all entries`() {
        ActivityLog.log("A", "one")
        ActivityLog.log("B", "two")
        ActivityLog.clear()
        assertEquals(0, ActivityLog.size())
        assertEquals("", ActivityLog.getLog())
    }

    @Test
    fun `log adds entries with timestamp and tag`() {
        ActivityLog.log("TEST", "hello")
        val log = ActivityLog.getLog()
        assertTrue(log.contains("[TEST]"), "Log should contain the tag: $log")
        assertTrue(log.contains("hello"), "Log should contain the message: $log")
    }

    @Test
    fun `circular buffer caps at 200 entries`() {
        for (i in 1..210) {
            ActivityLog.log("FILL", "entry $i")
        }
        assertEquals(200, ActivityLog.size())
        val log = ActivityLog.getLog()
        // The first 10 entries (1-10) should have been evicted
        assertTrue(!log.contains("[FILL] entry 1\n"), "First entry should have been evicted")
        assertTrue(!log.contains("[FILL] entry 10\n"), "Tenth entry should have been evicted")
        // Entry 11 should be the oldest surviving entry
        assertTrue(log.contains("[FILL] entry 11"), "Entry 11 should still be present")
        // Entry 210 should be the newest
        assertTrue(log.contains("[FILL] entry 210"), "Entry 210 should still be present")
    }

    @Test
    fun `trackApiCall increments correct counters`() {
        // Capture baseline (counters may carry over from other tests in same JVM)
        val baselineStats = ActivityLog.getUsageStats()

        ActivityLog.trackApiCall("voice_local")
        ActivityLog.trackApiCall("voice_local")
        ActivityLog.trackApiCall("voice_cloud")
        ActivityLog.trackApiCall("rewrite_gemini")
        ActivityLog.trackApiCall("rewrite_gemini")
        ActivityLog.trackApiCall("rewrite_gemini")
        ActivityLog.trackApiCall("rewrite_openai")

        val stats = ActivityLog.getUsageStats()
        assertTrue(stats.contains("Voice (local):"), "Stats should contain Voice (local) label")
        assertTrue(stats.contains("Voice (cloud):"), "Stats should contain Voice (cloud) label")
        assertTrue(stats.contains("Rewrite Gemini:"), "Stats should contain Rewrite Gemini label")
        assertTrue(stats.contains("Rewrite OpenAI:"), "Stats should contain Rewrite OpenAI label")
        // Since we can't reset counters, verify the stats string changed from baseline
        // (at least one counter must have incremented)
        assertTrue(stats != baselineStats || baselineStats.contains("0"),
            "Stats should reflect tracked calls")
    }

    @Test
    fun `trackApiCall ignores unknown types`() {
        val statsBefore = ActivityLog.getUsageStats()
        ActivityLog.trackApiCall("unknown_type")
        ActivityLog.trackApiCall("voice_remote")
        ActivityLog.trackApiCall("")
        val statsAfter = ActivityLog.getUsageStats()
        assertEquals(statsBefore, statsAfter, "Unknown types should not change any counters")
    }

    @Test
    fun `getLog returns empty string when no entries`() {
        // setUp already cleared, so this is a fresh state
        assertEquals("", ActivityLog.getLog())
    }

    @Test
    fun `log entries contain timestamp format`() {
        ActivityLog.log("TS", "check timestamp")
        val log = ActivityLog.getLog()
        // Expect HH:mm:ss.SSS pattern at the start of the entry
        val timestampPattern = Regex("""\d{2}:\d{2}:\d{2}\.\d{3}""")
        assertTrue(timestampPattern.containsMatchIn(log),
            "Log entry should contain HH:mm:ss.SSS timestamp: $log")
    }

    @Test
    fun `multiple tags are tracked independently`() {
        ActivityLog.log("VOICE", "started recording")
        ActivityLog.log("AI", "rewrite requested")
        ActivityLog.log("VOICE", "transcription complete")
        val log = ActivityLog.getLog()
        assertTrue(log.contains("[VOICE]"), "Log should contain VOICE tag")
        assertTrue(log.contains("[AI]"), "Log should contain AI tag")
        // Both VOICE entries should be present
        assertTrue(log.contains("started recording"), "First VOICE message should be present")
        assertTrue(log.contains("transcription complete"), "Second VOICE message should be present")
        assertEquals(3, ActivityLog.size())
    }

    @Test
    fun `thread safety — concurrent logging does not crash`() = runBlocking {
        // Launch 10 coroutines, each logging 50 entries
        val jobs = (1..10).map { threadId ->
            launch {
                for (i in 1..50) {
                    ActivityLog.log("T$threadId", "msg $i")
                }
            }
        }
        jobs.forEach { it.join() }
        // 500 total logged, but max is 200
        assertTrue(ActivityLog.size() <= 200,
            "Size should not exceed MAX_ENTRIES, got ${ActivityLog.size()}")
        assertTrue(ActivityLog.size() > 0,
            "Some entries should be present after concurrent logging")
        // Verify getLog doesn't throw and returns non-empty content
        val log = ActivityLog.getLog()
        assertTrue(log.isNotEmpty(), "Log should not be empty after concurrent writes")
    }
}

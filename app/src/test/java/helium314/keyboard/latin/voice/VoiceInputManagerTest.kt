// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.DeviceProtectedUtils
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [VoiceInputManager] public API surface — state management, STT mode
 * detection, mode labels, and SAM callback interfaces.
 *
 * Uses Robolectric to provide an Android [Context] for SharedPreferences access.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VoiceInputManagerTest {

    private lateinit var context: Context

    /** No-op callbacks used when constructing VoiceInputManager for tests. */
    private val noOpResult = VoiceInputManager.ResultCallback { }
    private val noOpState = VoiceInputManager.StateCallback { }

    @Before
    fun setUp() {
        // Reset the static cached prefs inside DeviceProtectedUtils so each test
        // starts with a fresh SharedPreferences instance from Robolectric.
        try {
            val prefsField = DeviceProtectedUtils::class.java.getDeclaredField("prefs")
            prefsField.isAccessible = true
            prefsField.set(null, null)
        } catch (_: Exception) {
            // If reflection fails, tests that depend on prefs may share state —
            // but the test class will still compile and run.
        }
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // Clear any prefs written during the test.
        val prefName = context.packageName + "_preferences"
        context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    // -----------------------------------------------------------------------
    // State tests
    // -----------------------------------------------------------------------

    @Test
    fun `initial state is IDLE`() {
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertEquals(VoiceInputManager.VoiceState.IDLE, manager.currentState)
    }

    @Test
    fun `isRecording returns false when IDLE`() {
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertFalse(manager.isRecording(), "isRecording should be false when state is IDLE")
    }

    @Test
    fun `isModelLoaded returns false initially`() {
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertFalse(manager.isModelLoaded(), "No model should be loaded on a fresh instance")
    }

    @Test
    fun `VoiceState enum has all expected values`() {
        val values = VoiceInputManager.VoiceState.values()
        val names = values.map { it.name }.toSet()
        assertTrue("IDLE" in names, "VoiceState must contain IDLE")
        assertTrue("LISTENING" in names, "VoiceState must contain LISTENING")
        assertTrue("TRANSCRIBING" in names, "VoiceState must contain TRANSCRIBING")
        assertTrue("ERROR" in names, "VoiceState must contain ERROR")
        assertEquals(4, values.size, "VoiceState should have exactly 4 values")
    }

    // -----------------------------------------------------------------------
    // STT mode tests (SharedPreferences via Robolectric)
    // -----------------------------------------------------------------------

    @Test
    fun `default STT mode is local`() {
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertEquals("local", manager.getSttMode(), "Default STT mode should be 'local'")
    }

    @Test
    fun `isCloudMode returns true when mode is cloud`() {
        setSttModePref("cloud")
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertTrue(manager.isCloudMode(), "isCloudMode should be true when pref is 'cloud'")
    }

    @Test
    fun `isGoogleMode returns true when mode is google`() {
        setSttModePref("google")
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertTrue(manager.isGoogleMode(), "isGoogleMode should be true when pref is 'google'")
    }

    @Test
    fun `needsLocalModel returns true for local mode`() {
        setSttModePref("local")
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertTrue(manager.needsLocalModel(), "needsLocalModel should be true for 'local' mode")
    }

    @Test
    fun `needsLocalModel returns false for cloud mode`() {
        setSttModePref("cloud")
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertFalse(manager.needsLocalModel(), "needsLocalModel should be false for 'cloud' mode")
    }

    @Test
    fun `needsLocalModel returns false for google mode`() {
        setSttModePref("google")
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertFalse(manager.needsLocalModel(), "needsLocalModel should be false for 'google' mode")
    }

    // -----------------------------------------------------------------------
    // Mode label tests
    // -----------------------------------------------------------------------

    @Test
    fun `getSttModeLabel returns correct label for local`() {
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertEquals("Local STT", manager.getSttModeLabel("local"))
    }

    @Test
    fun `getSttModeLabel returns correct label for cloud`() {
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertEquals("Cloud STT", manager.getSttModeLabel("cloud"))
    }

    @Test
    fun `getSttModeLabel returns correct label for google`() {
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertEquals("Google STT", manager.getSttModeLabel("google"))
    }

    @Test
    fun `getSttModeLabel falls back to Local STT for unknown mode`() {
        val manager = VoiceInputManager(context, noOpResult, noOpState)
        assertEquals("Local STT", manager.getSttModeLabel("unknown_mode"))
    }

    // -----------------------------------------------------------------------
    // Callback interface (SAM) tests
    // -----------------------------------------------------------------------

    @Test
    fun `ResultCallback is SAM-compatible`() {
        var captured = ""
        val callback = VoiceInputManager.ResultCallback { text -> captured = text }
        callback.onResult("hello world")
        assertEquals("hello world", captured, "ResultCallback lambda should receive the text")
    }

    @Test
    fun `StateCallback is SAM-compatible`() {
        var captured: VoiceInputManager.VoiceState? = null
        val callback = VoiceInputManager.StateCallback { state -> captured = state }
        callback.onStateChange(VoiceInputManager.VoiceState.LISTENING)
        assertEquals(VoiceInputManager.VoiceState.LISTENING, captured,
            "StateCallback lambda should receive the state")
    }

    @Test
    fun `ErrorDetailCallback is SAM-compatible`() {
        var captured = ""
        val callback = VoiceInputManager.ErrorDetailCallback { msg -> captured = msg }
        callback.onErrorDetail("something went wrong")
        assertEquals("something went wrong", captured,
            "ErrorDetailCallback lambda should receive the message")
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Write the STT mode preference using the same SharedPreferences file
     * that [DeviceProtectedUtils.getSharedPreferences] resolves to under
     * Robolectric (packageName + "_preferences").
     */
    private fun setSttModePref(mode: String) {
        val prefName = context.packageName + "_preferences"
        context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            .edit()
            .putString(Settings.PREF_STT_MODE, mode)
            .apply()
        // Reset the cached static prefs so VoiceInputManager picks up the new value.
        try {
            val prefsField = DeviceProtectedUtils::class.java.getDeclaredField("prefs")
            prefsField.isAccessible = true
            prefsField.set(null, null)
        } catch (_: Exception) { }
    }
}

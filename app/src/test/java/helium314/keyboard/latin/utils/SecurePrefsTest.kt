// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SecurePrefsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    /**
     * SecurePrefs is a singleton that caches its SharedPreferences instance.
     * We must reset the cached instance between tests so each test starts fresh.
     */
    @Before
    fun resetSingleton() {
        val field = SecurePrefs::class.java.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, null) // Force re-creation on next get()
    }

    @Test
    fun `get returns SharedPreferences instance`() {
        val prefs = SecurePrefs.get(context)
        assertNotNull(prefs, "SecurePrefs.get() should never return null")
    }

    @Test
    fun `get returns same instance on repeated calls`() {
        val first = SecurePrefs.get(context)
        val second = SecurePrefs.get(context)
        assertSame(first, second, "SecurePrefs should return the same singleton instance")
    }

    @Test
    fun `can write and read string value`() {
        val prefs = SecurePrefs.get(context)
        prefs.edit().putString("test_key", "test_value").commit()

        val readBack = prefs.getString("test_key", null)
        assertEquals("test_value", readBack)
    }

    @Test
    fun `migrateFromPlainPrefs moves value to secure prefs`() {
        val plainPrefs = context.getSharedPreferences("plain_test", Context.MODE_PRIVATE)
        plainPrefs.edit().putString("api_key", "secret123").commit()

        SecurePrefs.migrateFromPlainPrefs(context, plainPrefs, "api_key")

        val securePrefs = SecurePrefs.get(context)
        assertEquals("secret123", securePrefs.getString("api_key", null),
            "Value should exist in secure prefs after migration")
        assertFalse(plainPrefs.contains("api_key"),
            "Key should be removed from plain prefs after migration")
    }

    @Test
    fun `migrateFromPlainPrefs skips empty values`() {
        val plainPrefs = context.getSharedPreferences("plain_empty", Context.MODE_PRIVATE)
        plainPrefs.edit().putString("api_key", "").commit()

        SecurePrefs.migrateFromPlainPrefs(context, plainPrefs, "api_key")

        val securePrefs = SecurePrefs.get(context)
        assertFalse(securePrefs.contains("api_key"),
            "Empty value should not be migrated to secure prefs")
        // Plain prefs still has the key (migration was skipped, not executed)
        assertTrue(plainPrefs.contains("api_key"),
            "Plain prefs should still contain the key since migration was skipped")
    }

    @Test
    fun `migrateFromPlainPrefs skips if already in secure`() {
        val plainPrefs = context.getSharedPreferences("plain_dup", Context.MODE_PRIVATE)
        plainPrefs.edit().putString("api_key", "old_value").commit()

        // Pre-populate secure prefs with the same key
        val securePrefs = SecurePrefs.get(context)
        securePrefs.edit().putString("api_key", "new_value").commit()

        SecurePrefs.migrateFromPlainPrefs(context, plainPrefs, "api_key")

        assertEquals("new_value", securePrefs.getString("api_key", null),
            "Secure prefs value should remain unchanged")
        assertTrue(plainPrefs.contains("api_key"),
            "Plain prefs key should not be removed when secure already has it")
    }

    @Test
    fun `migrateFromPlainPrefs skips if key not in plain`() {
        val plainPrefs = context.getSharedPreferences("plain_missing", Context.MODE_PRIVATE)
        // Do not add any key to plainPrefs

        // Should not crash
        SecurePrefs.migrateFromPlainPrefs(context, plainPrefs, "nonexistent_key")

        val securePrefs = SecurePrefs.get(context)
        assertFalse(securePrefs.contains("nonexistent_key"),
            "Nothing should be written to secure prefs when key is absent from plain prefs")
    }
}

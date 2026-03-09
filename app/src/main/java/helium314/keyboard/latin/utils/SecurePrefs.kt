package helium314.keyboard.latin.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Provides encrypted SharedPreferences for storing sensitive data like API keys.
 * Falls back to regular SharedPreferences if encryption fails (e.g., on old devices).
 */
object SecurePrefs {
    private const val TAG = "SecurePrefs"
    private const val PREFS_NAME = "whisperclick_secure_prefs"

    @Volatile
    private var instance: SharedPreferences? = null

    fun get(context: Context): SharedPreferences {
        return instance ?: synchronized(this) {
            instance ?: createEncryptedPrefs(context).also { instance = it }
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to regular", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /** Migrate a key from regular prefs to secure prefs (one-time). */
    fun migrateFromPlainPrefs(context: Context, regularPrefs: SharedPreferences, key: String) {
        val securePrefs = get(context)
        // Only migrate if the key exists in regular prefs and NOT in secure prefs
        if (regularPrefs.contains(key) && !securePrefs.contains(key)) {
            val value = regularPrefs.getString(key, null)
            if (!value.isNullOrEmpty()) {
                securePrefs.edit().putString(key, value).apply()
                regularPrefs.edit().remove(key).apply()
                Log.d(TAG, "Migrated key '$key' from plain to encrypted storage")
            }
        }
    }
}

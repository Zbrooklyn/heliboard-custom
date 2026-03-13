// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import helium314.keyboard.latin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val API_URL = "https://api.github.com/repos/Zbrooklyn/heliboard-custom/releases/latest"

    data class UpdateResult(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val downloadUrl: String?,
        val releaseNotes: String?
    )

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        val conn = URL(API_URL).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            if (conn.responseCode != 200) {
                return@withContext UpdateResult(false, BuildConfig.VERSION_NAME, null, null)
            }

            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tagName = json.optString("tag_name", "").removePrefix("v")
            val body = json.optString("body", null)

            // Find APK asset download URL
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }

            val hasUpdate = tagName.isNotEmpty() && isNewer(tagName, BuildConfig.VERSION_NAME)
            UpdateResult(hasUpdate, tagName, apkUrl, body)
        } catch (e: Exception) {
            Log.w("UpdateChecker", "Failed to check for updates: ${e.message}")
            UpdateResult(false, BuildConfig.VERSION_NAME, null, null)
        } finally {
            conn.disconnect()
        }
    }

    fun openDownload(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    /** Compare version strings like "1.2.3" — returns true if remote is newer than local. */
    private fun isNewer(remote: String, local: String): Boolean {
        // Strip any suffix after hyphen for comparison (e.g. "1.0.0-beta" → "1.0.0")
        val r = remote.split("-").first().split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split("-").first().split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}

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

    /**
     * Compare version strings like "3.7.1" or "3.7.0-beta".
     * Format: MAJOR.MINOR.PATCH with optional "-beta" suffix.
     * A beta is older than the same numeric version without it (3.7.0-beta < 3.7.0).
     * Local version may have git-describe noise (e.g. "3.7.0-beta-5-g02f9a06") — stripped.
     */
    private fun isNewer(remote: String, local: String): Boolean {
        val (rNums, rBeta) = parseVersion(remote)
        val (lNums, lBeta) = parseVersion(local)
        for (i in 0 until maxOf(rNums.size, lNums.size)) {
            val rv = rNums.getOrElse(i) { 0 }
            val lv = lNums.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        // Same numeric version: non-beta is newer than beta
        if (lBeta && !rBeta) return true
        return false
    }

    /** Parse "3.7.1-beta" → Pair([3,7,1], true). Strips git-describe suffixes. */
    private fun parseVersion(raw: String): Pair<List<Int>, Boolean> {
        val parts = raw.split("-")
        val nums = parts.first().split(".").map { it.toIntOrNull() ?: 0 }
        val isBeta = parts.size >= 2 && parts[1].equals("beta", ignoreCase = true)
        return Pair(nums, isBeta)
    }
}

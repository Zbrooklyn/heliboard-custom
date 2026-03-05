// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import org.json.JSONArray

fun getQuickTextSnippets(prefs: SharedPreferences): List<String> {
    val json = prefs.getString(Settings.PREF_QUICK_TEXT_SNIPPETS, Defaults.PREF_QUICK_TEXT_SNIPPETS)!!
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { array.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }
}

fun setQuickTextSnippets(prefs: SharedPreferences, snippets: List<String>) {
    val array = JSONArray()
    snippets.forEach { array.put(it) }
    prefs.edit { putString(Settings.PREF_QUICK_TEXT_SNIPPETS, array.toString()) }
}

fun getDefaultQuickTextSnippet(prefs: SharedPreferences): String? {
    val snippets = getQuickTextSnippets(prefs)
    return snippets.firstOrNull()
}

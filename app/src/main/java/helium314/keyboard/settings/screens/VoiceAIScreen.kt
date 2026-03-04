// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.preferences.ListPreference
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.TextInputPreference

@Composable
fun VoiceAIScreen(onClickBack: () -> Unit) {
    val prefs = LocalContext.current.prefs()
    // Force recomposition when prefs change
    val b = (LocalContext.current as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")

    val items = listOf(
        // Voice Input section
        R.string.voice_ai_category_voice,
        Settings.PREF_STT_MODE,
        Settings.PREF_AI_ACTIVE_MODEL,
        // AI Rewrite section
        R.string.voice_ai_category_rewrite,
        Settings.PREF_AI_PROVIDER,
        Settings.PREF_GEMINI_API_KEY,
        Settings.PREF_OPENAI_API_KEY,
    )

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_voice_ai),
        settings = items,
    )
}

fun createVoiceAISettings(context: Context) = listOf(
    Setting(context, Settings.PREF_STT_MODE, R.string.voice_ai_stt_mode, R.string.voice_ai_stt_mode_summary) {
        ListPreference(
            setting = it,
            items = listOf(
                context.getString(R.string.voice_ai_stt_local) to "local",
                context.getString(R.string.voice_ai_stt_cloud) to "cloud",
            ),
            default = Defaults.PREF_STT_MODE,
        )
    },
    Setting(context, Settings.PREF_AI_ACTIVE_MODEL, R.string.voice_ai_active_model, R.string.voice_ai_active_model_summary) {
        val models = helium314.keyboard.latin.voice.ModelManager.availableModels
        ListPreference(
            setting = it,
            items = models.map { m -> "${m.name} (${m.sizeMb}MB)" to m.fileName },
            default = helium314.keyboard.latin.voice.ModelManager.defaultModel.fileName,
        )
    },
    Setting(context, Settings.PREF_AI_PROVIDER, R.string.voice_ai_provider, R.string.voice_ai_provider_summary) {
        ListPreference(
            setting = it,
            items = listOf(
                "Gemini (Google)" to "gemini",
                "OpenAI (GPT)" to "openai",
            ),
            default = Defaults.PREF_AI_PROVIDER,
        )
    },
    Setting(context, Settings.PREF_GEMINI_API_KEY, R.string.voice_ai_gemini_key) {
        TextInputPreference(setting = it, default = Defaults.PREF_GEMINI_API_KEY)
    },
    Setting(context, Settings.PREF_OPENAI_API_KEY, R.string.voice_ai_openai_key) {
        TextInputPreference(setting = it, default = Defaults.PREF_OPENAI_API_KEY)
    },
)

// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.keyboard.KeyboardLayoutSet
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.getQuickTextSnippets
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.setQuickTextSnippets
import helium314.keyboard.settings.ExpandableSection
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.Theme
import helium314.keyboard.settings.dialogs.ThreeButtonAlertDialog
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.preferences.ListPreference
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.PreferenceCategory
import helium314.keyboard.settings.preferences.ReorderSwitchPreference
import helium314.keyboard.settings.preferences.SliderPreference
import helium314.keyboard.settings.preferences.SwitchPreference
import helium314.keyboard.settings.preferences.SwitchPreferenceWithEmojiDictWarning
import helium314.keyboard.settings.previewDark

private const val MAX_SNIPPETS = 20
private const val MAX_SNIPPET_LENGTH = 500

@Composable
fun PreferencesScreen(
    onClickBack: () -> Unit,
) {
    val prefs = LocalContext.current.prefs()
    val b = (LocalContext.current.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")

    val showHints = prefs.getBoolean(Settings.PREF_SHOW_HINTS, Defaults.PREF_SHOW_HINTS)
    val vibrateOn = prefs.getBoolean(Settings.PREF_VIBRATE_ON, Defaults.PREF_VIBRATE_ON)
    val soundOn = prefs.getBoolean(Settings.PREF_SOUND_ON, Defaults.PREF_SOUND_ON)
    val hasVibrator = AudioAndHapticFeedbackManager.getInstance().hasVibrator()
    val clipboardEnabled = prefs.getBoolean(Settings.PREF_ENABLE_CLIPBOARD_HISTORY, Defaults.PREF_ENABLE_CLIPBOARD_HISTORY)
    val numberRowOn = prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW)
    val hasLocalizedNumberRow = SubtypeSettings.getEnabledSubtypes(true).any { it.locale().language in localesWithLocalizedNumberRow }

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_preferences),
        settings = emptyList(),
        content = {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            ) { innerPadding ->
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .then(Modifier.padding(innerPadding))
                ) {
                    // === Main Settings ===

                    PreferenceCategory(stringResource(R.string.settings_category_input))
                    RenderSetting(Settings.PREF_SHOW_HINTS)
                    RenderSetting(Settings.PREF_POPUP_ON)
                    if (hasVibrator) RenderSetting(Settings.PREF_VIBRATE_ON)
                    AnimatedVisibility(visible = vibrateOn) {
                        Column { RenderSetting(Settings.PREF_VIBRATION_DURATION_SETTINGS) }
                    }
                    RenderSetting(Settings.PREF_SOUND_ON)
                    AnimatedVisibility(visible = soundOn) {
                        Column { RenderSetting(Settings.PREF_KEYPRESS_SOUND_VOLUME) }
                    }

                    PreferenceCategory(stringResource(R.string.settings_category_additional_keys))
                    RenderSetting(Settings.PREF_SHOW_NUMBER_ROW)
                    RenderSetting(Settings.PREF_SHOW_LANGUAGE_SWITCH_KEY)
                    RenderSetting(Settings.PREF_SHOW_EMOJI_KEY)

                    PreferenceCategory(stringResource(R.string.settings_category_clipboard_history))
                    RenderSetting(Settings.PREF_ENABLE_CLIPBOARD_HISTORY)
                    AnimatedVisibility(visible = clipboardEnabled) {
                        Column { RenderSetting(Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME) }
                    }
                    RenderSetting(Settings.PREF_QUICK_TEXT_SNIPPETS)

                    // === Collapsible Advanced ===

                    ExpandableSection {
                        AnimatedVisibility(visible = showHints) {
                            Column { RenderSetting(Settings.PREF_POPUP_KEYS_LABELS_ORDER) }
                        }
                        RenderSetting(Settings.PREF_POPUP_KEYS_ORDER)
                        RenderSetting(Settings.PREF_SHOW_POPUP_HINTS)
                        RenderSetting(Settings.PREF_SHOW_TLD_POPUP_KEYS)
                        AnimatedVisibility(visible = vibrateOn) {
                            Column { RenderSetting(Settings.PREF_VIBRATE_IN_DND_MODE) }
                        }
                        RenderSetting(Settings.PREF_SAVE_SUBTYPE_PER_APP)
                        RenderSetting(Settings.PREF_SHOW_EMOJI_DESCRIPTIONS)
                        AnimatedVisibility(visible = hasLocalizedNumberRow) {
                            Column { RenderSetting(Settings.PREF_LOCALIZED_NUMBER_ROW) }
                        }
                        AnimatedVisibility(visible = showHints && numberRowOn) {
                            Column { RenderSetting(Settings.PREF_SHOW_NUMBER_ROW_HINTS) }
                        }
                        AnimatedVisibility(visible = !numberRowOn) {
                            Column { RenderSetting(Settings.PREF_SHOW_NUMBER_ROW_IN_SYMBOLS) }
                        }
                        RenderSetting(Settings.PREF_LANGUAGE_SWITCH_KEY)
                        RenderSetting(Settings.PREF_REMOVE_REDUNDANT_POPUPS)
                        AnimatedVisibility(visible = clipboardEnabled) {
                            Column { RenderSetting(Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST) }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun RenderSetting(key: String) {
    SettingsActivity.settingsContainer[key]?.Preference()
}

// Quick Text composables (moved from ClipboardSettingsScreen)

@Composable
fun QuickTextSnippetsPreference(setting: Setting) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val snippets = remember(prefs.getString(Settings.PREF_QUICK_TEXT_SNIPPETS, Defaults.PREF_QUICK_TEXT_SNIPPETS)) {
        getQuickTextSnippets(prefs)
    }
    val description = if (snippets.isEmpty()) stringResource(R.string.quick_text_empty)
        else stringResource(R.string.quick_text_snippets_summary)

    Preference(
        name = setting.title,
        description = description,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        QuickTextEditDialog(
            snippets = snippets,
            onDismiss = { showDialog = false },
            onConfirm = { newSnippets ->
                val validated = newSnippets
                    .filter { it.isNotBlank() }
                    .map { it.take(MAX_SNIPPET_LENGTH) }
                    .take(MAX_SNIPPETS)
                setQuickTextSnippets(prefs, validated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun QuickTextEditDialog(
    snippets: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val editList = remember { snippets.toMutableStateList() }

    ThreeButtonAlertDialog(
        onDismissRequest = onDismiss,
        onConfirmed = { onConfirm(editList.toList()) },
        title = { Text(stringResource(R.string.quick_text_snippets)) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.quick_text_snippets_summary),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                editList.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { editList[index] = it.take(MAX_SNIPPET_LENGTH) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.quick_text_hint)) },
                            singleLine = true,
                        )
                        IconButton(onClick = { editList.removeAt(index) }) {
                            Icon(painterResource(R.drawable.ic_bin), contentDescription = null)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (editList.size < MAX_SNIPPETS) {
                    TextButton(
                        onClick = { editList.add("") },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.quick_text_add))
                    }
                }
            }
        },
    )
}

fun createPreferencesSettings(context: Context) = listOf(
    Setting(context, Settings.PREF_SAVE_SUBTYPE_PER_APP, R.string.save_subtype_per_app) {
        SwitchPreference(it, Defaults.PREF_SAVE_SUBTYPE_PER_APP)
    },
    Setting(context, Settings.PREF_SHOW_HINTS, R.string.show_hints, R.string.show_hints_summary) {
        SwitchPreference(it, Defaults.PREF_SHOW_HINTS) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_POPUP_KEYS_LABELS_ORDER, R.string.hint_source) {
        ReorderSwitchPreference(it, Defaults.PREF_POPUP_KEYS_LABELS_ORDER)
    },
    Setting(context, Settings.PREF_POPUP_KEYS_ORDER, R.string.popup_order) {
        ReorderSwitchPreference(it, Defaults.PREF_POPUP_KEYS_ORDER)
    },
    Setting(
        context, Settings.PREF_SHOW_TLD_POPUP_KEYS, R.string.show_tld_popup_keys,
        R.string.show_tld_popup_keys_summary
    ) {
        SwitchPreference(it, Defaults.PREF_SHOW_TLD_POPUP_KEYS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SHOW_POPUP_HINTS, R.string.show_popup_hints, R.string.show_popup_hints_summary) {
        SwitchPreference(it, Defaults.PREF_SHOW_POPUP_HINTS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_POPUP_ON, R.string.popup_on_keypress) {
        SwitchPreference(it, Defaults.PREF_POPUP_ON) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_VIBRATE_ON, R.string.vibrate_on_keypress) {
        SwitchPreference(it, Defaults.PREF_VIBRATE_ON)
    },
    Setting(context, Settings.PREF_VIBRATE_IN_DND_MODE, R.string.vibrate_in_dnd_mode) {
        SwitchPreference(it, Defaults.PREF_VIBRATE_IN_DND_MODE)
    },
    Setting(context, Settings.PREF_SOUND_ON, R.string.sound_on_keypress) {
        SwitchPreference(it, Defaults.PREF_SOUND_ON)
    },
    Setting(context, Settings.PREF_SHOW_EMOJI_DESCRIPTIONS, R.string.show_emoji_descriptions) {
        SwitchPreferenceWithEmojiDictWarning(it, Defaults.PREF_SHOW_EMOJI_DESCRIPTIONS)
    },
    Setting(context, Settings.PREF_SHOW_NUMBER_ROW, R.string.number_row, R.string.number_row_summary) {
        SwitchPreference(it, Defaults.PREF_SHOW_NUMBER_ROW) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SHOW_NUMBER_ROW_IN_SYMBOLS, R.string.number_row_in_symbols) {
        SwitchPreference(it, Defaults.PREF_SHOW_NUMBER_ROW_IN_SYMBOLS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_LOCALIZED_NUMBER_ROW, R.string.localized_number_row, R.string.localized_number_row_summary) {
        SwitchPreference(it, Defaults.PREF_LOCALIZED_NUMBER_ROW) {
            KeyboardLayoutSet.onSystemLocaleChanged()
            KeyboardSwitcher.getInstance().reloadKeyboard()
        }
    },
    Setting(context, Settings.PREF_SHOW_NUMBER_ROW_HINTS, R.string.number_row_hints) {
        SwitchPreference(it, Defaults.PREF_SHOW_NUMBER_ROW_HINTS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SHOW_LANGUAGE_SWITCH_KEY, R.string.show_language_switch_key) {
        SwitchPreference(it, Defaults.PREF_SHOW_LANGUAGE_SWITCH_KEY) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_LANGUAGE_SWITCH_KEY, R.string.language_switch_key_behavior) {
        ListPreference(
            it,
            listOf(
                stringResource(R.string.switch_language) to "internal",
                stringResource(R.string.language_switch_key_switch_input_method) to "input_method",
                stringResource(R.string.language_switch_key_switch_both) to "both"
            ),
            Defaults.PREF_LANGUAGE_SWITCH_KEY
        ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SHOW_EMOJI_KEY, R.string.show_emoji_key) {
        SwitchPreference(it, Defaults.PREF_SHOW_EMOJI_KEY) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_REMOVE_REDUNDANT_POPUPS,
        R.string.remove_redundant_popups, R.string.remove_redundant_popups_summary)
    {
        SwitchPreference(it, Defaults.PREF_REMOVE_REDUNDANT_POPUPS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_VIBRATION_DURATION_SETTINGS, R.string.prefs_keypress_vibration_duration_settings) { setting ->
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = Defaults.PREF_VIBRATION_DURATION_SETTINGS,
            description = {
                if (it < 0) stringResource(R.string.settings_system_default)
                else stringResource(R.string.abbreviation_unit_milliseconds, it.toString())
            },
            range = -1f..100f,
            onValueChanged = { it?.let { AudioAndHapticFeedbackManager.getInstance().vibrate(it.toLong()) } }
        )
    },
    Setting(context, Settings.PREF_KEYPRESS_SOUND_VOLUME, R.string.prefs_keypress_sound_volume_settings) { setting ->
        val audioManager = LocalContext.current.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = Defaults.PREF_KEYPRESS_SOUND_VOLUME,
            description = {
                if (it < 0) stringResource(R.string.settings_system_default)
                else (it * 100).toInt().toString()
            },
            range = -0.01f..1f,
            onValueChanged = { it?.let { audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, it) } }
        )
    },
    // Clipboard settings (moved from ClipboardSettingsScreen)
    Setting(context, Settings.PREF_ENABLE_CLIPBOARD_HISTORY,
        R.string.enable_clipboard_history, R.string.enable_clipboard_history_summary)
    {
        val ctx = LocalContext.current
        SwitchPreference(it, Defaults.PREF_ENABLE_CLIPBOARD_HISTORY) { ClipboardDao.getInstance(ctx)?.clearNonPinned() }
    },
    Setting(context, Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME, R.string.clipboard_history_retention_time) { setting ->
        val ctx = LocalContext.current
        val items = listOf(
            ctx.getString(R.string.clipboard_retention_10min) to 10,
            ctx.getString(R.string.clipboard_retention_1hour) to 60,
            ctx.getString(R.string.clipboard_retention_24hours) to 1440,
            ctx.getString(R.string.settings_no_limit) to 121,
        )
        ListPreference(setting, items, Defaults.PREF_CLIPBOARD_HISTORY_RETENTION_TIME) {
            ClipboardDao.getInstance(ctx)?.clearOldClips(true)
        }
    },
    Setting(context, Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST, R.string.clipboard_history_pinned_first) {
        SwitchPreference(it, Defaults.PREF_CLIPBOARD_HISTORY_PINNED_FIRST)
    },
    Setting(context, Settings.PREF_QUICK_TEXT_SNIPPETS, R.string.quick_text_snippets, R.string.quick_text_snippets_summary) {
        QuickTextSnippetsPreference(it)
    },
)

// todo (later): not good to have it hardcoded, but reading a bunch of files may be noticeably slow
private val localesWithLocalizedNumberRow = listOf("ar", "bn", "fa", "gu", "hi", "kn", "mr", "ne", "ur")

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            PreferencesScreen { }
        }
    }
}

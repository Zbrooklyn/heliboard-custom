// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.preferences.ReorderSwitchPreference

@Composable
fun ActionBarScreen(onClickBack: () -> Unit) {
    val items = listOf(
        Settings.PREF_CLIPBOARD_TOOLBAR_KEYS,
    )
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_action_bar),
        settings = items,
    )
}

fun createActionBarSettings(context: Context) = listOf(
    Setting(context, Settings.PREF_CLIPBOARD_TOOLBAR_KEYS, R.string.clipboard_toolbar_keys) {
        ReorderSwitchPreference(it, Defaults.PREF_CLIPBOARD_TOOLBAR_KEYS)
    },
)

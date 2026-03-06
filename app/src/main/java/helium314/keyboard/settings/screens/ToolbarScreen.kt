// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.graphics.drawable.VectorDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.Theme
import helium314.keyboard.settings.dialogs.ToolbarKeysCustomizer
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.ReorderSwitchPreference
import helium314.keyboard.settings.preferences.SwitchPreference
import helium314.keyboard.settings.previewDark

@Composable
fun ToolbarScreen(
    onClickBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val toolbarOn = prefs.getString(Settings.PREF_TOOLBAR_MODE, Defaults.PREF_TOOLBAR_MODE) != "HIDDEN"
    val actionBarOn = prefs.getBoolean(Settings.PREF_SHOW_ACTION_BAR, Defaults.PREF_SHOW_ACTION_BAR)
    val items = listOfNotNull(
        Settings.PREF_TOOLBAR_MODE,
        if (toolbarOn) Settings.PREF_TOOLBAR_KEYS else null,
        Settings.PREF_SHOW_ACTION_BAR,
        if (actionBarOn) Settings.PREF_CLIPBOARD_TOOLBAR_KEYS else null,
        Settings.PREF_TOOLBAR_CUSTOM_KEY_CODES,
    )
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_toolbar),
        settings = items
    )
}

fun createToolbarSettings(context: Context) = listOf(
    Setting(context, Settings.PREF_TOOLBAR_MODE, R.string.show_toolbar) {
        val ctx = LocalContext.current
        val prefs = ctx.prefs()
        val b = (ctx.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
        if ((b?.value ?: 0) < 0) { /* trigger recomposition */ }
        val isOn = prefs.getString(Settings.PREF_TOOLBAR_MODE, Defaults.PREF_TOOLBAR_MODE) != "HIDDEN"
        Preference(
            name = it.title,
            onClick = {
                val newMode = if (isOn) "HIDDEN" else "TOOLBAR_KEYS"
                prefs.edit { putString(Settings.PREF_TOOLBAR_MODE, newMode) }
            },
        ) {
            Switch(
                checked = isOn,
                onCheckedChange = { checked ->
                    val newMode = if (checked) "TOOLBAR_KEYS" else "HIDDEN"
                    prefs.edit { putString(Settings.PREF_TOOLBAR_MODE, newMode) }
                },
            )
        }
    },
    Setting(context, Settings.PREF_TOOLBAR_KEYS, R.string.toolbar_keys) {
        ReorderSwitchPreference(it, Defaults.PREF_TOOLBAR_KEYS)
    },
    Setting(context, Settings.PREF_SHOW_ACTION_BAR, R.string.show_action_bar) {
        SwitchPreference(it, Defaults.PREF_SHOW_ACTION_BAR)
    },
    Setting(context, Settings.PREF_CLIPBOARD_TOOLBAR_KEYS, R.string.clipboard_toolbar_keys) {
        ReorderSwitchPreference(it, Defaults.PREF_CLIPBOARD_TOOLBAR_KEYS)
    },
    Setting(context, Settings.PREF_TOOLBAR_CUSTOM_KEY_CODES, R.string.customize_toolbar_key_codes) {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        Preference(
            name = it.title,
            onClick = { showDialog = true },
        )
        if (showDialog)
            ToolbarKeysCustomizer(
                key = it.key,
                onDismissRequest = { showDialog = false }
            )
    },
)

@Composable
fun KeyboardIconsSet.GetIcon(name: String?) {
    val ctx = LocalContext.current
    val drawable = getNewDrawable(name, ctx)
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        if (drawable is VectorDrawable)
            Icon(painterResource(iconIds[name?.lowercase()]!!), name, Modifier.fillMaxSize(0.8f))
        else if (drawable != null) {
            val px = with(LocalDensity.current) { 40.dp.toPx() }.toInt()
            Icon(drawable.toBitmap(px, px).asImageBitmap(), name, Modifier.fillMaxSize(0.8f))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            ToolbarScreen { }
        }
    }
}

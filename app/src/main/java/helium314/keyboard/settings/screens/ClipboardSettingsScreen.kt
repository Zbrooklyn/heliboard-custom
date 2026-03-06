// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.getQuickTextSnippets
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.setQuickTextSnippets
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.dialogs.ThreeButtonAlertDialog
import helium314.keyboard.settings.preferences.ListPreference
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.SwitchPreference

@Composable
fun ClipboardSettingsScreen(onClickBack: () -> Unit) {
    val prefs = LocalContext.current.prefs()
    val b = (LocalContext.current as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")

    val clipboardHistoryEnabled = prefs.getBoolean(Settings.PREF_ENABLE_CLIPBOARD_HISTORY, Defaults.PREF_ENABLE_CLIPBOARD_HISTORY)
    val items = listOf(
        Settings.PREF_ENABLE_CLIPBOARD_HISTORY,
        if (clipboardHistoryEnabled) Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME else null,
        if (clipboardHistoryEnabled) Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST else null,
        R.string.settings_category_quick_text,
        Settings.PREF_QUICK_TEXT_SNIPPETS,
    )
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_clipboard),
        settings = items,
    )
}

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
                setQuickTextSnippets(prefs, newSnippets.filter { it.isNotBlank() })
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
                            onValueChange = { editList[index] = it },
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
                TextButton(
                    onClick = { editList.add("") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.quick_text_add))
                }
            }
        },
    )
}

fun createClipboardSettings(context: Context) = listOf(
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

// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.Setting
import androidx.core.content.edit

/**
 * Inline text field for API keys — masked by default with an eye toggle.
 * Saves to SharedPreferences on each keystroke.
 */
@Composable
fun SecretTextInputPreference(setting: Setting, default: String) {
    val prefs = LocalContext.current.prefs()
    val focusManager = LocalFocusManager.current
    var text by rememberSaveable { mutableStateOf(prefs.getString(setting.key, default) ?: "") }
    var revealed by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                prefs.edit { putString(setting.key, it) }
            },
            label = { Text(setting.title) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (revealed) VisualTransformation.None
                else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            trailingIcon = {
                IconButton(onClick = { revealed = !revealed }) {
                    Text(
                        text = if (revealed) "HIDE" else "SHOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

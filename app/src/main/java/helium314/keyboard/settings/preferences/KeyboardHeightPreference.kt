// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.createPrefKeyForBooleanSettings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.WithSmallTitle
import helium314.keyboard.settings.dialogs.ThreeButtonAlertDialog
import androidx.core.content.edit

private data class HeightPreset(
    val labelResId: Int,
    val scale: Float
)

private val PRESETS = listOf(
    HeightPreset(R.string.height_preset_compact, 0.8f),
    HeightPreset(R.string.height_preset_normal, 1.0f),
    HeightPreset(R.string.height_preset_large, 1.2f),
)

private const val CUSTOM_TOLERANCE = 0.01f

/** Samsung-style keyboard height preference with preset chips + fine-tune slider. */
@Composable
fun KeyboardHeightPreference(setting: Setting) {
    var showDialog by remember { mutableStateOf(false) }
    Preference(
        name = setting.title,
        onClick = { showDialog = true },
    )
    if (showDialog) {
        KeyboardHeightDialog(
            onDismissRequest = { showDialog = false },
            onDone = { KeyboardSwitcher.getInstance().setThemeNeedsReload() },
        )
    }
}

@Composable
private fun KeyboardHeightDialog(
    onDismissRequest: () -> Unit,
    onDone: () -> Unit,
) {
    val prefs = LocalContext.current.prefs()
    val baseKey = Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX
    val defaults = Defaults.PREF_KEYBOARD_HEIGHT_SCALE
    val dimensions = listOf(stringResource(R.string.landscape))

    // Build keys for portrait (index 0) and landscape (index 1)
    val keys = List(2) { createPrefKeyForBooleanSettings(baseKey, it, 1) }
    val variants = listOf("", stringResource(R.string.landscape))

    var checked by remember { mutableStateOf(listOf(true, true)) }
    val sliderPositions = remember {
        Array(2) { mutableFloatStateOf(prefs.getFloat(keys[it], defaults[it])) }
    }

    val done = {
        for (i in 0..1) {
            val value = sliderPositions[i].floatValue
            if (value == defaults[i])
                prefs.edit { remove(keys[i]) }
            else
                prefs.edit { putFloat(keys[i], value) }
        }
        onDone()
    }

    ThreeButtonAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmed = { done() },
        title = { Text(stringResource(R.string.prefs_keyboard_height_scale)) },
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyLarge
            ) {
                val state = rememberScrollState()
                Column(Modifier.verticalScroll(state)) {
                    // Landscape dimension checkbox
                    if (dimensions.size >= 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checked = listOf(checked[0], !checked[1]) }
                        ) {
                            Checkbox(
                                checked = checked[1],
                                onCheckedChange = { checked = listOf(checked[0], it) }
                            )
                            Text(dimensions[0])
                        }
                    }

                    // For each variant (portrait, landscape)
                    variants.forEachIndexed { i, variant ->
                        val visible = if (i == 0) true else checked[1]

                        AnimatedVisibility(visible, exit = fadeOut(), enter = fadeIn()) {
                            Column {
                                WithSmallTitle(variant.ifEmpty { stringResource(R.string.button_default) }) {
                                    // Preset chips row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PRESETS.forEach { preset ->
                                            val currentValue = sliderPositions[i].floatValue
                                            val isSelected = kotlin.math.abs(currentValue - preset.scale) < CUSTOM_TOLERANCE
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { sliderPositions[i].floatValue = preset.scale },
                                                label = {
                                                    Text(stringResource(preset.labelResId))
                                                },
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Fine-tune slider
                                    Slider(
                                        value = sliderPositions[i].floatValue,
                                        onValueChange = { sliderPositions[i].floatValue = it },
                                        valueRange = 0.3f..1.5f,
                                    )

                                    // Value display + default button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val pct = (100 * sliderPositions[i].floatValue).toInt()
                                        val label = PRESETS.find {
                                            kotlin.math.abs(sliderPositions[i].floatValue - it.scale) < CUSTOM_TOLERANCE
                                        }?.let { stringResource(it.labelResId) }
                                            ?: stringResource(R.string.height_preset_custom)
                                        Text("$pct% ($label)")
                                        TextButton({ sliderPositions[i].floatValue = defaults[i] }) {
                                            Text(stringResource(R.string.button_default))
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

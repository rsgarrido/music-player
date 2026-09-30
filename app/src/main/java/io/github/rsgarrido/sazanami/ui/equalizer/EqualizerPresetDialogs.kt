package io.github.rsgarrido.sazanami.ui.equalizer

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.player.equalizer.GraphicEqualizerPresets
import io.github.rsgarrido.sazanami.player.equalizer.UserEqualizerPreset
import io.github.rsgarrido.sazanami.player.equalizer.normalizePresetName

@Composable
internal fun EqualizerPresetSelectorDialog(
    userPresets: List<UserEqualizerPreset>,
    onDismiss: () -> Unit,
    onApplyBuiltIn: (Int) -> Unit,
    onApplyUser: (String) -> Unit,
    onSaveAs: () -> Unit,
    onRename: (UserEqualizerPreset) -> Unit,
    onDelete: (UserEqualizerPreset) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.eq_choose_preset_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(
                    rememberScrollState()
                )
            ) {
                Text(stringResource(R.string.eq_builtin_presets))
                builtInEqualizerPresets.forEachIndexed {
                        index,
                        preset ->
                    ListItem(
                        headlineContent = { Text(stringResource(preset.id.labelRes)) },
                        supportingContent = {
                            Text(
                                preset.bandGainsDb.joinToString(
                                    separator = "  "
                                ) { gain ->
                                    formatEqualizerDb(gain)
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            onApplyBuiltIn(index)
                            onDismiss()
                        }
                    )
                }
                if (userPresets.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.eq_user_presets),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                userPresets.forEach { preset ->
                    ListItem(
                        headlineContent = { Text(preset.name) },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = { onRename(preset) }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription =
                                            stringResource(R.string.eq_rename_named_preset, preset.name)
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(preset) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription =
                                            stringResource(R.string.eq_delete_named_preset, preset.name)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            onApplyUser(preset.id)
                            onDismiss()
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.eq_close))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onSaveAs()
                }
            ) {
                Text(stringResource(R.string.eq_save_as_preset))
            }
        }
    )
}

@Composable
internal fun EqualizerPresetNameDialog(
    title: String,
    initialName: String,
    userPresets: List<UserEqualizerPreset>,
    excludingPresetId: String? = null,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) {
        mutableStateOf(initialName)
    }
    val validationError = presetNameValidationError(
        name = name,
        userPresets = userPresets,
        excludingPresetId = excludingPresetId
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { updated -> name = updated },
                label = { Text(stringResource(R.string.eq_preset_name)) },
                supportingText = validationError?.let { message ->
                    { Text(stringResource(message)) }
                },
                isError = validationError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.eq_cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = validationError == null,
                onClick = {
                    onConfirm(normalizePresetName(name))
                }
            ) {
                Text(confirmText)
            }
        }
    )
}

@Composable
internal fun ConfirmEqualizerActionDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.eq_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        }
    )
}

@StringRes
internal fun presetNameValidationError(
    name: String,
    userPresets: List<UserEqualizerPreset>,
    excludingPresetId: String? = null
): Int? {
    val normalized = name.trim()
    if (normalized.isBlank()) return R.string.eq_name_blank
    if (normalized.length > 40) {
        return R.string.eq_name_too_long
    }
    if (
        normalized.lowercase() in
        GraphicEqualizerPresets.builtInNamesLowercase
    ) {
        return R.string.eq_name_builtin
    }
    if (
        userPresets.any { preset ->
            preset.id != excludingPresetId &&
                preset.name.equals(normalized, ignoreCase = true)
        }
    ) {
        return R.string.eq_name_duplicate
    }
    return null
}

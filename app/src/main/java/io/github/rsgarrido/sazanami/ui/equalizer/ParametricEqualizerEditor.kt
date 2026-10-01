package io.github.rsgarrido.sazanami.ui.equalizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MAX_PARAMETRIC_FILTER_COUNT
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MAX_PARAMETRIC_FREQUENCY_HZ
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MAX_PARAMETRIC_GAIN_DB
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MAX_PARAMETRIC_Q
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MAX_PARAMETRIC_SHELF_SLOPE
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MIN_PARAMETRIC_FREQUENCY_HZ
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MIN_PARAMETRIC_GAIN_DB
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MIN_PARAMETRIC_Q
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MIN_PARAMETRIC_SHELF_SLOPE
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricEqualizerPreset
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricFilter
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricFilterFactory
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricFilterType
import io.github.rsgarrido.sazanami.player.equalizer.parametric.changeType
import io.github.rsgarrido.sazanami.player.equalizer.parametric.gainDbOrNull
import io.github.rsgarrido.sazanami.player.equalizer.parametric.qOrNull
import io.github.rsgarrido.sazanami.player.equalizer.parametric.slopeOrNull
import io.github.rsgarrido.sazanami.player.equalizer.parametric.withEnabled
import io.github.rsgarrido.sazanami.player.equalizer.parametric.withFrequencyHz
import io.github.rsgarrido.sazanami.player.equalizer.parametric.withGainDb
import io.github.rsgarrido.sazanami.player.equalizer.parametric.withQ
import io.github.rsgarrido.sazanami.player.equalizer.parametric.withShelfSlope
import java.util.Locale

@Composable
internal fun ParametricEqualizerEditor(
    state: EqualizerScreenState,
    actions: EqualizerUiActions
) {
    val resources = LocalResources.current
    var presetDialogVisible by remember { mutableStateOf(false) }
    var saveNameDialogVisible by remember { mutableStateOf(false) }
    var renamePreset by remember {
        mutableStateOf<ParametricEqualizerPreset?>(null)
    }
    var deletePreset by remember {
        mutableStateOf<ParametricEqualizerPreset?>(null)
    }
    var editOriginal by remember {
        mutableStateOf<ParametricFilter?>(null)
    }
    var deleteFilter by remember {
        mutableStateOf<ParametricFilter?>(null)
    }
    val parametric = state.editablePreferences.parametricState
    val presetLabel = state.presetMatch.localizedLabel()

    ListItem(
        headlineContent = { Text(stringResource(R.string.eq_parametric_preset)) },
        supportingContent = { Text(presetLabel) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { presetDialogVisible = true }
            .semantics {
                contentDescription =
                    resources.getString(R.string.eq_parametric_preset_description, presetLabel)
            }
    )
    TextButton(
        onClick = { presetDialogVisible = true },
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Text(stringResource(R.string.eq_manage_presets))
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Button(
            onClick = actions.onAddParametricFilter,
            enabled =
                parametric.filters.size < MAX_PARAMETRIC_FILTER_COUNT,
            modifier = Modifier.semantics {
                contentDescription = resources.getString(R.string.eq_add_parametric_filter_description)
            }
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.eq_add_filter))
        }
        Text(
            pluralStringResource(R.plurals.eq_filter_capacity, parametric.filters.size, parametric.filters.size, MAX_PARAMETRIC_FILTER_COUNT),
            style = MaterialTheme.typography.bodyMedium
        )
    }
    if (parametric.filters.size == MAX_PARAMETRIC_FILTER_COUNT) {
        Text(
            stringResource(R.string.eq_max_filters_reached),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    if (parametric.filters.isEmpty()) {
        Text(
            stringResource(R.string.eq_no_parametric_filters),
            modifier = Modifier.padding(16.dp)
        )
    }
    parametric.filters.forEachIndexed { index, filter ->
        ParametricFilterCard(
            index = index,
            filter = filter,
            selected = filter.id == state.selectedParametricFilterId,
            unavailable = index in state.analysis.ignoredFilterIndices,
            canMoveUp = index > 0,
            canMoveDown = index < parametric.filters.lastIndex,
            onSelect = { actions.onSelectParametricFilter(filter.id) },
            onToggle = { enabled ->
                actions.onCommitParametricFilter(
                    filter.withEnabled(enabled)
                )
            },
            onMoveUp = {
                actions.onMoveParametricFilter(filter.id, index - 1)
            },
            onMoveDown = {
                actions.onMoveParametricFilter(filter.id, index + 1)
            },
            onEdit = {
                actions.onSelectParametricFilter(filter.id)
                editOriginal = filter
            },
            onDelete = { deleteFilter = filter }
        )
    }

    if (presetDialogVisible) {
        ParametricPresetSelectorDialog(
            userPresets = state.parametricUserPresets,
            onDismiss = { presetDialogVisible = false },
            onApplyFlat = actions.onApplyParametricFlatPreset,
            onApplyUser = actions.onApplyParametricUserPreset,
            onSaveAs = { saveNameDialogVisible = true },
            onRename = { renamePreset = it },
            onDelete = { deletePreset = it }
        )
    }
    if (saveNameDialogVisible) {
        ParametricPresetNameDialog(
            title = stringResource(R.string.eq_save_parametric_preset),
            initialName = "",
            presets = state.parametricUserPresets,
            confirmText = stringResource(R.string.eq_save),
            onDismiss = { saveNameDialogVisible = false },
            onConfirm = {
                actions.onSaveParametricUserPreset(it)
                saveNameDialogVisible = false
            }
        )
    }
    renamePreset?.let { preset ->
        ParametricPresetNameDialog(
            title = stringResource(R.string.eq_rename_parametric_preset),
            initialName = preset.name,
            presets = state.parametricUserPresets,
            excludingPresetId = preset.id,
            confirmText = stringResource(R.string.eq_rename),
            onDismiss = { renamePreset = null },
            onConfirm = {
                actions.onRenameParametricUserPreset(preset.id, it)
                renamePreset = null
            }
        )
    }
    deletePreset?.let { preset ->
        ConfirmEqualizerActionDialog(
            title = stringResource(R.string.eq_delete_preset_confirm, preset.name),
            message = stringResource(R.string.eq_delete_parametric_preset_help),
            confirmText = stringResource(R.string.eq_delete),
            onDismiss = { deletePreset = null },
            onConfirm = {
                actions.onDeleteParametricUserPreset(preset.id)
                deletePreset = null
            }
        )
    }
    deleteFilter?.let { filter ->
        ConfirmEqualizerActionDialog(
            title = stringResource(R.string.eq_delete_filter_confirm, stringResource(filter.type.labelRes)),
            message = stringResource(R.string.eq_delete_filter_help),
            confirmText = stringResource(R.string.eq_delete),
            onDismiss = { deleteFilter = null },
            onConfirm = {
                actions.onDeleteParametricFilter(filter.id)
                deleteFilter = null
            }
        )
    }
    editOriginal?.let { original ->
        ParametricFilterEditorDialog(
            original = original,
            unavailable =
                parametric.filters.indexOfFirst { it.id == original.id } in
                    state.analysis.ignoredFilterIndices,
            onPreview = actions.onPreviewParametricFilter,
            onCancel = {
                actions.onCancelParametricFilterPreview(original)
                editOriginal = null
            },
            onApply = {
                actions.onCommitParametricFilter(it)
                editOriginal = null
            }
        )
    }
}

@Composable
private fun ParametricFilterCard(
    index: Int,
    filter: ParametricFilter,
    selected: Boolean,
    unavailable: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val resources = LocalResources.current
    val description = filterAccessibilityDescription(
        index, filter, unavailable
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect)
            .semantics {
                this.selected = selected
                contentDescription = description
            }
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (selected) R.string.eq_filter_row_selected else R.string.eq_filter_row, index + 1, stringResource(filter.type.labelRes)),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(filterParameterSummary(filter))
                    if (unavailable) {
                        Text(
                            stringResource(R.string.eq_unavailable_current_source),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (!filter.enabled) {
                        Text(
                            stringResource(R.string.eq_bypassed),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Switch(
                    checked = filter.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics {
                        contentDescription =
                            resources.getString(R.string.eq_filter_enabled_description, index + 1)
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = stringResource(R.string.eq_move_filter_up, index + 1)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = stringResource(R.string.eq_move_filter_down, index + 1)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.eq_edit_filter, index + 1)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.eq_delete_filter, index + 1)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ParametricFilterEditorDialog(
    original: ParametricFilter,
    unavailable: Boolean,
    onPreview: (ParametricFilter) -> Unit,
    onCancel: () -> Unit,
    onApply: (ParametricFilter) -> Unit
) {
    val resources = LocalResources.current
    var draft by remember(original) { mutableStateOf(original) }
    var typeMenuVisible by remember { mutableStateOf(false) }
    var frequencyText by remember(original) {
        mutableStateOf(formatEditable(original.frequencyHz, 1))
    }
    var gainText by remember(original) {
        mutableStateOf(
            original.gainDbOrNull?.let { formatEditable(it, 1) } ?: ""
        )
    }
    var qText by remember(original) {
        mutableStateOf(
            original.qOrNull?.let { formatEditable(it, 2) } ?: ""
        )
    }
    var slopeText by remember(original) {
        mutableStateOf(
            original.slopeOrNull?.let { formatEditable(it, 2) } ?: ""
        )
    }

    fun update(candidate: ParametricFilter) {
        draft = candidate
        onPreview(candidate)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.eq_edit_filter_type, stringResource(draft.type.labelRes))) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.eq_enabled_label), Modifier.weight(1f))
                    Checkbox(
                        checked = draft.enabled,
                        onCheckedChange = {
                            update(draft.withEnabled(it))
                        },
                        modifier = Modifier.semantics {
                            contentDescription = resources.getString(R.string.eq_filter_enabled)
                        }
                    )
                }
                OutlinedButton(
                    onClick = { typeMenuVisible = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                resources.getString(R.string.eq_filter_type_description, resources.getString(draft.type.labelRes))
                        }
                ) {
                    Text(stringResource(R.string.eq_filter_type, stringResource(draft.type.labelRes)))
                }
                DropdownMenu(
                    expanded = typeMenuVisible,
                    onDismissRequest = { typeMenuVisible = false }
                ) {
                    ParametricFilterType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(type.labelRes)) },
                            onClick = {
                                val changed = draft.changeType(type)
                                update(changed)
                                frequencyText =
                                    formatEditable(changed.frequencyHz, 1)
                                gainText = changed.gainDbOrNull
                                    ?.let { formatEditable(it, 1) } ?: ""
                                qText = changed.qOrNull
                                    ?.let { formatEditable(it, 2) } ?: ""
                                slopeText = changed.slopeOrNull
                                    ?.let { formatEditable(it, 2) } ?: ""
                                typeMenuVisible = false
                            }
                        )
                    }
                }
                ParametricNumberField(
                    label = stringResource(R.string.eq_frequency),
                    value = frequencyText,
                    range = stringResource(R.string.eq_frequency_range),
                    step = frequencyStep(draft.frequencyHz),
                    minimum = MIN_PARAMETRIC_FREQUENCY_HZ,
                    maximum = MAX_PARAMETRIC_FREQUENCY_HZ,
                    decimals = 1,
                    onValueChanged = { text, value ->
                        frequencyText = text
                        value?.let {
                            update(draft.withFrequencyHz(it))
                        }
                    }
                )
                draft.gainDbOrNull?.let {
                    ParametricNumberField(
                        label = stringResource(R.string.eq_gain),
                        value = gainText,
                        range = stringResource(R.string.eq_gain_range),
                        step = 0.1,
                        minimum = MIN_PARAMETRIC_GAIN_DB,
                        maximum = MAX_PARAMETRIC_GAIN_DB,
                        decimals = 1,
                        onValueChanged = { text, value ->
                            gainText = text
                            value?.let {
                                update(draft.withGainDb(it))
                            }
                        }
                    )
                }
                draft.qOrNull?.let {
                    ParametricNumberField(
                        label = stringResource(R.string.eq_q),
                        value = qText,
                        range = stringResource(R.string.eq_q_range),
                        step = 0.01,
                        minimum = MIN_PARAMETRIC_Q,
                        maximum = MAX_PARAMETRIC_Q,
                        decimals = 2,
                        onValueChanged = { text, value ->
                            qText = text
                            value?.let { update(draft.withQ(it)) }
                        }
                    )
                }
                draft.slopeOrNull?.let {
                    ParametricNumberField(
                        label = stringResource(R.string.eq_shelf_slope),
                        value = slopeText,
                        range = stringResource(R.string.eq_shelf_slope_range),
                        step = 0.01,
                        minimum = MIN_PARAMETRIC_SHELF_SLOPE,
                        maximum = MAX_PARAMETRIC_SHELF_SLOPE,
                        decimals = 2,
                        onValueChanged = { text, value ->
                            slopeText = text
                            value?.let {
                                update(draft.withShelfSlope(it))
                            }
                        }
                    )
                }
                if (unavailable) {
                    Text(
                        stringResource(R.string.eq_frequency_unavailable_help),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(
                    onClick = {
                        val reset = ParametricFilterFactory.default(
                            draft.type,
                            draft.id
                        ).withEnabled(draft.enabled)
                        update(reset)
                        frequencyText = formatEditable(reset.frequencyHz, 1)
                        gainText = reset.gainDbOrNull
                            ?.let { formatEditable(it, 1) } ?: ""
                        qText = reset.qOrNull
                            ?.let { formatEditable(it, 2) } ?: ""
                        slopeText = reset.slopeOrNull
                            ?.let { formatEditable(it, 2) } ?: ""
                    }
                ) {
                    Text(stringResource(R.string.eq_reset_parameters))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.eq_cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(draft) },
                enabled =
                    parseInRange(
                        frequencyText,
                        MIN_PARAMETRIC_FREQUENCY_HZ,
                        MAX_PARAMETRIC_FREQUENCY_HZ
                    ) != null &&
                        (draft.gainDbOrNull == null ||
                            parseInRange(
                                gainText,
                                MIN_PARAMETRIC_GAIN_DB,
                                MAX_PARAMETRIC_GAIN_DB
                            ) != null) &&
                        (draft.qOrNull == null ||
                            parseInRange(
                                qText,
                                MIN_PARAMETRIC_Q,
                                MAX_PARAMETRIC_Q
                            ) != null) &&
                        (draft.slopeOrNull == null ||
                            parseInRange(
                                slopeText,
                                MIN_PARAMETRIC_SHELF_SLOPE,
                                MAX_PARAMETRIC_SHELF_SLOPE
                            ) != null)
            ) {
                Text(stringResource(R.string.eq_apply))
            }
        }
    )
}

@Composable
private fun ParametricNumberField(
    label: String,
    value: String,
    range: String,
    step: Double,
    minimum: Double,
    maximum: Double,
    decimals: Int,
    onValueChanged: (String, Double?) -> Unit
) {
    val parsed = parseInRange(value, minimum, maximum)
    val decreaseDescription = stringResource(R.string.eq_decrease_parameter, label)
    val increaseDescription = stringResource(R.string.eq_increase_parameter, label)
    Column(Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    val next = ((parsed ?: minimum) - step)
                        .coerceIn(minimum, maximum)
                    onValueChanged(
                        formatEditable(next, decimals),
                        next
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = decreaseDescription
                }
            ) { Text(stringResource(R.string.eq_decrease_symbol)) }
            OutlinedTextField(
                value = value,
                onValueChange = { text ->
                    onValueChanged(
                        text,
                        parseInRange(text, minimum, maximum)
                    )
                },
                label = { Text(label) },
                supportingText = {
                    Text(if (parsed == null) stringResource(R.string.eq_enter_range, range) else range)
                },
                isError = parsed == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    val next = ((parsed ?: minimum) + step)
                        .coerceIn(minimum, maximum)
                    onValueChanged(
                        formatEditable(next, decimals),
                        next
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = increaseDescription
                }
            ) { Text(stringResource(R.string.eq_increase_symbol)) }
        }
    }
}

@Composable
private fun ParametricPresetSelectorDialog(
    userPresets: List<ParametricEqualizerPreset>,
    onDismiss: () -> Unit,
    onApplyFlat: () -> Unit,
    onApplyUser: (String) -> Unit,
    onSaveAs: () -> Unit,
    onRename: (ParametricEqualizerPreset) -> Unit,
    onDelete: (ParametricEqualizerPreset) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.eq_choose_parametric_preset)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.eq_flat)) },
                    supportingContent = { Text(stringResource(R.string.eq_flat_no_filters)) },
                    modifier = Modifier.clickable {
                        onApplyFlat()
                        onDismiss()
                    }
                )
                userPresets.forEach { preset ->
                    ListItem(
                        headlineContent = { Text(preset.name) },
                        supportingContent = {
                            Text(pluralStringResource(R.plurals.eq_filter_count, preset.filters.size, preset.filters.size))
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onRename(preset) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        stringResource(R.string.eq_rename_named_preset, preset.name)
                                    )
                                }
                                IconButton(onClick = { onDelete(preset) }) {
                                    Icon(
                                        Icons.Default.Delete,
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.eq_close)) }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onSaveAs()
                }
            ) { Text(stringResource(R.string.eq_save_as_preset)) }
        }
    )
}

@Composable
private fun ParametricPresetNameDialog(
    title: String,
    initialName: String,
    presets: List<ParametricEqualizerPreset>,
    excludingPresetId: String? = null,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val normalized = name.trim()
    val errorRes = when {
        normalized.isBlank() -> R.string.eq_name_blank
        normalized.length > 40 -> R.string.eq_name_too_long
        normalized.equals("Flat", ignoreCase = true) ->
            R.string.eq_flat_builtin
        presets.any {
            it.id != excludingPresetId &&
                it.name.equals(normalized, ignoreCase = true)
        } -> R.string.eq_name_duplicate
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.eq_preset_name)) },
                supportingText = errorRes?.let { message ->
                    { Text(stringResource(message)) }
                },
                isError = errorRes != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.eq_cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(normalized) },
                enabled = errorRes == null
            ) { Text(confirmText) }
        }
    )
}

internal val ParametricFilterType.labelRes: Int
    get() = when (this) {
        ParametricFilterType.PEAKING -> R.string.eq_type_peaking
        ParametricFilterType.LOW_SHELF -> R.string.eq_type_low_shelf
        ParametricFilterType.HIGH_SHELF -> R.string.eq_type_high_shelf
        ParametricFilterType.LOW_PASS -> R.string.eq_type_low_pass
        ParametricFilterType.HIGH_PASS -> R.string.eq_type_high_pass
        ParametricFilterType.NOTCH -> R.string.eq_type_notch
        ParametricFilterType.BAND_PASS -> R.string.eq_type_band_pass
    }

internal fun filterParameterSummary(filter: ParametricFilter): String =
    buildString {
        append(formatEqualizerFrequency(filter.frequencyHz))
        filter.gainDbOrNull?.let {
            append(" · ")
            append(formatEqualizerDb(it))
        }
        filter.qOrNull?.let {
            append(" · Q ")
            append(formatEditable(it, 2))
        }
        filter.slopeOrNull?.let {
            append(" · S ")
            append(formatEditable(it, 2))
        }
    }

@Composable
private fun filterAccessibilityDescription(
    index: Int,
    filter: ParametricFilter,
    unavailable: Boolean
): String = stringResource(
    if (unavailable) R.string.eq_filter_access_unavailable else R.string.eq_filter_access,
    index + 1,
    stringResource(filter.type.labelRes),
    stringResource(if (filter.enabled) R.string.eq_enabled_label else R.string.eq_bypassed),
    filterParameterSummary(filter)
)

private fun frequencyStep(frequencyHz: Double): Double = when {
    frequencyHz < 100.0 -> 1.0
    frequencyHz < 1_000.0 -> 10.0
    frequencyHz < 10_000.0 -> 100.0
    else -> 1_000.0
}

private fun parseInRange(
    text: String,
    minimum: Double,
    maximum: Double
): Double? = text.trim()
    .replace(',', '.')
    .toDoubleOrNull()
    ?.takeIf { it.isFinite() && it in minimum..maximum }

private fun formatEditable(value: Double, decimals: Int): String =
    String.format(Locale.ROOT, "%.${decimals}f", value)

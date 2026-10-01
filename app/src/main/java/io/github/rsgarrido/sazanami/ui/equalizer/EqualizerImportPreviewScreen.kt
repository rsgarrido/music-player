package io.github.rsgarrido.sazanami.ui.equalizer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import io.github.rsgarrido.sazanami.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.ui.state.resolve
import io.github.rsgarrido.sazanami.player.equalizer.interchange.EqualizerProfileDiagnostic
import io.github.rsgarrido.sazanami.player.equalizer.interchange.EqualizerProfileDiagnosticCode
import io.github.rsgarrido.sazanami.player.equalizer.interchange.EqualizerProfileDiagnosticSeverity
import io.github.rsgarrido.sazanami.player.equalizer.interchange.EqualizerProfileFormat
import io.github.rsgarrido.sazanami.player.equalizer.interchange.ImportedFilterDeclaration
import io.github.rsgarrido.sazanami.player.equalizer.interchange.ImportedFilterStatus
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricEqualizerPresets
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricFilter
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricFilterFactory
import io.github.rsgarrido.sazanami.player.equalizer.parametric.gainDbOrNull
import io.github.rsgarrido.sazanami.player.equalizer.parametric.qOrNull
import io.github.rsgarrido.sazanami.player.equalizer.parametric.slopeOrNull
import java.util.Locale

@Composable
internal fun EqualizerImportPreviewScreen(
    state: EqualizerScreenState,
    actions: EqualizerUiActions,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val preview = state.importPreview ?: return
    var editingLine by remember { mutableStateOf<Int?>(null) }
    var replaceConfirmationVisible by remember {
        mutableStateOf(false)
    }
    var supportedOnlyConfirmationVisible by remember {
        mutableStateOf(false)
    }
    val nameIsValid = remember(
        preview.proposedName,
        state.parametricUserPresets
    ) {
        runCatching {
            ParametricEqualizerPresets.requireNameAvailable(
                preview.proposedName,
                state.parametricUserPresets
            )
        }.isSuccess
    }
    val currentParametricIsFlat =
        state.durablePreferences.parametricState.let {
            it.preampDb == 0.0 &&
                it.automaticHeadroomEnabled &&
                it.filters.isEmpty()
        }

    BackHandler(onBack = actions.onDismissImportPreview)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("equalizer_import_preview")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = actions.onDismissImportPreview) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.eq_cancel_import)
                )
            }
            Text(
                stringResource(R.string.eq_import_parametric),
                style = MaterialTheme.typography.titleLarge
            )
        }

        ImportSummary(preview)

        OutlinedTextField(
            value = preview.proposedName,
            onValueChange = { name ->
                actions.onUpdateImportPreview {
                    it.copy(proposedName = name.take(40))
                }
            },
            label = { Text(stringResource(R.string.eq_preset_name)) },
            supportingText = {
                Text(
                    if (nameIsValid) {
                        stringResource(R.string.eq_name_saving_only)
                    } else {
                        stringResource(R.string.eq_name_unique_range)
                    }
                )
            },
            isError = !nameIsValid,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .semantics {
                    contentDescription =
                        resources.getString(R.string.eq_imported_preset_name_description, preview.proposedName)
                }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.eq_automatic_headroom))
                Text(
                    if (preview.automaticHeadroomEnabled) {
                        stringResource(R.string.eq_headroom_on_recommended)
                    } else {
                        stringResource(R.string.eq_headroom_off_exact)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = preview.automaticHeadroomEnabled,
                onCheckedChange = { enabled ->
                    actions.onUpdateImportPreview {
                        it.copy(
                            automaticHeadroomEnabled = enabled
                        )
                    }
                },
                modifier = Modifier.semantics {
                    contentDescription =
                        resources.getString(R.string.eq_imported_headroom_description)
                }
            )
        }

        if (state.runtimeState.sampleRateHz != null) {
            Text(
                stringResource(R.string.eq_preview_sample_rate),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            ) {
                FilterChip(
                    selected = preview.previewAtCurrentTrackRate,
                    onClick = {
                        actions.onUpdateImportPreview {
                            it.copy(previewAtCurrentTrackRate = true)
                        }
                    },
                    label = { Text(stringResource(R.string.eq_current_track)) }
                )
                FilterChip(
                    selected = !preview.previewAtCurrentTrackRate,
                    onClick = {
                        actions.onUpdateImportPreview {
                            it.copy(previewAtCurrentTrackRate = false)
                        }
                    },
                    label = { Text(stringResource(R.string.eq_48khz)) }
                )
            }
        }

        EqualizerResponseGraph(
            analysis = state.importAnalysis,
            modifier = Modifier.padding(16.dp)
        )
        ImportAnalysisSummary(state)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    actions.onUpdateImportPreview {
                        it.selectFirstTen()
                    }
                }
            ) {
                Text(stringResource(R.string.eq_select_first_ten))
            }
            TextButton(
                onClick = {
                    actions.onUpdateImportPreview {
                        it.clearSelection()
                    }
                }
            ) {
                Text(stringResource(R.string.eq_clear_selection))
            }
        }
        Text(
            stringResource(R.string.eq_import_selection_help),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        )

        preview.parseResult.declarations.forEach { declaration ->
            ImportedFilterRow(
                declaration = declaration,
                selected = declaration.sourceLineNumber in
                    preview.selectedSourceLines,
                selectionLimitReached =
                    preview.selectedFilters.size >= 10,
                onSelectionChanged = {
                    actions.onUpdateImportPreview {
                        it.toggleSelection(
                            declaration.sourceLineNumber
                        )
                    }
                },
                onEdit = {
                    editingLine = declaration.sourceLineNumber
                }
            )
        }

        ImportSafetyConfirmations(
            preview = preview,
            onUpdate = actions.onUpdateImportPreview,
            onRequestSupportedOnly = {
                supportedOnlyConfirmationVisible = true
            }
        )
        if (state.importInProgress) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics {
                        contentDescription =
                            resources.getString(R.string.eq_saving_imported_profile)
                    }
            )
        }
        state.importMessage?.let { message ->
            Text(
                message.resolve(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Text(
            stringResource(R.string.eq_import_destination),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp
            )
        )
        Text(
            stringResource(R.string.eq_import_destination_help),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Button(
            onClick = {
                if (currentParametricIsFlat) {
                    actions.onReplaceWithImportedProfile()
                } else {
                    replaceConfirmationVisible = true
                }
            },
            enabled = preview.canApply && !state.importInProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics {
                    contentDescription =
                        resources.getString(R.string.eq_replace_current)
                }
        ) {
            Text(stringResource(R.string.eq_replace_current))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    actions.onSaveImportedProfile(false)
                },
                enabled = preview.canApply &&
                    nameIsValid &&
                    !state.importInProgress,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.eq_save_as_preset))
            }
            OutlinedButton(
                onClick = {
                    actions.onSaveImportedProfile(true)
                },
                enabled = preview.canApply &&
                    nameIsValid &&
                    !state.importInProgress,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.eq_save_and_apply))
            }
        }
        TextButton(
            onClick = actions.onDismissImportPreview,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(stringResource(R.string.eq_cancel))
        }
        Spacer(Modifier.height(24.dp))
    }

    editingLine?.let { lineNumber ->
        val declaration = preview.parseResult.declarations.first {
            it.sourceLineNumber == lineNumber
        }
        val initial = declaration.mappedFilter
            ?: ParametricFilterFactory.default()
        ParametricFilterEditorDialog(
            original = initial,
            unavailable = false,
            onPreview = {},
            onCancel = { editingLine = null },
            onApply = { filter ->
                actions.onUpdateImportPreview {
                    it.replaceDeclaration(lineNumber, filter)
                }
                editingLine = null
            }
        )
    }
    if (supportedOnlyConfirmationVisible) {
        AlertDialog(
            onDismissRequest = {
                supportedOnlyConfirmationVisible = false
            },
            title = { Text(stringResource(R.string.eq_supported_only_confirm)) },
            text = {
                Text(
                    stringResource(R.string.eq_supported_only_warning)
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        supportedOnlyConfirmationVisible = false
                    }
                ) {
                    Text(stringResource(R.string.eq_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.onUpdateImportPreview {
                            it.copy(
                                supportedOnlyOverrideConfirmed = true
                            )
                        }
                        supportedOnlyConfirmationVisible = false
                    }
                ) {
                    Text(stringResource(R.string.eq_import_supported_only))
                }
            }
        )
    }
    if (replaceConfirmationVisible) {
        AlertDialog(
            onDismissRequest = {
                replaceConfirmationVisible = false
            },
            title = { Text(stringResource(R.string.eq_replace_confirm)) },
            text = {
                Text(
                    stringResource(R.string.eq_replace_help)
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        replaceConfirmationVisible = false
                    }
                ) {
                    Text(stringResource(R.string.eq_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        replaceConfirmationVisible = false
                        actions.onReplaceWithImportedProfile()
                    }
                ) {
                    Text(stringResource(R.string.eq_replace))
                }
            }
        )
    }
}

@Composable
private fun ImportSummary(
    preview: EqualizerImportPreviewState
) {
    val result = preview.parseResult
    val resources = LocalResources.current
    val formatLabel = formatName(result.detectedFormat)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.eq_source, result.sourceName ?: stringResource(R.string.eq_clipboard_source)),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                formatLabel,
                modifier = Modifier.semantics {
                    contentDescription =
                        resources.getString(R.string.eq_detected_format_description, formatLabel)
                }
            )
            Text(
                stringResource(R.string.eq_import_preamp, formatEqualizerDb(result.preampDb ?: 0.0))
            )
            Text(
                stringResource(R.string.eq_import_filter_selection, result.declarations.size, preview.selectedFilters.size),
                modifier = Modifier.semantics {
                    contentDescription =
                        resources.getString(R.string.eq_import_filter_selection_description, preview.selectedFilters.size, result.declarations.size)
                }
            )
            Text(
                stringResource(R.string.eq_import_issue_counts, result.warningCount, result.errorCount),
                color = if (result.errorCount > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.semantics {
                    contentDescription =
                        resources.getString(R.string.eq_import_issue_counts_description, result.warningCount, result.errorCount)
                }
            )
        }
    }
}

@Composable
private fun ImportAnalysisSummary(state: EqualizerScreenState) {
    val analysis = state.importAnalysis
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.eq_preview_analysis),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(R.string.eq_predicted_maximum, formatEqualizerDb(analysis.predictedMaximumDb))
            )
            Text(
                stringResource(R.string.eq_automatic_attenuation, formatEqualizerDb(
                        analysis.automaticHeadroom.attenuationDb,
                        includePlus = false
                    ))
            )
            Text(
                stringResource(R.string.eq_effective_preamp, formatEqualizerDb(
                        analysis.automaticHeadroom.effectivePreampDb
                    ))
            )
            Text(stringResource(R.string.eq_sample_rate, analysis.sampleRateHz))
            if (analysis.ignoredFilterIndices.isNotEmpty()) {
                Text(
                    pluralStringResource(R.plurals.eq_import_unavailable_filters, analysis.ignoredFilterIndices.size, analysis.ignoredFilterIndices.size),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ImportedFilterRow(
    declaration: ImportedFilterDeclaration,
    selected: Boolean,
    selectionLimitReached: Boolean,
    onSelectionChanged: () -> Unit,
    onEdit: () -> Unit
) {
    val filter = declaration.mappedFilter
    val status = stringResource(declaration.status.labelRes)
    val resources = LocalResources.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription =
                    resources.getString(if (selected) R.string.eq_source_line_selected_description else R.string.eq_source_line_unselected_description, declaration.sourceLineNumber, status)
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onSelectionChanged() },
                enabled = filter != null &&
                    (!selectionLimitReached || selected),
                modifier = Modifier.semantics {
                    contentDescription =
                        resources.getString(R.string.eq_select_source_line, declaration.sourceLineNumber)
                }
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.eq_source_line_status, declaration.sourceLineNumber, status),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    filter?.summary()
                        ?: declaration.originalText.take(160),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onEdit) {
                Text(
                    if (filter == null) {
                        stringResource(R.string.eq_replace_supported_filter)
                    } else {
                        stringResource(R.string.eq_edit)
                    }
                )
            }
        }
        declaration.diagnostics.forEach { diagnostic ->
            DiagnosticText(diagnostic)
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun DiagnosticText(
    diagnostic: EqualizerProfileDiagnostic
) {
    val severity = stringResource(diagnostic.severity.labelRes)
    val detail = stringResource(diagnostic.code.labelRes)
    Text(
        if (diagnostic.lineNumber == null) stringResource(R.string.eq_diagnostic, severity, detail)
        else stringResource(R.string.eq_diagnostic_line, diagnostic.lineNumber, severity, detail),
        color = if (
            diagnostic.severity.name in setOf("ERROR", "BLOCKING")
        ) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ImportSafetyConfirmations(
    preview: EqualizerImportPreviewState,
    onUpdate:
        ((EqualizerImportPreviewState) ->
            EqualizerImportPreviewState) -> Unit,
    onRequestSupportedOnly: () -> Unit
) {
    if (preview.hasOverrideableSemanticBlocks) {
        SafetyCheckbox(
            checked = preview.supportedOnlyOverrideConfirmed,
            text = stringResource(R.string.eq_supported_only_checkbox),
            description = stringResource(R.string.eq_supported_only_description),
            onCheckedChange = { checked ->
                if (checked) {
                    onRequestSupportedOnly()
                } else {
                    onUpdate {
                        it.copy(
                            supportedOnlyOverrideConfirmed = false
                        )
                    }
                }
            }
        )
    }
    if (preview.hasUnsupportedDeclarations) {
        SafetyCheckbox(
            checked = preview.unsupportedExclusionConfirmed,
            text = stringResource(R.string.eq_exclude_unsupported_checkbox),
            description = stringResource(R.string.eq_exclude_unsupported_description),
            onCheckedChange = { checked ->
                onUpdate {
                    it.copy(
                        unsupportedExclusionConfirmed = checked
                    )
                }
            }
        )
    }
    if (preview.hasUnrecognizedText) {
        SafetyCheckbox(
            checked = preview.unrecognizedTextConfirmed,
            text = stringResource(R.string.eq_reviewed_unrecognized_checkbox),
            description = stringResource(R.string.eq_reviewed_unrecognized_description),
            onCheckedChange = { checked ->
                onUpdate {
                    it.copy(unrecognizedTextConfirmed = checked)
                }
            }
        )
    }
    if (preview.selectedFilters.isEmpty()) {
        SafetyCheckbox(
            checked = preview.flatImportConfirmed,
            text = stringResource(R.string.eq_flat_import_checkbox),
            description = stringResource(R.string.eq_flat_import_description),
            onCheckedChange = { checked ->
                onUpdate {
                    it.copy(flatImportConfirmed = checked)
                }
            }
        )
    }
    preview.parseResult.diagnostics.forEach { diagnostic ->
        DiagnosticText(diagnostic)
    }
}

@Composable
private fun SafetyCheckbox(
    checked: Boolean,
    text: String,
    description: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics {
                contentDescription = description
            }
        )
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ParametricFilter.summary(): String {
    val enabledLabel = stringResource(if (enabled) R.string.eq_on else R.string.eq_off)
    val typeLabel = stringResource(type.labelRes)
    val parameters = buildList {
        add(enabledLabel)
        add(typeLabel)
        add(formatFrequency(frequencyHz))
        gainDbOrNull?.let { add(formatEqualizerDb(it)) }
        qOrNull?.let {
            add(String.format(Locale.ROOT, "Q %.2f", it))
        }
        slopeOrNull?.let {
            add(String.format(Locale.ROOT, "S %.2f", it))
        }
    }
    return parameters.joinToString(" · ")
}

private fun formatFrequency(value: Double): String =
    String.format(Locale.ROOT, "%.1f Hz", value)

@Composable
private fun formatName(format: EqualizerProfileFormat): String =
    when (format) {
        EqualizerProfileFormat.AUTOEQ_PARAMETRIC_TEXT ->
            stringResource(R.string.eq_format_autoeq)
        EqualizerProfileFormat.EQUALIZER_APO_SUBSET ->
            stringResource(R.string.eq_format_apo)
        EqualizerProfileFormat.SAZANAMI_PARAMETRIC_PRESET_JSON ->
            stringResource(R.string.eq_format_native)
    }

private val ImportedFilterStatus.labelRes: Int
    get() = when (this) {
        ImportedFilterStatus.VALID -> R.string.eq_import_status_valid
        ImportedFilterStatus.INVALID -> R.string.eq_import_status_invalid
        ImportedFilterStatus.UNSUPPORTED -> R.string.eq_import_status_unsupported
    }

private val EqualizerProfileDiagnosticSeverity.labelRes: Int
    get() = when (this) {
        EqualizerProfileDiagnosticSeverity.INFO -> R.string.eq_severity_info
        EqualizerProfileDiagnosticSeverity.WARNING -> R.string.eq_severity_warning
        EqualizerProfileDiagnosticSeverity.ERROR -> R.string.eq_severity_error
        EqualizerProfileDiagnosticSeverity.BLOCKING -> R.string.eq_severity_blocking
    }

private val EqualizerProfileDiagnosticCode.labelRes: Int
    get() = when (this) {
        EqualizerProfileDiagnosticCode.DECIMAL_COMMA_NORMALIZED -> R.string.eq_diag_decimal_comma
        EqualizerProfileDiagnosticCode.MULTIPLE_PREAMPS_COMBINED -> R.string.eq_diag_multiple_preamps
        EqualizerProfileDiagnosticCode.UNSUPPORTED_COMMAND -> R.string.eq_diag_unsupported_command
        EqualizerProfileDiagnosticCode.UNSUPPORTED_FILTER_TYPE -> R.string.eq_diag_unsupported_filter_type
        EqualizerProfileDiagnosticCode.MISSING_PARAMETER -> R.string.eq_diag_missing_parameter
        EqualizerProfileDiagnosticCode.DUPLICATE_PARAMETER -> R.string.eq_diag_duplicate_parameter
        EqualizerProfileDiagnosticCode.OUT_OF_RANGE -> R.string.eq_diag_out_of_range
        EqualizerProfileDiagnosticCode.FILTER_LIMIT_EXCEEDED -> R.string.eq_diag_filter_limit
        EqualizerProfileDiagnosticCode.UNKNOWN_COMMAND -> R.string.eq_diag_unknown_command
        EqualizerProfileDiagnosticCode.MALFORMED_FILTER -> R.string.eq_diag_malformed_filter
        EqualizerProfileDiagnosticCode.SHELF_ROUNDING_NORMALIZED -> R.string.eq_diag_shelf_rounding
        EqualizerProfileDiagnosticCode.UNRECOGNIZED_TEXT -> R.string.eq_diag_unrecognized_text
        EqualizerProfileDiagnosticCode.INPUT_TOO_LARGE -> R.string.eq_diag_input_too_large
        EqualizerProfileDiagnosticCode.TOO_MANY_LINES -> R.string.eq_diag_too_many_lines
        EqualizerProfileDiagnosticCode.LINE_TOO_LONG -> R.string.eq_diag_line_too_long
        EqualizerProfileDiagnosticCode.TOO_MANY_DECLARATIONS -> R.string.eq_diag_too_many_declarations
        EqualizerProfileDiagnosticCode.NUL_CHARACTER -> R.string.eq_diag_nul_character
        EqualizerProfileDiagnosticCode.INVALID_UNIT -> R.string.eq_diag_invalid_unit
        EqualizerProfileDiagnosticCode.EXTRA_PARAMETER -> R.string.eq_diag_extra_parameter
        EqualizerProfileDiagnosticCode.INVALID_NUMBER -> R.string.eq_diag_invalid_number
        EqualizerProfileDiagnosticCode.INVALID_PREAMP -> R.string.eq_diag_invalid_preamp
        EqualizerProfileDiagnosticCode.INVALID_NATIVE_FILE -> R.string.eq_diag_invalid_native_file
        EqualizerProfileDiagnosticCode.UNSUPPORTED_NATIVE_VERSION -> R.string.eq_diag_unsupported_native_version
    }

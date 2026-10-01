package io.github.rsgarrido.sazanami.ui.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.controller.SmartPlaylistUiData
import io.github.rsgarrido.sazanami.data.SmartPlaylistDefinition
import io.github.rsgarrido.sazanami.data.SmartPlaylistDraft
import io.github.rsgarrido.sazanami.data.SmartPlaylistMatchMode
import io.github.rsgarrido.sazanami.data.SmartPlaylistOperator
import io.github.rsgarrido.sazanami.data.SmartPlaylistResolution
import io.github.rsgarrido.sazanami.data.SmartPlaylistRuleField
import io.github.rsgarrido.sazanami.data.SmartPlaylistSortDirection
import io.github.rsgarrido.sazanami.data.SmartPlaylistTemplate
import io.github.rsgarrido.sazanami.data.UNKNOWN_GENRE_NAME
import kotlinx.coroutines.delay

@Immutable
data class SmartPlaylistUiEnvironment(
    val availableGenres: List<String> = emptyList(),
    val onPreview: (SmartPlaylistDraft, (Result<SmartPlaylistResolution>) -> Unit) -> Unit = { _, _ -> },
    val onCreate: (
        String,
        SmartPlaylistDraft,
        Long?,
        SmartPlaylistTemplate?,
        (Result<SmartPlaylistDefinition>) -> Unit
    ) -> Unit = { _, _, _, _, _ -> },
    val onUpdate: (Long, SmartPlaylistDraft, (Result<SmartPlaylistDefinition>) -> Unit) -> Unit =
        { _, _, _ -> },
    val onLoad: (Long, (Result<SmartPlaylistUiData>) -> Unit) -> Unit = { _, _ -> },
    val onRefresh: (Long, (Result<SmartPlaylistResolution>) -> Unit) -> Unit = { _, _ -> },
    val onResolve: (Long, (Result<SmartPlaylistResolution>) -> Unit) -> Unit = { _, _ -> }
)

val LocalSmartPlaylistUi = compositionLocalOf { SmartPlaylistUiEnvironment() }

data class SmartPlaylistEditorRequest(
    val folderId: Long?,
    val playlistId: Long? = null,
    val originalName: String? = null,
    val model: SmartPlaylistEditorModel = SmartPlaylistEditorModel(),
    val template: SmartPlaylistTemplate? = null
)

@Composable
fun PlaylistCreationChooserDialog(
    onDismiss: () -> Unit,
    onManual: () -> Unit,
    onSmart: (SmartPlaylistTemplate?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
        title = { Text(stringResource(R.string.smart_new_playlist)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.smart_manual_playlist))
                }
                Button(onClick = { onSmart(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Text(stringResource(R.string.smart_playlist), modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    stringResource(R.string.smart_playlist_ideas),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 6.dp)
                )
                SmartPlaylistTemplate.entries.forEach { template ->
                    TextButton(
                        onClick = { onSmart(template) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(template.nameRes), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(template.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.smart_cancel)) } }
    )
}

@Composable
fun SmartPlaylistEditor(
    request: SmartPlaylistEditorRequest,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSaved: (name: String) -> Unit
) {
    val smartUi = LocalSmartPlaylistUi.current
    var model by remember(request) { mutableStateOf(request.model) }
    var nextRuleId by remember(request) {
        mutableLongStateOf((model.rules.maxOfOrNull(SmartPlaylistEditorRule::id) ?: 0L) + 1L)
    }
    var preview by remember(request) { mutableStateOf<SmartPlaylistResolution?>(null) }
    var previewError by remember(request) { mutableStateOf<Int?>(null) }
    var saveError by remember(request) { mutableStateOf<Int?>(null) }
    var saving by remember(request) { mutableStateOf(false) }
    var fieldSelectorRuleId by remember(request) { mutableStateOf<Long?>(null) }
    var genreSelectorRuleId by remember(request) { mutableStateOf<Long?>(null) }
    val validation = model.validation(existingNames, request.originalName)
    val previewValidationError = validation.generalError ?: validation.ruleErrors.values.firstOrNull()
    val canPreview = previewValidationError == null
    val definitionReadOnly = request.template != null

    LaunchedEffect(model, canPreview) {
        preview = null
        previewError = null
        if (!canPreview) return@LaunchedEffect
        delay(350L)
        smartUi.onPreview(model.toDraft()) { result ->
            result.onSuccess { preview = it }
                .onFailure { previewError = smartEditorError(it) }
        }
    }

    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.smart_close_editor))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(if (request.playlistId == null) R.string.smart_new_smart_playlist else R.string.smart_edit_smart_playlist),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        request.template?.let {
                            Text(stringResource(R.string.smart_template_selected, stringResource(it.nameRes)), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextButton(
                        enabled = validation.isValid && !saving,
                        onClick = {
                            saving = true
                            saveError = null
                            val completion: (Result<SmartPlaylistDefinition>) -> Unit = { result ->
                                saving = false
                                result.onSuccess { onSaved(model.name.trim()) }
                                    .onFailure { saveError = smartEditorError(it) }
                            }
                            request.playlistId?.let { id ->
                                smartUi.onUpdate(id, model.toDraft(), completion)
                            } ?: smartUi.onCreate(
                                model.name.trim(),
                                model.toDraft(),
                                request.folderId,
                                request.template,
                                completion
                            )
                        }
                    ) { Text(stringResource(if (saving) R.string.smart_saving else R.string.smart_save)) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = model.name,
                            onValueChange = { model = model.copy(name = it) },
                            label = { Text(stringResource(R.string.smart_name)) },
                            isError = validation.nameError != null,
                            supportingText = validation.nameError?.let { error -> { Text(stringResource(error)) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text(
                            stringResource(R.string.smart_rules),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (model.showsMatchModeChoice) item {
                        Text(stringResource(R.string.smart_songs_must_match), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                SmartPlaylistMatchMode.ALL to R.string.smart_all_conditions,
                                SmartPlaylistMatchMode.ANY to R.string.smart_any_condition
                            ).forEach { (mode, labelRes) ->
                                FilterChip(
                                    selected = model.matchMode == mode,
                                    onClick = { if (!definitionReadOnly) model = model.copy(matchMode = mode) },
                                    enabled = !definitionReadOnly,
                                    label = { Text(stringResource(labelRes)) }
                                )
                            }
                        }
                        Text(
                            stringResource(if (model.matchMode == SmartPlaylistMatchMode.ALL)
                                R.string.smart_every_condition else R.string.smart_at_least_one_condition),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(model.rules, key = SmartPlaylistEditorRule::id) { rule ->
                        SmartRuleCard(
                            rule = rule,
                            error = validation.ruleErrors[rule.id],
                            readOnly = definitionReadOnly,
                            onChooseField = { fieldSelectorRuleId = rule.id },
                            onChooseGenre = { genreSelectorRuleId = rule.id },
                            onChange = { changed ->
                                model = model.copy(rules = model.rules.map {
                                    if (it.id == changed.id) changed else it
                                })
                            },
                            onRemove = {
                                model = model.copy(rules = model.rules.filterNot { it.id == rule.id })
                            }
                        )
                    }
                    if (!definitionReadOnly) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    model = model.copy(
                                        rules = model.rules + SmartPlaylistEditorRule(nextRuleId++)
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text(stringResource(R.string.smart_add_rule), modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                    item {
                        Text(stringResource(R.string.smart_results), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.smart_sort_field),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        CompactChoiceRow(
                            selected = model.sortField,
                            options = smartSortOptions,
                            enabled = !definitionReadOnly,
                            onSelected = { model = model.copy(sortField = it) }
                        )
                        Text(
                            stringResource(R.string.smart_direction),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        CompactChoiceRow(
                            selected = model.sortDirection,
                            options = listOf(
                                SmartPlaylistSortDirection.ASCENDING to R.string.smart_sort_ascending,
                                SmartPlaylistSortDirection.DESCENDING to R.string.smart_sort_descending
                            ),
                            enabled = !definitionReadOnly,
                            onSelected = { model = model.copy(sortDirection = it) }
                        )
                        OutlinedTextField(
                            value = model.resultLimit,
                            onValueChange = { if (!definitionReadOnly) model = model.copy(resultLimit = it) },
                            enabled = !definitionReadOnly,
                            label = { Text(stringResource(R.string.smart_limit_optional)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                    item {
                        PreviewCard(
                            preview = preview,
                            error = previewError,
                            validationError = previewValidationError,
                            usesRecentHistory = model.rules.any { rule ->
                                rule.field == SmartPlaylistRuleField.RECENT_PLAY_COUNT ||
                                    rule.field == SmartPlaylistRuleField.LAST_PLAYED
                            }
                        )
                    }
                    saveError?.let { error ->
                        item { Text(stringResource(error), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    fieldSelectorRuleId?.let { ruleId ->
        FieldSelectorSheet(
            selected = model.rules.firstOrNull { it.id == ruleId }?.field,
            onDismiss = { fieldSelectorRuleId = null },
            onSelected = { selected ->
                model = model.copy(rules = model.rules.map { rule ->
                    if (rule.id == ruleId) changeSmartRuleField(rule, selected) else rule
                })
                fieldSelectorRuleId = null
            }
        )
    }
    genreSelectorRuleId?.let { ruleId ->
        GenreSelectorSheet(
            genres = smartUi.availableGenres,
            selected = model.rules.firstOrNull { it.id == ruleId }?.value,
            onDismiss = { genreSelectorRuleId = null },
            onSelected = { selected ->
                model = model.copy(rules = model.rules.map { rule ->
                    if (rule.id == ruleId) rule.copy(value = selected) else rule
                })
                genreSelectorRuleId = null
            }
        )
    }
}

@Composable
private fun SmartRuleCard(
    rule: SmartPlaylistEditorRule,
    error: Int?,
    readOnly: Boolean,
    onChooseField: () -> Unit,
    onChooseGenre: () -> Unit,
    onChange: (SmartPlaylistEditorRule) -> Unit,
    onRemove: () -> Unit
) {
    val field = smartRuleFieldOptions.firstOrNull { it.storage == rule.field }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = onChooseField,
                    enabled = !readOnly
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.smart_field_label), style = MaterialTheme.typography.labelSmall)
                            Text(field?.let { stringResource(it.labelRes) }
                                ?: stringResource(R.string.smart_unsupported_value, rule.field), maxLines = 1)
                        }
                        if (!readOnly) Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.smart_choose_rule_field)
                        )
                    }
                }
                if (!readOnly) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.smart_remove_rule))
                    }
                }
            }
            if (field == null) {
                Text(stringResource(R.string.smart_unsupported_field, rule.field), color = MaterialTheme.colorScheme.error)
            } else {
                CompactChoiceRow(
                    selected = rule.operator,
                    options = field.operators.map { it.storage to it.labelRes },
                    enabled = !readOnly,
                    onSelected = { onChange(rule.copy(operator = it)) },
                )
                RuleValueInput(rule, field.valueKind, readOnly, onChooseGenre, onChange)
                Text(
                    naturalRuleText(rule),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun RuleValueInput(
    rule: SmartPlaylistEditorRule,
    kind: SmartRuleValueKind,
    readOnly: Boolean,
    onChooseGenre: () -> Unit,
    onChange: (SmartPlaylistEditorRule) -> Unit
) {
    if (rule.operator == SmartPlaylistOperator.UNRATED ||
        rule.operator == SmartPlaylistOperator.NEVER || kind == SmartRuleValueKind.NONE
    ) return
    if (kind == SmartRuleValueKind.GENRE) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            onClick = onChooseGenre,
            enabled = !readOnly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    rule.value.ifBlank { stringResource(R.string.smart_choose_genre) },
                    modifier = Modifier.weight(1f),
                    color = if (rule.value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                )
                if (!readOnly) Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.smart_choose_genre)
                )
            }
        }
        return
    }
    if (kind == SmartRuleValueKind.RATING) {
        SelectionMenu(
            label = stringResource(R.string.smart_stars),
            selected = rule.value,
            options = (1..5).map { it.toString() to "★".repeat(it) },
            enabled = !readOnly,
            onSelected = { onChange(rule.copy(value = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        return
    }
    val keyboardType = when (kind) {
        SmartRuleValueKind.TEXT,
        SmartRuleValueKind.GENRE -> KeyboardType.Text
        SmartRuleValueKind.DURATION_MINUTES -> KeyboardType.Decimal
        else -> KeyboardType.Number
    }
    OutlinedTextField(
        value = rule.value,
        onValueChange = { if (!readOnly) onChange(rule.copy(value = it)) },
        enabled = !readOnly,
        label = {
            Text(when {
                rule.field == SmartPlaylistRuleField.YEAR -> stringResource(R.string.smart_field_year)
                rule.field == SmartPlaylistRuleField.BPM -> stringResource(R.string.smart_field_bpm)
                kind == SmartRuleValueKind.DURATION_MINUTES -> stringResource(R.string.smart_minutes)
                kind == SmartRuleValueKind.RELATIVE_DAYS -> stringResource(R.string.smart_days)
                else -> stringResource(R.string.smart_value)
            })
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    if (kind == SmartRuleValueKind.DURATION_MINUTES &&
        rule.operator == SmartPlaylistOperator.ABOUT
    ) {
        Text(
            stringResource(R.string.smart_nearest_minute_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (rule.operator == SmartPlaylistOperator.BETWEEN) {
        OutlinedTextField(
            value = rule.secondValue,
            onValueChange = { if (!readOnly) onChange(rule.copy(secondValue = it)) },
            enabled = !readOnly,
            label = {
                Text(stringResource(if (kind == SmartRuleValueKind.DURATION_MINUTES)
                    R.string.smart_and_minutes else R.string.smart_and))
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (kind == SmartRuleValueKind.RECENT_COUNT) {
        OutlinedTextField(
            value = rule.windowDays,
            onValueChange = { if (!readOnly) onChange(rule.copy(windowDays = it)) },
            enabled = !readOnly,
            label = { Text(stringResource(R.string.smart_within_last_days)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CompactChoiceRow(
    selected: String,
    options: List<Pair<String, Int>>,
    enabled: Boolean,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (storage, labelRes) ->
            FilterChip(
                selected = selected == storage,
                onClick = { onSelected(storage) },
                enabled = enabled,
                label = { Text(stringResource(labelRes), maxLines = 1) }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FieldSelectorSheet(
    selected: String?,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(R.string.smart_choose_field),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                SmartRuleFieldGroup.entries.forEach { group ->
                    item(key = "field-group-${group.name}") {
                        Text(
                            stringResource(group.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        smartRuleFieldOptions.filter { it.group == group },
                        key = SmartRuleFieldOption::storage
                    ) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelected(option.storage) }
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(option.labelRes), modifier = Modifier.weight(1f))
                            if (option.storage == selected) {
                                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.smart_selected))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GenreSelectorSheet(
    genres: List<String>,
    selected: String?,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val options = remember(genres) {
        (genres + UNKNOWN_GENRE_NAME).distinctBy { it.lowercase() }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 10.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.smart_choose_genre),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                items(options, key = { it.lowercase() }) { genre ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(genre) }
                            .padding(horizontal = 20.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(genre, modifier = Modifier.weight(1f))
                        if (genre.equals(selected, ignoreCase = true)) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.smart_selected))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionMenu(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    enabled: Boolean,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second
        ?: stringResource(R.string.smart_unsupported_value, selected)
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(selectedLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (storage, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelected(storage)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(
    preview: SmartPlaylistResolution?,
    error: Int?,
    validationError: Int?,
    usesRecentHistory: Boolean
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.smart_matches), style = MaterialTheme.typography.titleMedium)
            when {
                validationError != null -> Text(stringResource(validationError), color = MaterialTheme.colorScheme.error)
                error != null -> Text(stringResource(error), color = MaterialTheme.colorScheme.error)
                preview == null -> Text(stringResource(R.string.smart_checking_matches))
                preview.songs.isEmpty() -> Text(
                    if (usesRecentHistory) {
                        stringResource(R.string.smart_no_recent_matches)
                    } else {
                        pluralStringResource(R.plurals.smart_song_matches, 0, 0)
                    }
                )
                else -> {
                    Text(pluralStringResource(R.plurals.smart_song_matches, preview.count, preview.count))
                    preview.songs.take(5).forEach { song ->
                        Text(stringResource(R.string.smart_preview_song, song.title, song.artist), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (preview.count > 5) Text(stringResource(R.string.smart_and_more, preview.count - 5))
                }
            }
        }
    }
}

@Composable
internal fun naturalRuleText(rule: SmartPlaylistEditorRule): String {
    val field = smartRuleFieldOptions.firstOrNull { it.storage == rule.field }
        ?.let { stringResource(it.labelRes) } ?: rule.field
    val operator = smartRuleFieldOptions.firstOrNull { it.storage == rule.field }
        ?.operators?.firstOrNull { it.storage == rule.operator }
        ?.let { stringResource(it.labelRes) } ?: rule.operator
    return when {
        rule.field == SmartPlaylistRuleField.RATING && rule.operator != SmartPlaylistOperator.UNRATED ->
            stringResource(R.string.smart_rule_summary_value, field, operator,
                "★".repeat(rule.value.toIntOrNull() ?: 0))
        rule.operator == SmartPlaylistOperator.UNRATED || rule.operator == SmartPlaylistOperator.NEVER ->
            stringResource(R.string.smart_rule_summary_no_value, field, operator)
        rule.field == LISTENING_HISTORY_EDITOR_FIELD ->
            if (rule.operator == SmartPlaylistOperator.WITHIN_LAST_DAYS) {
                pluralStringResource(R.plurals.smart_rule_played_within_days,
                    rule.value.toIntOrNull() ?: 0, field, rule.value)
            } else {
                pluralStringResource(R.plurals.smart_rule_not_played_days,
                    rule.value.toIntOrNull() ?: 0, field, rule.value)
            }
        rule.field == SmartPlaylistRuleField.DATE_ADDED ->
            if (rule.operator == SmartPlaylistOperator.WITHIN_LAST_DAYS) {
                pluralStringResource(R.plurals.smart_rule_within_days,
                    rule.value.toIntOrNull() ?: 0, field, rule.value)
            } else {
                pluralStringResource(R.plurals.smart_rule_days_ago,
                    rule.value.toIntOrNull() ?: 0, field, rule.value)
            }
        rule.field == SmartPlaylistRuleField.RECENT_PLAY_COUNT ->
            pluralStringResource(R.plurals.smart_rule_recent_count,
                rule.windowDays.toIntOrNull() ?: 0, field, operator, rule.value, rule.windowDays)
        rule.field == SmartPlaylistRuleField.DURATION ->
            if (rule.operator == SmartPlaylistOperator.BETWEEN) {
                stringResource(R.string.smart_rule_duration_between, field, rule.value, rule.secondValue)
            } else {
                pluralStringResource(R.plurals.smart_rule_duration,
                    rule.value.toDoubleOrNull()?.toInt() ?: 0, field, operator, rule.value)
            }
        rule.operator == SmartPlaylistOperator.BETWEEN ->
            stringResource(R.string.smart_rule_between, field, rule.value, rule.secondValue)
        else -> stringResource(R.string.smart_rule_summary_value, field, operator, rule.value)
    }
}

private fun smartEditorError(error: Throwable): Int = when (error) {
    is IllegalArgumentException -> R.string.smart_error_invalid_definition
    else -> R.string.smart_error_update_failed
}

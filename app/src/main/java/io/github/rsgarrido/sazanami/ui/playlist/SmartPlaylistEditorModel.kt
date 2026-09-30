package io.github.rsgarrido.sazanami.ui.playlist

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.MAX_SMART_PLAYLIST_RESULT_LIMIT
import io.github.rsgarrido.sazanami.data.SmartPlaylistDraft
import io.github.rsgarrido.sazanami.data.SmartPlaylistMatchMode
import io.github.rsgarrido.sazanami.data.SmartPlaylistOperator
import io.github.rsgarrido.sazanami.data.SmartPlaylistRule
import io.github.rsgarrido.sazanami.data.SmartPlaylistRuleField
import io.github.rsgarrido.sazanami.data.SmartPlaylistSortDirection
import io.github.rsgarrido.sazanami.data.SmartPlaylistSortField

data class SmartPlaylistEditorRule(
    val id: Long,
    val field: String = SmartPlaylistRuleField.ARTIST,
    val operator: String = SmartPlaylistOperator.CONTAINS,
    val value: String = "",
    val secondValue: String = "",
    val windowDays: String = "30"
)

data class SmartPlaylistEditorModel(
    val name: String = "",
    val matchMode: String = SmartPlaylistMatchMode.ALL,
    val rules: List<SmartPlaylistEditorRule> = listOf(SmartPlaylistEditorRule(1L)),
    val sortField: String = SmartPlaylistSortField.TITLE,
    val sortDirection: String = SmartPlaylistSortDirection.ASCENDING,
    val resultLimit: String = ""
) {
    val showsMatchModeChoice: Boolean get() = rules.size > 1

    fun validation(existingNames: Collection<String>, originalName: String? = null): SmartEditorValidation {
        val errors = mutableMapOf<Long, Int>()
        rules.forEach { rule -> validateRule(rule)?.let { errors[rule.id] = it } }
        val trimmedName = name.trim()
        val nameError = when {
            trimmedName.isBlank() -> R.string.smart_error_name_required
            existingNames.any {
                !it.equals(originalName, ignoreCase = true) && it.equals(trimmedName, ignoreCase = true)
            } -> R.string.smart_error_name_duplicate
            else -> null
        }
        val parsedLimit = resultLimit.trim().takeIf(String::isNotEmpty)?.toIntOrNull()
        val limitError = when {
            resultLimit.isBlank() -> null
            parsedLimit == null -> R.string.smart_error_limit_whole
            parsedLimit !in 1..MAX_SMART_PLAYLIST_RESULT_LIMIT ->
                R.string.smart_error_limit_range
            else -> null
        }
        return SmartEditorValidation(
            nameError = nameError,
            generalError = when {
                rules.isEmpty() -> R.string.smart_error_rule_required
                sortField == SmartPlaylistSortField.RECENT_PLAY_COUNT &&
                    rules.none { it.field == SmartPlaylistRuleField.RECENT_PLAY_COUNT } ->
                    R.string.smart_error_recent_rule_required
                limitError != null -> limitError
                else -> null
            },
            ruleErrors = errors
        )
    }

    fun toDraft(): SmartPlaylistDraft = SmartPlaylistDraft(
        matchMode = matchMode,
        rules = rules.map(SmartPlaylistEditorRule::toRule),
        sortField = sortField,
        sortDirection = sortDirection,
        resultLimit = resultLimit.trim().takeIf(String::isNotEmpty)?.toInt()
    ).validated()

    companion object {
        fun fromDraft(name: String, draft: SmartPlaylistDraft): SmartPlaylistEditorModel =
            SmartPlaylistEditorModel(
                name = name,
                matchMode = draft.matchMode,
                rules = draft.rules.mapIndexed { index, rule -> rule.toEditorRule(index + 1L) },
                sortField = draft.sortField,
                sortDirection = draft.sortDirection,
                resultLimit = draft.resultLimit?.toString().orEmpty()
            )
    }
}

data class SmartEditorValidation(
    @StringRes val nameError: Int? = null,
    @StringRes val generalError: Int? = null,
    val ruleErrors: Map<Long, Int> = emptyMap()
) {
    val isValid: Boolean get() = nameError == null && generalError == null && ruleErrors.isEmpty()
}

data class SmartRuleFieldOption(
    val storage: String,
    @StringRes val labelRes: Int,
    val operators: List<SmartRuleOperatorOption>,
    val valueKind: SmartRuleValueKind,
    val group: SmartRuleFieldGroup = SmartRuleFieldGroup.METADATA
)

data class SmartRuleOperatorOption(val storage: String, @StringRes val labelRes: Int)

enum class SmartRuleValueKind { TEXT, GENRE, NUMBER, RATING, DURATION_MINUTES, RELATIVE_DAYS, RECENT_COUNT, NONE }

enum class SmartRuleFieldGroup(@StringRes val labelRes: Int) {
    LISTENING(R.string.smart_group_listening),
    METADATA(R.string.smart_group_metadata),
    LIBRARY_FILE(R.string.smart_group_library_file)
}

private val equals = SmartRuleOperatorOption(SmartPlaylistOperator.EQUALS, R.string.smart_op_equals)
private val notEquals = SmartRuleOperatorOption(SmartPlaylistOperator.NOT_EQUALS, R.string.smart_op_is_not)
private val atLeast = SmartRuleOperatorOption(SmartPlaylistOperator.AT_LEAST, R.string.smart_op_at_least)
private val atMost = SmartRuleOperatorOption(SmartPlaylistOperator.AT_MOST, R.string.smart_op_at_most)
private val between = SmartRuleOperatorOption(SmartPlaylistOperator.BETWEEN, R.string.smart_op_between)
internal const val LISTENING_HISTORY_EDITOR_FIELD = "listening_history_editor"
private val relativeOperators = listOf(
    SmartRuleOperatorOption(SmartPlaylistOperator.WITHIN_LAST_DAYS, R.string.smart_op_within_last_days),
    SmartRuleOperatorOption(SmartPlaylistOperator.MORE_THAN_DAYS_AGO, R.string.smart_op_more_than_days_ago)
)

val smartRuleFieldOptions = listOf(
    SmartRuleFieldOption(
        SmartPlaylistRuleField.RATING,
        R.string.smart_field_rating,
        listOf(equals, atLeast, atMost, SmartRuleOperatorOption(SmartPlaylistOperator.UNRATED, R.string.smart_op_unrated)),
        SmartRuleValueKind.RATING,
        SmartRuleFieldGroup.LISTENING
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.TOTAL_PLAY_COUNT,
        R.string.smart_field_total_play_count,
        listOf(equals, atLeast, atMost, between),
        SmartRuleValueKind.NUMBER,
        SmartRuleFieldGroup.LISTENING
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.RECENT_PLAY_COUNT,
        R.string.smart_field_play_count,
        listOf(equals, atLeast, atMost, between),
        SmartRuleValueKind.RECENT_COUNT,
        SmartRuleFieldGroup.LISTENING
    ),
    SmartRuleFieldOption(
        LISTENING_HISTORY_EDITOR_FIELD,
        R.string.smart_field_listening_history,
        listOf(
            SmartRuleOperatorOption(SmartPlaylistOperator.NEVER, R.string.smart_op_never_played),
            SmartRuleOperatorOption(SmartPlaylistOperator.WITHIN_LAST_DAYS, R.string.smart_op_played_within_last),
            SmartRuleOperatorOption(SmartPlaylistOperator.MORE_THAN_DAYS_AGO, R.string.smart_op_not_played_for)
        ),
        SmartRuleValueKind.RELATIVE_DAYS,
        SmartRuleFieldGroup.LISTENING
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.TITLE,
        R.string.smart_field_title,
        textOperators(),
        SmartRuleValueKind.TEXT
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.ARTIST,
        R.string.smart_field_artist,
        textOperators(),
        SmartRuleValueKind.TEXT
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.ALBUM,
        R.string.smart_field_album,
        textOperators(),
        SmartRuleValueKind.TEXT
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.GENRE,
        R.string.smart_field_genre,
        listOf(
            SmartRuleOperatorOption(SmartPlaylistOperator.IS, R.string.smart_op_is),
            SmartRuleOperatorOption(SmartPlaylistOperator.IS_NOT, R.string.smart_op_is_not),
            SmartRuleOperatorOption(SmartPlaylistOperator.CONTAINS, R.string.smart_op_contains)
        ),
        SmartRuleValueKind.GENRE
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.COMPOSER,
        R.string.smart_field_composer,
        textOperators(),
        SmartRuleValueKind.TEXT
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.PUBLISHER,
        R.string.smart_field_publisher,
        textOperators(),
        SmartRuleValueKind.TEXT
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.YEAR,
        R.string.smart_field_year,
        listOf(
            SmartRuleOperatorOption(SmartPlaylistOperator.EQUALS, R.string.smart_op_is),
            notEquals,
            SmartRuleOperatorOption(SmartPlaylistOperator.BEFORE, R.string.smart_op_before),
            SmartRuleOperatorOption(SmartPlaylistOperator.AFTER, R.string.smart_op_after),
            between
        ),
        SmartRuleValueKind.NUMBER
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.BPM,
        R.string.smart_field_bpm,
        listOf(
            SmartRuleOperatorOption(SmartPlaylistOperator.EQUALS, R.string.smart_op_is),
            notEquals,
            SmartRuleOperatorOption(SmartPlaylistOperator.GREATER_THAN, R.string.smart_op_greater_than),
            SmartRuleOperatorOption(SmartPlaylistOperator.LESS_THAN, R.string.smart_op_less_than),
            between
        ),
        SmartRuleValueKind.NUMBER
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.DURATION,
        R.string.smart_field_duration,
        listOf(
            SmartRuleOperatorOption(SmartPlaylistOperator.SHORTER_THAN, R.string.smart_op_shorter_than),
            SmartRuleOperatorOption(SmartPlaylistOperator.LONGER_THAN, R.string.smart_op_longer_than),
            between,
            SmartRuleOperatorOption(SmartPlaylistOperator.ABOUT, R.string.smart_op_about)
        ),
        SmartRuleValueKind.DURATION_MINUTES,
        SmartRuleFieldGroup.LIBRARY_FILE
    ),
    SmartRuleFieldOption(
        SmartPlaylistRuleField.DATE_ADDED,
        R.string.smart_field_date_added,
        relativeOperators,
        SmartRuleValueKind.RELATIVE_DAYS,
        SmartRuleFieldGroup.LIBRARY_FILE
    )
)

val smartSortOptions = listOf(
    SmartPlaylistSortField.TITLE to R.string.smart_field_title,
    SmartPlaylistSortField.ARTIST to R.string.smart_field_artist,
    SmartPlaylistSortField.ALBUM to R.string.smart_field_album,
    SmartPlaylistSortField.YEAR to R.string.smart_field_year,
    SmartPlaylistSortField.RATING to R.string.smart_field_rating,
    SmartPlaylistSortField.PLAY_COUNT to R.string.smart_field_play_count,
    SmartPlaylistSortField.RECENT_PLAY_COUNT to R.string.smart_sort_recent_play_count,
    SmartPlaylistSortField.FORGOTTEN_FAVORITES_RANK to R.string.smart_sort_historical_strength,
    SmartPlaylistSortField.LAST_PLAYED to R.string.smart_sort_last_played
)

private fun textOperators() = listOf(
    SmartRuleOperatorOption(SmartPlaylistOperator.CONTAINS, R.string.smart_op_contains),
    SmartRuleOperatorOption(SmartPlaylistOperator.DOES_NOT_CONTAIN, R.string.smart_op_does_not_contain),
    SmartRuleOperatorOption(SmartPlaylistOperator.IS, R.string.smart_op_is),
    SmartRuleOperatorOption(SmartPlaylistOperator.IS_NOT, R.string.smart_op_is_not)
)

@StringRes
private fun validateRule(rule: SmartPlaylistEditorRule): Int? {
    val field = smartRuleFieldOptions.firstOrNull { it.storage == rule.field }
        ?: return R.string.smart_error_unsupported_field
    if (field.operators.none { it.storage == rule.operator }) {
        return R.string.smart_error_unsupported_operator
    }
    if (rule.operator == SmartPlaylistOperator.UNRATED ||
        rule.operator == SmartPlaylistOperator.NEVER || field.valueKind == SmartRuleValueKind.NONE
    ) return null
    if (rule.value.isBlank()) return R.string.smart_error_value_required
    return when (field.valueKind) {
        SmartRuleValueKind.TEXT,
        SmartRuleValueKind.GENRE -> null
        SmartRuleValueKind.RATING -> if (rule.value.toIntOrNull() in 1..5) null else R.string.smart_error_rating_range
        SmartRuleValueKind.NUMBER -> when (rule.field) {
            SmartPlaylistRuleField.YEAR -> wholeNumberRuleError(rule, minimum = 1000, maximum = 2999)
            SmartPlaylistRuleField.BPM -> wholeNumberRuleError(rule, minimum = 1, maximum = 999)
            SmartPlaylistRuleField.TOTAL_PLAY_COUNT -> wholeNumberRuleError(rule, minimum = 0)
            else -> numericRuleError(rule, positive = false)
        }
        SmartRuleValueKind.DURATION_MINUTES -> numericRuleError(rule, positive = true)
        SmartRuleValueKind.RELATIVE_DAYS -> if (rule.value.toIntOrNull()?.let { it > 0 } == true) null
            else R.string.smart_error_positive_days
        SmartRuleValueKind.RECENT_COUNT -> when {
            rule.windowDays.toIntOrNull()?.let { it > 0 } != true -> R.string.smart_error_positive_window
            else -> wholeNumberRuleError(rule, minimum = 0)
        }
        SmartRuleValueKind.NONE -> null
    }
}

private fun wholeNumberRuleError(
    rule: SmartPlaylistEditorRule,
    minimum: Int,
    maximum: Int = Int.MAX_VALUE
): Int? {
    val first = rule.value.toIntOrNull()
    if (first == null || first !in minimum..maximum) return R.string.smart_error_valid_whole
    if (rule.operator == SmartPlaylistOperator.BETWEEN) {
        val second = rule.secondValue.toIntOrNull()
        if (second == null || second !in first..maximum) {
            return R.string.smart_error_upper_whole
        }
    }
    return null
}

internal fun changeSmartRuleField(
    rule: SmartPlaylistEditorRule,
    selectedFieldStorage: String
): SmartPlaylistEditorRule {
    val selectedField = smartRuleFieldOptions.first { it.storage == selectedFieldStorage }
    return rule.copy(
        field = selectedFieldStorage,
        operator = selectedField.operators.first().storage,
        value = "",
        secondValue = "",
        windowDays = "30"
    )
}

private fun numericRuleError(rule: SmartPlaylistEditorRule, positive: Boolean): Int? {
    val first = rule.value.toDoubleOrNull()
    if (first == null || positive && first <= 0 || !positive && first < 0) return R.string.smart_error_valid_number
    if (rule.operator == SmartPlaylistOperator.BETWEEN) {
        val second = rule.secondValue.toDoubleOrNull()
        if (second == null || second < first) return R.string.smart_error_upper_number
    }
    return null
}

private fun SmartPlaylistEditorRule.toRule(): SmartPlaylistRule {
    val kind = smartRuleFieldOptions.first { it.storage == field }.valueKind
    val noValue = operator == SmartPlaylistOperator.UNRATED || operator == SmartPlaylistOperator.NEVER
    val values = when {
        field == SmartPlaylistRuleField.NEVER_PLAYED -> listOf("true")
        noValue -> emptyList()
        operator == SmartPlaylistOperator.BETWEEN -> listOf(convertValue(value, kind), convertValue(secondValue, kind))
        else -> listOf(convertValue(value, kind))
    }
    return SmartPlaylistRule(
        field = when {
            field == LISTENING_HISTORY_EDITOR_FIELD && operator == SmartPlaylistOperator.NEVER ->
                SmartPlaylistRuleField.NEVER_PLAYED
            field == LISTENING_HISTORY_EDITOR_FIELD -> SmartPlaylistRuleField.LAST_PLAYED
            else -> field
        },
        operator = if (
            field == LISTENING_HISTORY_EDITOR_FIELD && operator == SmartPlaylistOperator.NEVER
        ) SmartPlaylistOperator.IS else operator,
        values = values,
        parameters = if (kind == SmartRuleValueKind.RECENT_COUNT) mapOf("days" to windowDays) else emptyMap()
    )
}

private fun SmartPlaylistRule.toEditorRule(id: Long): SmartPlaylistEditorRule {
    val editorField = when (field) {
        SmartPlaylistRuleField.LAST_PLAYED,
        SmartPlaylistRuleField.NEVER_PLAYED -> LISTENING_HISTORY_EDITOR_FIELD
        else -> field
    }
    val editorOperator = when {
        field == SmartPlaylistRuleField.NEVER_PLAYED -> SmartPlaylistOperator.NEVER
        field == SmartPlaylistRuleField.DURATION && operator == SmartPlaylistOperator.EQUALS ->
            SmartPlaylistOperator.ABOUT
        field == SmartPlaylistRuleField.DURATION && operator == SmartPlaylistOperator.AT_LEAST ->
            SmartPlaylistOperator.LONGER_THAN
        field == SmartPlaylistRuleField.DURATION && operator == SmartPlaylistOperator.AT_MOST ->
            SmartPlaylistOperator.SHORTER_THAN
        field == SmartPlaylistRuleField.YEAR && operator == SmartPlaylistOperator.AT_LEAST ->
            SmartPlaylistOperator.AFTER
        field == SmartPlaylistRuleField.YEAR && operator == SmartPlaylistOperator.AT_MOST ->
            SmartPlaylistOperator.BEFORE
        else -> operator
    }
    val kind = smartRuleFieldOptions.firstOrNull { it.storage == editorField }?.valueKind
    return SmartPlaylistEditorRule(
        id = id,
        field = editorField,
        operator = editorOperator,
        value = displayValue(values.getOrNull(0).orEmpty(), kind),
        secondValue = displayValue(values.getOrNull(1).orEmpty(), kind),
        windowDays = parameters["days"] ?: "30"
    )
}

private fun convertValue(value: String, kind: SmartRuleValueKind): String =
    if (kind == SmartRuleValueKind.DURATION_MINUTES) {
        (value.toDouble() * 60_000.0).toLong().toString()
    } else value.trim()

private fun displayValue(value: String, kind: SmartRuleValueKind?): String =
    if (kind == SmartRuleValueKind.DURATION_MINUTES && value.isNotBlank()) {
        (value.toDoubleOrNull()?.div(60_000.0))?.let { minutes ->
            if (minutes % 1.0 == 0.0) minutes.toLong().toString() else minutes.toString()
        } ?: value
    } else value

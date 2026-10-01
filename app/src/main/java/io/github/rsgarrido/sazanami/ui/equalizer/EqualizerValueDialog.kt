package io.github.rsgarrido.sazanami.ui.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.player.equalizer.normalizeEqualizerDb

@Composable
internal fun EqualizerValueDialog(
    title: String,
    initialValueDb: Double,
    minimumDb: Double,
    maximumDb: Double,
    onPreview: (Double) -> Unit,
    onCancel: () -> Unit,
    onApply: (Double) -> Unit
) {
    var valueText by remember(initialValueDb) {
        mutableStateOf("%.1f".format(java.util.Locale.ROOT, initialValueDb))
    }
    val parsed = parseEqualizerValue(
        valueText,
        minimumDb,
        maximumDb
    )
    val error = if (valueText.isBlank() || parsed != null) {
        null
    } else {
        stringResource(R.string.eq_value_range_error, minimumDb, maximumDb)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { updated ->
                        valueText = updated
                        parseEqualizerValue(
                            updated,
                            minimumDb,
                            maximumDb
                        )?.let(onPreview)
                    },
                    label = { Text(stringResource(R.string.eq_decibels)) },
                    supportingText = error?.let { message ->
                        { Text(message) }
                    },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            val next = (
                                (parsed ?: initialValueDb) - 0.1
                                ).coerceAtLeast(minimumDb)
                            valueText = "%.1f".format(
                                java.util.Locale.ROOT,
                                next
                            )
                            onPreview(next)
                        }
                    ) {
                        Text(stringResource(R.string.eq_decrease_tenth))
                    }
                    TextButton(
                        onClick = {
                            valueText = "0.0"
                            onPreview(0.0)
                        }
                    ) {
                        Text(stringResource(R.string.eq_reset_zero))
                    }
                    TextButton(
                        onClick = {
                            val next = (
                                (parsed ?: initialValueDb) + 0.1
                                ).coerceAtMost(maximumDb)
                            valueText = "%.1f".format(
                                java.util.Locale.ROOT,
                                next
                            )
                            onPreview(next)
                        }
                    ) {
                        Text(stringResource(R.string.eq_increase_tenth))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.eq_cancel))
            }
        },
        confirmButton = {
            Button(
                enabled = parsed != null,
                onClick = {
                    parsed?.let(onApply)
                }
            ) {
                Text(stringResource(R.string.eq_apply))
            }
        }
    )
}

internal fun parseEqualizerValue(
    text: String,
    minimumDb: Double,
    maximumDb: Double
): Double? {
    val parsed = text.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?: return null
    if (!parsed.isFinite() || parsed !in minimumDb..maximumDb) {
        return null
    }
    return normalizeEqualizerDb(parsed)
}

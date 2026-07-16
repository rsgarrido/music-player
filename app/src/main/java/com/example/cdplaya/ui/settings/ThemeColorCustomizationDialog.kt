package com.example.cdplaya.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.ui.player.theme.PlayerThemeColorPreset
import com.example.cdplaya.ui.player.theme.PlayerThemeCustomizationOption
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenField
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.colorPresetsFor
import com.example.cdplaya.ui.player.theme.customizationOptions

@Composable
fun ThemeColorCustomizationDialog(
    playerTheme: PlayerTheme,
    tokens: PlayerThemeTokens,
    onColorSelected: (PlayerThemeTokenField, Color) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = remember(playerTheme) { playerTheme.customizationOptions() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Customize ${playerTheme.displayName}")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                options.forEachIndexed { index, option ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }

                    ThemeColorOptionRow(
                        playerTheme = playerTheme,
                        option = option,
                        currentColor = tokens.colorFor(option.field),
                        onColorSelected = { color ->
                            onColorSelected(option.field, color)
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onReset) {
                Text(text = "Reset theme colors")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done")
            }
        }
    )
}

@Composable
private fun ThemeColorOptionRow(
    playerTheme: PlayerTheme,
    option: PlayerThemeCustomizationOption,
    currentColor: Color?,
    onColorSelected: (Color) -> Unit
) {
    val presets = remember(playerTheme, option.field) {
        playerTheme.colorPresetsFor(option.field)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (currentColor != null) {
                Surface(
                    modifier = Modifier
                        .size(28.dp)
                        .semantics {
                            contentDescription = "Current ${option.displayName} color"
                        },
                    shape = CircleShape,
                    color = currentColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {}
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                ColorSwatchButton(
                    preset = preset,
                    optionName = option.displayName,
                    isSelected = preset.color == currentColor,
                    onClick = {
                        onColorSelected(preset.color)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchButton(
    preset: PlayerThemeColorPreset,
    optionName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(58.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(42.dp)
                .semantics {
                    contentDescription = "$optionName: ${preset.name}"
                    selected = isSelected
                },
            shape = CircleShape,
            color = preset.color,
            border = BorderStroke(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        ) {}

        Spacer(modifier = Modifier.size(4.dp))

        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private fun PlayerThemeTokens.colorFor(field: PlayerThemeTokenField): Color? = when (field) {
    PlayerThemeTokenField.SHELL -> shellColor
    PlayerThemeTokenField.ACCENT -> accentColor
    PlayerThemeTokenField.DISPLAY_BACKGROUND -> displayBackgroundColor
    PlayerThemeTokenField.DISPLAY_TEXT -> displayTextColor
    PlayerThemeTokenField.SECONDARY_ACCENT -> secondaryAccentColor
}

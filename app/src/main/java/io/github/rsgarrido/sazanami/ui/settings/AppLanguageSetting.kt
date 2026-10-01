package io.github.rsgarrido.sazanami.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.localization.AppLanguage
import io.github.rsgarrido.sazanami.localization.AppLanguages

@Composable
internal fun SettingsLanguageRow() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    var showChooser by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) revision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Read the authoritative override again on resume/configuration changes, not a stored preference.
    val selected = remember(context, configuration, revision) { AppLanguages.current(context) }
    SettingsRow(
        title = stringResource(R.string.settings_language),
        summary = stringResource(selected.labelRes),
        icon = Icons.Filled.Language,
        emphasizeSummary = true,
        onClick = {
            revision++
            showChooser = true
        },
        navigationContentDescription = stringResource(R.string.settings_choose_language)
    )
    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text(stringResource(R.string.settings_choose_language)) },
            text = {
                Column(Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .selectable(
                                    selected = language == selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        showChooser = false
                                        AppLanguages.select(context, language)
                                        revision++
                                    }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = language == selected, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(language.labelRes), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChooser = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }
}

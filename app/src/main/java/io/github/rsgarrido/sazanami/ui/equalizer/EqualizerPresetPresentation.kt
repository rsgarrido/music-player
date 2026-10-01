package io.github.rsgarrido.sazanami.ui.equalizer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.player.equalizer.EqualizerPresetMatch

@Composable
internal fun EqualizerPresetMatch.localizedLabel(): String =
    builtInId?.let { stringResource(it.labelRes) } ?: name

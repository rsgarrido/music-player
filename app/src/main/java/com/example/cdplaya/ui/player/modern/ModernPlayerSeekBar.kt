package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cdplaya.ui.formatDuration

@Composable
internal fun ModernPlayerSeekBar(
    currentPosition: Int,
    duration: Int,
    onSeekChange: (Int) -> Unit,
    style: ModernPlayerStyle
) {
    val safeDuration = duration.coerceAtLeast(1)
    val safePosition = currentPosition.coerceIn(0, safeDuration)

    Slider(
        value = safePosition.toFloat(),
        onValueChange = { newPosition ->
            onSeekChange(newPosition.toInt())
        },
        valueRange = 0f..safeDuration.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = style.contentColor,
            activeTrackColor = style.contentColor,
            inactiveTrackColor = style.inactiveTrackColor
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDuration(safePosition),
            style = MaterialTheme.typography.bodySmall,
            color = style.timeColor
        )

        Text(
            text = formatDuration(duration),
            style = MaterialTheme.typography.bodySmall,
            color = style.timeColor
        )
    }
}

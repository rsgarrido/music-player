package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cdplaya.player.audioquality.AudioQualityInfo
import com.example.cdplaya.player.audioquality.toDisplayText

@Composable
internal fun ModernPlayerAudioQualityBadge(
    audioQualityInfo: AudioQualityInfo?,
    style: ModernPlayerStyle,
    modifier: Modifier = Modifier
) {
    val displayText = audioQualityInfo?.toDisplayText() ?: return
    val badgeShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .background(
                color = style.contentColor.copy(alpha = 0.10f),
                shape = badgeShape
            )
            .border(
                width = 1.dp,
                color = style.contentColor.copy(alpha = 0.14f),
                shape = badgeShape
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = style.contentColor.copy(alpha = 0.82f),
            maxLines = 1
        )
    }
}

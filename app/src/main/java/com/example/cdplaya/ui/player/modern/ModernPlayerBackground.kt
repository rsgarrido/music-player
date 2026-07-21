package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song

@Composable
internal fun BoxScope.ModernPlayerBackground(
    currentSong: Song,
    style: ModernPlayerStyle
) {
    ModernPlayerAlbumImage(
        currentSong = currentSong,
        contentDescription = null,
        modifier = Modifier
            .matchParentSize()
            .blur(42.dp),
        contentScale = ContentScale.Crop,
        transitionDurationMillis = ModernPlayerDefaults.BackgroundTransitionDurationMillis
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(style.backgroundOverlayColor)
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        style.gradientTopColor,
                        style.gradientCenterColor,
                        style.gradientBottomColor
                    )
                )
            )
    )
}

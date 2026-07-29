package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import android.os.Build
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
    val backgroundPolicy = modernBackgroundPolicy(Build.VERSION.SDK_INT)
    ModernPlayerAlbumImage(
        currentSong = currentSong,
        contentDescription = null,
        modifier = Modifier
            .matchParentSize()
            .then(
                if (backgroundPolicy.usePlatformBlur) {
                    Modifier.blur(42.dp)
                } else {
                    Modifier
                }
            ),
        contentScale = ContentScale.Crop,
        transitionDurationMillis = ModernPlayerDefaults.BackgroundTransitionDurationMillis
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(style.backgroundOverlayColor)
    )

    if (backgroundPolicy.legacyScrimAlpha > 0f) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    style.backgroundOverlayColor.copy(
                        alpha = backgroundPolicy.legacyScrimAlpha
                    )
                )
        )
    }

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

internal data class ModernBackgroundPolicy(
    val usePlatformBlur: Boolean,
    val legacyScrimAlpha: Float
)

internal fun modernBackgroundPolicy(sdkInt: Int): ModernBackgroundPolicy =
    if (sdkInt >= Build.VERSION_CODES.S) {
        ModernBackgroundPolicy(
            usePlatformBlur = true,
            legacyScrimAlpha = 0f
        )
    } else {
        ModernBackgroundPolicy(
            usePlatformBlur = false,
            legacyScrimAlpha = 0.38f
        )
    }

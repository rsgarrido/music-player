package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class ModernPlayerStyle(
    val backgroundColor: Color,
    val backgroundOverlayColor: Color,
    val gradientTopColor: Color,
    val gradientCenterColor: Color,
    val gradientBottomColor: Color,
    val contentColor: Color,
    val secondaryContentColor: Color,
    val tertiaryContentColor: Color,
    val timeColor: Color,
    val inactiveTrackColor: Color,
    val inactiveControlColor: Color,
    val inactiveControlBackgroundColor: Color,
    val secondaryActionBackgroundColor: Color,
    val artworkContainerColor: Color,
    val accentColor: Color,
    val onAccentColor: Color,
    val artworkShape: Shape,
    val primaryControlShape: Shape,
    val modeControlShape: Shape
)

object ModernPlayerDefaults {
    val MaximumArtworkSize = 400.dp
    val ArtworkShape = RoundedCornerShape(30.dp)
    val ContentHorizontalPadding = 16.dp
    val ContentVerticalPadding = 12.dp

    @Composable
    fun style(): ModernPlayerStyle {
        return ModernPlayerStyle(
            backgroundColor = Color.Black,
            backgroundOverlayColor = Color.Black.copy(alpha = 0.42f),
            gradientTopColor = Color.Black.copy(alpha = 0.70f),
            gradientCenterColor = Color.Black.copy(alpha = 0.16f),
            gradientBottomColor = Color.Black.copy(alpha = 0.78f),
            contentColor = Color.White,
            secondaryContentColor = Color.White.copy(alpha = 0.88f),
            tertiaryContentColor = Color.White.copy(alpha = 0.68f),
            timeColor = Color.White.copy(alpha = 0.72f),
            inactiveTrackColor = Color.White.copy(alpha = 0.22f),
            inactiveControlColor = Color.White.copy(alpha = 0.74f),
            inactiveControlBackgroundColor = Color.White.copy(alpha = 0.10f),
            secondaryActionBackgroundColor = Color.White.copy(alpha = 0.18f),
            artworkContainerColor = Color.Black.copy(alpha = 0.20f),
            accentColor = MaterialTheme.colorScheme.primary,
            onAccentColor = MaterialTheme.colorScheme.onPrimary,
            artworkShape = ArtworkShape,
            primaryControlShape = CircleShape,
            modeControlShape = CircleShape
        )
    }
}

package com.example.cdplaya.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

@Composable
fun appShellBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerLowest,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}

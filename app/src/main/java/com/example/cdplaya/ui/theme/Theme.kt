package com.example.cdplaya.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CdPlayaAccent,
    onPrimary = CdPlayaOnAccent,
    primaryContainer = CdPlayaAccentContainer,
    onPrimaryContainer = CdPlayaOnAccentContainer,
    secondary = CdPlayaOnSurfaceVariant,
    onSecondary = CdPlayaBackground,
    secondaryContainer = CdPlayaSurfaceHighest,
    onSecondaryContainer = CdPlayaOnSurface,
    tertiary = CdPlayaOnAccentContainer,
    background = CdPlayaBackground,
    onBackground = CdPlayaOnSurface,
    surface = CdPlayaSurface,
    onSurface = CdPlayaOnSurface,
    surfaceVariant = CdPlayaSurfaceHigh,
    onSurfaceVariant = CdPlayaOnSurfaceVariant,
    outline = CdPlayaOutline,
    outlineVariant = CdPlayaOutlineVariant,
    surfaceTint = CdPlayaAccent,
    surfaceContainerLowest = CdPlayaBackground,
    surfaceContainerLow = CdPlayaSurfaceLow,
    surfaceContainer = CdPlayaSurfaceContainer,
    surfaceContainerHigh = CdPlayaSurfaceHigh,
    surfaceContainerHighest = CdPlayaSurfaceHighest
)

private val LightColorScheme = lightColorScheme(
    primary = CdPlayaAccentContainer,
    onPrimary = CdPlayaOnAccentContainer,
    primaryContainer = CdPlayaOnAccentContainer,
    onPrimaryContainer = CdPlayaOnAccent,
    background = CdPlayaLightBackground,
    onBackground = CdPlayaLightOnSurface,
    surface = CdPlayaLightSurface,
    onSurface = CdPlayaLightOnSurface,
    surfaceTint = CdPlayaAccentContainer
)

@Composable
fun CdplayaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

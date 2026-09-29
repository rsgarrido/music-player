package io.github.rsgarrido.sazanami.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.rsgarrido.sazanami.data.preferences.AppFont

private val DarkColorScheme = darkColorScheme(
    primary = SazanamiAccent,
    onPrimary = SazanamiOnAccent,
    primaryContainer = SazanamiAccentContainer,
    onPrimaryContainer = SazanamiOnAccentContainer,
    secondary = SazanamiOnSurfaceVariant,
    onSecondary = SazanamiBackground,
    secondaryContainer = SazanamiSurfaceHighest,
    onSecondaryContainer = SazanamiOnSurface,
    tertiary = SazanamiOnAccentContainer,
    background = SazanamiBackground,
    onBackground = SazanamiOnSurface,
    surface = SazanamiSurface,
    onSurface = SazanamiOnSurface,
    surfaceVariant = SazanamiSurfaceHigh,
    onSurfaceVariant = SazanamiOnSurfaceVariant,
    outline = SazanamiOutline,
    outlineVariant = SazanamiOutlineVariant,
    surfaceTint = SazanamiAccent,
    surfaceContainerLowest = SazanamiBackground,
    surfaceContainerLow = SazanamiSurfaceLow,
    surfaceContainer = SazanamiSurfaceContainer,
    surfaceContainerHigh = SazanamiSurfaceHigh,
    surfaceContainerHighest = SazanamiSurfaceHighest
)

private val LightColorScheme = lightColorScheme(
    primary = SazanamiLightPrimary,
    onPrimary = Color.White,
    primaryContainer = SazanamiLightPrimaryContainer,
    onPrimaryContainer = SazanamiAccentContainer,
    inversePrimary = SazanamiAccent,
    secondary = SazanamiLightSecondary,
    onSecondary = SazanamiLightOnSecondary,
    secondaryContainer = SazanamiLightSecondaryContainer,
    onSecondaryContainer = SazanamiLightOnSecondaryContainer,
    tertiary = SazanamiLightTertiary,
    onTertiary = SazanamiLightOnTertiary,
    tertiaryContainer = SazanamiLightTertiaryContainer,
    onTertiaryContainer = SazanamiLightOnTertiaryContainer,
    background = SazanamiLightBackground,
    onBackground = SazanamiLightOnSurface,
    surface = SazanamiLightSurface,
    onSurface = SazanamiLightOnSurface,
    surfaceVariant = SazanamiLightSurfaceVariant,
    onSurfaceVariant = SazanamiLightOnSurfaceVariant,
    inverseSurface = SazanamiLightInverseSurface,
    inverseOnSurface = SazanamiLightInverseOnSurface,
    outline = SazanamiLightOutline,
    outlineVariant = SazanamiLightOutlineVariant,
    error = SazanamiLightError,
    onError = SazanamiLightOnError,
    errorContainer = SazanamiLightErrorContainer,
    onErrorContainer = SazanamiLightOnErrorContainer,
    scrim = Color.Black,
    surfaceTint = SazanamiLightPrimary,
    surfaceBright = SazanamiLightSurface,
    surfaceDim = SazanamiLightSurfaceHighest,
    surfaceContainerLowest = SazanamiLightSurface,
    surfaceContainerLow = SazanamiLightSurfaceLow,
    surfaceContainer = SazanamiLightSurfaceContainer,
    surfaceContainerHigh = SazanamiLightSurfaceHigh,
    surfaceContainerHighest = SazanamiLightSurfaceHighest
)

@Composable
fun SazanamiTheme(
    darkTheme: Boolean = true,
    appFont: AppFont = AppFont.SAZANAMI,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyFor(appFont),
        content = content
    )
}

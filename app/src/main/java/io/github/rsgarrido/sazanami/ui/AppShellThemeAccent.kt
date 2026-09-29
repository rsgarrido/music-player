package io.github.rsgarrido.sazanami.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import io.github.rsgarrido.sazanami.data.PlayerTheme
import io.github.rsgarrido.sazanami.ui.player.theme.PlayerThemeTokens
import io.github.rsgarrido.sazanami.ui.theme.SazanamiAccent
import io.github.rsgarrido.sazanami.ui.theme.SazanamiSurfaceHigh

private const val MinimumShellAccentContrast = 4.5f
internal const val ChartAccentMinimumContrast = 3f

internal val LocalAppShellAccent = staticCompositionLocalOf { SazanamiAccent }
internal val LocalAppShellChartAccent = staticCompositionLocalOf { SazanamiAccent }

val AppShellAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppShellAccent.current

val AppShellChartAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppShellChartAccent.current

@Composable
fun rememberAppShellAccent(
    playerTheme: PlayerTheme,
    tokens: PlayerThemeTokens?,
    fallbackAccent: Color = MaterialTheme.colorScheme.primary,
    minimumContrast: Float = MinimumShellAccentContrast,
    contrastSurface: Color? = null
): Color {
    val scheme = MaterialTheme.colorScheme
    val surfaces = if (contrastSurface == null) {
        listOf(
            scheme.background,
            scheme.surfaceContainerLow,
            scheme.surfaceContainerHigh,
            scheme.surfaceContainerHighest
        )
    } else {
        listOf(contrastSurface)
    }
    return remember(playerTheme, tokens, fallbackAccent, surfaces, scheme.onSurface, minimumContrast) {
        resolveAppShellAccent(
            playerTheme = playerTheme,
            tokens = tokens,
            fallbackAccent = fallbackAccent,
            surfaces = surfaces,
            foreground = scheme.onSurface,
            minimumContrast = minimumContrast
        )
    }
}

internal fun resolveAppShellAccent(
    playerTheme: PlayerTheme,
    tokens: PlayerThemeTokens?,
    fallbackAccent: Color = SazanamiAccent,
    surfaces: List<Color> = listOf(SazanamiSurfaceHigh),
    foreground: Color = Color.White,
    minimumContrast: Float = MinimumShellAccentContrast
): Color {
    val tokenAccent = when (playerTheme) {
        PlayerTheme.DEFAULT -> null
        PlayerTheme.CLASSIC_WHEEL -> tokens?.accentColor
        PlayerTheme.POCKET_CASSETTE -> tokens?.secondaryAccentColor ?: tokens?.accentColor
        PlayerTheme.POCKET_FLIP -> tokens?.accentColor
        PlayerTheme.RETRO_RACK -> tokens?.accentColor
        PlayerTheme.POCKET_DISC -> tokens?.secondaryAccentColor ?: tokens?.accentColor
    }
    return ensureReadableShellAccent(
        tokenAccent ?: fallbackAccent,
        surfaces,
        foreground,
        minimumContrast
    )
}

private fun ensureReadableShellAccent(
    accent: Color,
    surfaces: List<Color>,
    foreground: Color,
    minimumContrast: Float
): Color {
    var readableAccent = accent.copy(alpha = 1f)
    val adjustment = if (foreground.luminance() < 0.5f) Color.Black else foreground
    repeat(20) {
        if (surfaces.all { surface ->
                contrastRatio(readableAccent, surface) >= minimumContrast
            }) {
            return readableAccent
        }
        readableAccent = lerp(
            readableAccent,
            adjustment,
            if (adjustment == Color.Black) 0.08f else 0.12f
        )
    }
    return adjustment
}

private fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

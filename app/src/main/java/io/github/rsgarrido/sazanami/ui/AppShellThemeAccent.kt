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

internal val LocalAppShellAccent = staticCompositionLocalOf { SazanamiAccent }

val AppShellAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppShellAccent.current

@Composable
fun rememberAppShellAccent(
    playerTheme: PlayerTheme,
    tokens: PlayerThemeTokens?,
    fallbackAccent: Color = MaterialTheme.colorScheme.primary
): Color {
    val scheme = MaterialTheme.colorScheme
    val surfaces = listOf(
        scheme.background,
        scheme.surfaceContainerLow,
        scheme.surfaceContainerHigh,
        scheme.surfaceContainerHighest
    )
    return remember(playerTheme, tokens, fallbackAccent, surfaces, scheme.onSurface) {
        resolveAppShellAccent(
            playerTheme = playerTheme,
            tokens = tokens,
            fallbackAccent = fallbackAccent,
            surfaces = surfaces,
            foreground = scheme.onSurface
        )
    }
}

internal fun resolveAppShellAccent(
    playerTheme: PlayerTheme,
    tokens: PlayerThemeTokens?,
    fallbackAccent: Color = SazanamiAccent,
    surfaces: List<Color> = listOf(SazanamiSurfaceHigh),
    foreground: Color = Color.White
): Color {
    val tokenAccent = when (playerTheme) {
        PlayerTheme.DEFAULT -> null
        PlayerTheme.CLASSIC_WHEEL -> tokens?.accentColor
        PlayerTheme.POCKET_CASSETTE -> tokens?.secondaryAccentColor ?: tokens?.accentColor
        PlayerTheme.POCKET_FLIP -> tokens?.accentColor
        PlayerTheme.RETRO_RACK -> tokens?.accentColor
        PlayerTheme.POCKET_DISC -> tokens?.secondaryAccentColor ?: tokens?.accentColor
    }
    return ensureReadableShellAccent(tokenAccent ?: fallbackAccent, surfaces, foreground)
}

private fun ensureReadableShellAccent(
    accent: Color,
    surfaces: List<Color>,
    foreground: Color
): Color {
    var readableAccent = accent.copy(alpha = 1f)
    repeat(20) {
        if (surfaces.all { surface ->
                contrastRatio(readableAccent, surface) >= MinimumShellAccentContrast
            }) {
            return readableAccent
        }
        readableAccent = lerp(readableAccent, foreground, 0.12f)
    }
    return foreground
}

private fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

package io.github.rsgarrido.sazanami.data

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R

enum class PlayerTheme(
    val id: String,
    val displayName: String,
    @StringRes val labelRes: Int
) {
    DEFAULT(
        id = "default",
        displayName = "Sazanami Default",
        labelRes = R.string.player_theme_default
    ),

    CLASSIC_WHEEL(
        id = "classic_wheel",
        displayName = "Classic Wheel",
        labelRes = R.string.player_theme_classic_wheel
    ),

    RETRO_RACK(
        id = "retro_rack",
        displayName = "Retro Rack",
        labelRes = R.string.player_theme_retro_rack
    ),

    POCKET_FLIP(
        id = "pocket_flip",
        displayName = "Pocket Flip",
        labelRes = R.string.player_theme_pocket_flip
    ),

    POCKET_CASSETTE(
        id = "pocket_cassette",
        displayName = "Pocket Cassette",
        labelRes = R.string.player_theme_pocket_cassette
    ),

    POCKET_DISC(
        id = "pocket_disc",
        displayName = "Pocket Disc",
        labelRes = R.string.player_theme_pocket_disc
    );

    companion object {
        fun fromId(id: String?): PlayerTheme {
            return values().firstOrNull { playerTheme ->
                playerTheme.id == id
            } ?: DEFAULT
        }
    }
}

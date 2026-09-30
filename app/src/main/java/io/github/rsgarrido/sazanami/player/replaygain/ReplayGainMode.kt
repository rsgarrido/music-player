package io.github.rsgarrido.sazanami.player.replaygain

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R

enum class ReplayGainMode(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val diagnosticDisplayName: String
) {
    OFF(
        R.string.replay_gain_off,
        R.string.replay_gain_off_description,
        "Off"
    ),
    TRACK(
        R.string.replay_gain_track,
        R.string.replay_gain_track_description,
        "Track gain"
    ),
    ALBUM(
        R.string.replay_gain_album,
        R.string.replay_gain_album_description,
        "Album gain"
    ),
    SMART(
        R.string.replay_gain_smart,
        R.string.replay_gain_smart_description,
        "Smart"
    )
}

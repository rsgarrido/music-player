package io.github.rsgarrido.sazanami.ui.player

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.knownDiscNumber
import io.github.rsgarrido.sazanami.data.trackNumberWithinDisc
import io.github.rsgarrido.sazanami.player.audioquality.AudioQualityInfo
import io.github.rsgarrido.sazanami.player.audioquality.formatSampleRate
import io.github.rsgarrido.sazanami.player.audioquality.normalizeAudioFormat
import java.text.NumberFormat
import java.util.Locale

internal sealed interface TrackInfoValue {
    data class Text(val text: String) : TrackInfoValue
    data class Localized(@StringRes val resource: Int, val arguments: List<Any> = emptyList()) : TrackInfoValue
}

internal data class TrackInfoRow(@StringRes val label: Int, val value: TrackInfoValue)
internal data class TrackInfoSection(@StringRes val title: Int, val rows: List<TrackInfoRow>)

internal fun formatTrackInfoFileSize(bytes: Long, locale: Locale = Locale.getDefault()): String? {
    if (bytes <= 0L) return null
    val units = listOf("B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB")
    var amount = bytes.toDouble()
    var unit = 0
    while (amount >= 1024 && unit < units.lastIndex) { amount /= 1024; unit++ }
    val number = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = if (unit == 0) 0 else 1
    }.format(amount)
    return "$number ${units[unit]}"
}

internal fun formatTrackInfoDuration(milliseconds: Long): String? {
    if (milliseconds <= 0L) return null
    val seconds = milliseconds / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

internal fun formatTrackInfoCodec(mime: String?): String? = when (val known = mime?.trim()) {
    "audio/mpeg" -> "MP3"
    "audio/mp4a-latm" -> "AAC"
    "audio/vorbis" -> "Vorbis"
    "audio/opus" -> "Opus"
    "audio/flac" -> "FLAC"
    "audio/alac" -> "ALAC"
    "audio/raw" -> "PCM"
    else -> known?.takeIf { it.startsWith("audio/") && it.length > 6 }
}

internal fun knownTrackInfoText(value: String): String? = value.trim().takeUnless {
    it.isEmpty() || it.lowercase(Locale.ROOT) in setOf("<unknown>", "unknown", "unknown artist",
        "unknown album", "unknown title", "artista desconocido", "álbum desconocido", "título desconocido")
}

internal fun trackInfoSections(
    song: Song,
    audio: AudioQualityInfo?,
    locale: Locale = Locale.getDefault()
): List<TrackInfoSection> {
    fun MutableList<TrackInfoRow>.text(label: Int, value: String?) {
        if (!value.isNullOrBlank()) add(TrackInfoRow(label, TrackInfoValue.Text(value)))
    }
    fun MutableList<TrackInfoRow>.number(label: Int, value: Int?, unit: Int) {
        if (value != null && value > 0) add(TrackInfoRow(label, TrackInfoValue.Localized(unit, listOf(value))))
    }
    val track = buildList<TrackInfoRow> {
        text(R.string.metadata_title, knownTrackInfoText(song.title))
        text(R.string.metadata_artist, knownTrackInfoText(song.artist))
        text(R.string.metadata_album, knownTrackInfoText(song.album))
        text(R.string.metadata_album_artist, knownTrackInfoText(song.albumArtist))
        text(R.string.metadata_track_number, song.trackNumberWithinDisc()?.toString())
        text(R.string.metadata_disc_number, song.knownDiscNumber()?.toString())
        text(R.string.metadata_date_year, song.year?.takeIf { it > 0 }?.toString())
        text(R.string.metadata_genre, song.genres.mapNotNull(::knownTrackInfoText).distinct()
            .joinToString(" · ").takeIf { it.isNotEmpty() })
        text(R.string.smart_field_duration, formatTrackInfoDuration(song.duration))
    }
    val file = buildList<TrackInfoRow> {
        val path = song.filePath.trim().takeIf { it.startsWith("/") && !it.startsWith("/proc/") }
        text(R.string.player_track_info_filename, song.displayName.trim().ifBlank {
            path?.substringAfterLast('/').orEmpty()
        })
        text(R.string.player_track_info_file_size, formatTrackInfoFileSize(song.fileSizeBytes, locale))
        text(R.string.player_track_info_location, path ?: song.relativePath.trim().takeIf { it.isNotEmpty() })
    }
    val technical = buildList<TrackInfoRow> {
        val extension = song.displayName.substringAfterLast('.', "").ifBlank {
            song.filePath.substringAfterLast('.', "")
        }
        text(R.string.player_track_info_format, normalizeAudioFormat(audio?.format) ?: normalizeAudioFormat(extension))
        text(R.string.player_track_info_codec, formatTrackInfoCodec(audio?.codecMimeType))
        audio?.sampleRateHz?.takeIf { it > 0 }?.let {
            text(R.string.player_track_info_sample_rate, "${formatSampleRate(it)} kHz")
        }
        number(R.string.player_track_info_bit_depth, audio?.bitDepth, R.string.player_track_info_bits)
        number(R.string.player_track_info_bitrate, audio?.bitrateKbps, R.string.player_track_info_kbps)
        audio?.channelCount?.takeIf { it > 0 }?.let {
            add(TrackInfoRow(R.string.player_track_info_channels, TrackInfoValue.Localized(
                when (it) {
                    1 -> R.string.player_track_info_mono
                    2 -> R.string.player_track_info_stereo
                    else -> R.string.player_track_info_channel_count
                }, listOf(it)
            )))
        }
    }
    return listOf(TrackInfoSection(R.string.player_track_info_track, track),
        TrackInfoSection(R.string.player_track_info_file, file),
        TrackInfoSection(R.string.player_track_info_audio, technical)).filter { it.rows.isNotEmpty() }
}

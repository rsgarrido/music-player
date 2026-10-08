package io.github.rsgarrido.sazanami.ui.player

import android.net.Uri
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.player.audioquality.AudioQualityInfo
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class TrackInfoFormatterTest {
    private val song = Song(1, "Title", "Artist", "Album", 1001, 185_000L,
        mock(Uri::class.java), "/music/track.flac", "/music", null, volumeName = "external",
        fileSizeBytes = 1536, year = 2024, genres = listOf("Rock", "Metal"))

    @Test fun trackAndDiscUseExistingEncodedNumberInterpretation() {
        val rows = trackInfoSections(song, null).flatMap { it.rows }
        assertEquals(TrackInfoValue.Text("1"), rows.first { it.label == R.string.metadata_track_number }.value)
        assertEquals(TrackInfoValue.Text("1"), rows.first { it.label == R.string.metadata_disc_number }.value)
        assertEquals(TrackInfoValue.Text("3:05"), rows.first { it.label == R.string.smart_field_duration }.value)
        assertEquals(TrackInfoValue.Text("Rock · Metal"), rows.first { it.label == R.string.metadata_genre }.value)
    }

    @Test fun fileSizesUseHumanUnitsAndRejectUnknownNumbers() {
        assertNull(formatTrackInfoFileSize(0))
        assertNull(formatTrackInfoFileSize(-1))
        assertEquals("512 B", formatTrackInfoFileSize(512, Locale.US))
        assertEquals("1.5 KiB", formatTrackInfoFileSize(1536, Locale.US))
        assertEquals("1,5 KiB", formatTrackInfoFileSize(1536, Locale("es", "ES")))
        assertEquals("1 MiB", formatTrackInfoFileSize(1024L * 1024, Locale.US))
        assertNull(formatTrackInfoDuration(0))
        assertNull(formatTrackInfoDuration(-1))
    }

    @Test fun audioSourceValuesKeepContainerCodecAndUnitsSeparate() {
        val rows = trackInfoSections(song, AudioQualityInfo("OGG", 16, 44_100, 830, "audio/vorbis", 2))
            .last().rows.associate { it.label to it.value }
        assertEquals(TrackInfoValue.Text("OGG"), rows[R.string.player_track_info_format])
        assertEquals(TrackInfoValue.Text("Vorbis"), rows[R.string.player_track_info_codec])
        assertEquals(TrackInfoValue.Text("44.1 kHz"), rows[R.string.player_track_info_sample_rate])
        assertEquals(TrackInfoValue.Localized(R.string.player_track_info_bits, listOf(16)),
            rows[R.string.player_track_info_bit_depth])
        assertEquals(TrackInfoValue.Localized(R.string.player_track_info_kbps, listOf(830)),
            rows[R.string.player_track_info_bitrate])
        assertEquals(TrackInfoValue.Localized(R.string.player_track_info_stereo, listOf(2)),
            rows[R.string.player_track_info_channels])
    }

    @Test fun unknownSourceValuesDoNotBecomeZeroRowsOrGuessedBitDepth() {
        val target = song.copy(title = "", artist = "<unknown>", album = "", filePath = "content://only/1",
            displayName = "", fileSizeBytes = 0, trackNumber = 0, duration = 0, year = 0, genres = emptyList())
        assertTrue(trackInfoSections(target, AudioQualityInfo(null, 0, -1, 0, "", 0)).isEmpty())
        assertEquals("FLAC", (trackInfoSections(song, null).last().rows.single().value as TrackInfoValue.Text).text)
        assertNull(formatTrackInfoCodec(null))
    }

    @Test fun monoAndMultichannelUseTheirOwnLocalizedValues() {
        listOf(1 to R.string.player_track_info_mono, 6 to R.string.player_track_info_channel_count).forEach { (n, res) ->
            val rows = trackInfoSections(song, AudioQualityInfo(null, null, null, null, channelCount = n)).last().rows
            assertEquals(TrackInfoValue.Localized(res, listOf(n)), rows.first {
                it.label == R.string.player_track_info_channels
            }.value)
        }
    }
}

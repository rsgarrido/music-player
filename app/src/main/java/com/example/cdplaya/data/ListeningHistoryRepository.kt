package com.example.cdplaya.data

import com.example.cdplaya.data.local.SongPlayStatsDao
import com.example.cdplaya.data.local.SongPlayStatsEntity
import kotlin.math.max
import kotlin.math.min

class ListeningHistoryRepository(
    private val songPlayStatsDao: SongPlayStatsDao
) {
    suspend fun getRecentlyPlayed(): List<ListeningHistoryEntry> {
        return songPlayStatsDao.getRecentlyPlayed().map { entity ->
            entity.toListeningHistoryEntry()
        }
    }

    suspend fun getMostPlayed(): List<ListeningHistoryEntry> {
        return songPlayStatsDao.getMostPlayed().map { entity ->
            entity.toListeningHistoryEntry()
        }
    }

    suspend fun recordSongPlay(song: Song) {
        val songKey = song.stableKey()
        val now = System.currentTimeMillis()

        val existingStats = songPlayStatsDao.getStatsByKey(songKey)

        val updatedStats = if (existingStats == null) {
            SongPlayStatsEntity(
                songKey = songKey,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                playCount = 1,
                firstPlayedAt = now,
                lastPlayedAt = now
            )
        } else {
            existingStats.copy(
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                playCount = existingStats.playCount + 1,
                lastPlayedAt = now
            )
        }

        songPlayStatsDao.insertOrReplaceStats(updatedStats)
    }

    suspend fun updateSongReferenceAfterTagEdit(
        originalSong: Song,
        editedTags: EditableSongTags
    ) {
        val oldSongKey = originalSong.stableKey()

        val newTitle = editedTags.title.trim()
        val newArtist = editedTags.artist.trim()
        val newAlbum = editedTags.album.trim()

        val newSongKey = stableSongKey(
            title = newTitle,
            artist = newArtist,
            album = newAlbum,
            duration = originalSong.duration
        )

        val oldStats = songPlayStatsDao.getStatsByKey(oldSongKey)
            ?: return

        val existingNewStats = songPlayStatsDao.getStatsByKey(newSongKey)

        if (oldSongKey == newSongKey) {
            songPlayStatsDao.insertOrReplaceStats(
                oldStats.copy(
                    title = newTitle,
                    artist = newArtist,
                    album = newAlbum,
                    duration = originalSong.duration
                )
            )

            return
        }

        if (existingNewStats != null) {
            val mergedStats = existingNewStats.copy(
                title = newTitle,
                artist = newArtist,
                album = newAlbum,
                duration = originalSong.duration,
                playCount = existingNewStats.playCount + oldStats.playCount,
                firstPlayedAt = min(existingNewStats.firstPlayedAt, oldStats.firstPlayedAt),
                lastPlayedAt = max(existingNewStats.lastPlayedAt, oldStats.lastPlayedAt)
            )

            songPlayStatsDao.insertOrReplaceStats(mergedStats)
            songPlayStatsDao.deleteStatsByKey(oldSongKey)
            return
        }

        songPlayStatsDao.insertOrReplaceStats(
            oldStats.copy(
                songKey = newSongKey,
                title = newTitle,
                artist = newArtist,
                album = newAlbum,
                duration = originalSong.duration
            )
        )

        songPlayStatsDao.deleteStatsByKey(oldSongKey)
    }

    private fun SongPlayStatsEntity.toListeningHistoryEntry(): ListeningHistoryEntry {
        return ListeningHistoryEntry(
            songKey = songKey,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            playCount = playCount,
            firstPlayedAt = firstPlayedAt,
            lastPlayedAt = lastPlayedAt
        )
    }
}
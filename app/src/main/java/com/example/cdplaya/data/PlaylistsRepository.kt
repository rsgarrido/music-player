package com.example.cdplaya.data

import com.example.cdplaya.data.local.PlaylistDao
import com.example.cdplaya.data.local.PlaylistEntity
import com.example.cdplaya.data.local.PlaylistSongEntity

class PlaylistsRepository(
    private val playlistDao: PlaylistDao
) {
    suspend fun getPlaylists(): List<Playlist> {
        return playlistDao.getPlaylistsWithSongCount().map { playlist ->
            Playlist(
                playlistId = playlist.playlistId,
                name = playlist.name,
                songCount = playlist.songCount
            )
        }
    }

    suspend fun getPlaylistName(playlistId: Long): String {
        return playlistDao.getPlaylistById(playlistId)?.name ?: "Playlist"
    }

    suspend fun getPlaylistSongs(playlistId: Long): List<PlaylistSong> {
        return playlistDao.getPlaylistSongs(playlistId).map { playlistSong ->
            PlaylistSong(
                playlistSongId = playlistSong.playlistSongId,
                playlistId = playlistSong.playlistId,
                songKey = playlistSong.songKey,
                position = playlistSong.position,
                title = playlistSong.title,
                artist = playlistSong.artist,
                album = playlistSong.album,
                duration = playlistSong.duration
            )
        }
    }

    suspend fun createPlaylist(name: String) {
        val trimmedName = name.trim()

        if (trimmedName.isBlank()) {
            return
        }

        val now = System.currentTimeMillis()

        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = trimmedName,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(
        playlistId: Long,
        song: Song
    ) {
        val now = System.currentTimeMillis()
        val nextPosition = playlistDao.getLastPositionForPlaylist(playlistId) + 1

        playlistDao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songKey = song.stableKey(),
                position = nextPosition,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                addedAt = now
            )
        )

        playlistDao.updatePlaylistTimestamp(
            playlistId = playlistId,
            updatedAt = now
        )
    }

    suspend fun removePlaylistSong(
        playlistId: Long,
        playlistSongId: Long
    ) {
        playlistDao.deletePlaylistSong(playlistSongId)
        playlistDao.updatePlaylistTimestamp(
            playlistId = playlistId,
            updatedAt = System.currentTimeMillis()
        )
    }
}
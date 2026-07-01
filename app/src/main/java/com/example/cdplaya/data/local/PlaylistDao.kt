package com.example.cdplaya.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT 
            playlists.playlistId,
            playlists.name,
            playlists.createdAt,
            playlists.updatedAt,
            COUNT(playlist_songs.playlistSongId) AS songCount
        FROM playlists
        LEFT JOIN playlist_songs ON playlists.playlistId = playlist_songs.playlistId
        GROUP BY playlists.playlistId
        ORDER BY playlists.updatedAt DESC
        """
    )
    suspend fun getPlaylistsWithSongCount(): List<PlaylistWithSongCount>

    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongs(playlistId: Long): List<PlaylistSongEntity>

    @Query("SELECT COUNT(*) FROM playlists WHERE LOWER(name) = LOWER(:name)")
    suspend fun countPlaylistsWithName(name: String): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM playlists 
        WHERE LOWER(name) = LOWER(:name) 
        AND playlistId != :playlistId
        """
    )
    suspend fun countOtherPlaylistsWithName(
        playlistId: Long,
        name: String
    ): Int

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert
    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity): Long

    @Insert
    suspend fun insertPlaylistSongs(playlistSongs: List<PlaylistSongEntity>): List<Long>

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE playlistId = :playlistId")
    suspend fun renamePlaylist(
        playlistId: Long,
        name: String,
        updatedAt: Long
    )

    @Query(
        """
    UPDATE playlist_songs
    SET 
        songKey = :newSongKey,
        title = :title,
        artist = :artist,
        album = :album,
        duration = :duration
    WHERE songKey = :oldSongKey
    """
    )
    suspend fun updatePlaylistSongReferences(
        oldSongKey: String,
        newSongKey: String,
        title: String,
        artist: String,
        album: String,
        duration: Long
    )

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistSongId = :playlistSongId")
    suspend fun deletePlaylistSong(playlistSongId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getLastPositionForPlaylist(playlistId: Long): Int

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE playlistId = :playlistId")
    suspend fun updatePlaylistTimestamp(playlistId: Long, updatedAt: Long)
}
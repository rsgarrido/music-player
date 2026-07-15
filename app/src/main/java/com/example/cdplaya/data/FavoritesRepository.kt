package com.example.cdplaya.data

import com.example.cdplaya.data.backup.BackupFavoriteSong
import com.example.cdplaya.data.local.FavoriteSongDao
import com.example.cdplaya.data.local.FavoriteSongEntity

class FavoritesRepository(
    private val favoriteSongDao: FavoriteSongDao
) {
    suspend fun getFavoriteSongKeys(): Set<String> {
        return favoriteSongDao.getFavoriteSongKeys().toSet()
    }

    suspend fun getFavoritesForBackup(): List<BackupFavoriteSong> {
        return favoriteSongDao.getAllFavorites().map { favorite ->
            BackupFavoriteSong(
                songKey = favorite.songKey,
                title = favorite.title,
                artist = favorite.artist,
                album = favorite.album,
                duration = favorite.duration,
                createdAt = favorite.createdAt
            )
        }
    }

    suspend fun restoreFavoritesFromBackup(favorites: List<BackupFavoriteSong>) {
        favoriteSongDao.deleteAllFavorites()

        if (favorites.isEmpty()) {
            return
        }

        favoriteSongDao.insertFavorites(
            favorites.map { favorite ->
                FavoriteSongEntity(
                    songKey = favorite.songKey,
                    title = favorite.title,
                    artist = favorite.artist,
                    album = favorite.album,
                    duration = favorite.duration,
                    createdAt = favorite.createdAt
                )
            }
        )
    }

    suspend fun addFavorite(song: Song) {
        favoriteSongDao.insertFavorite(
            FavoriteSongEntity(
                songKey = song.favoriteKey(),
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFavorite(song: Song) {
        favoriteSongDao.deleteFavoriteByKey(song.favoriteKey())
    }

    suspend fun updateSongReferenceAfterTagEdit(
        originalSong: Song,
        editedTags: EditableSongTags
    ) {
        val oldSongKey = originalSong.favoriteKey()

        val newTitle = editedTags.title.trim()
        val newArtist = editedTags.artist.trim()
        val newAlbum = editedTags.album.trim()

        val newSongKey = stableSongKey(
            title = newTitle,
            artist = newArtist,
            album = newAlbum,
            duration = originalSong.duration
        )

        val oldFavoriteExists =
            favoriteSongDao.countFavoriteByKey(oldSongKey) > 0

        if (!oldFavoriteExists) {
            return
        }

        val newFavoriteAlreadyExists =
            oldSongKey != newSongKey &&
                    favoriteSongDao.countFavoriteByKey(newSongKey) > 0

        if (newFavoriteAlreadyExists) {
            favoriteSongDao.deleteFavoriteByKey(oldSongKey)
            return
        }

        favoriteSongDao.updateFavoriteSongReference(
            oldSongKey = oldSongKey,
            newSongKey = newSongKey,
            title = newTitle,
            artist = newArtist,
            album = newAlbum,
            duration = originalSong.duration
        )
    }
}

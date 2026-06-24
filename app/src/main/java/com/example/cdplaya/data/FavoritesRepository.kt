package com.example.cdplaya.data

import com.example.cdplaya.data.local.FavoriteSongDao
import com.example.cdplaya.data.local.FavoriteSongEntity

class FavoritesRepository(
    private val favoriteSongDao: FavoriteSongDao
) {
    suspend fun getFavoriteSongKeys(): Set<String> {
        return favoriteSongDao.getFavoriteSongKeys().toSet()
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
}
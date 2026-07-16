package com.example.cdplaya.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavoriteSongDao {

    @Query("SELECT * FROM favorite_songs ORDER BY createdAt DESC")
    suspend fun getAllFavorites(): List<FavoriteSongEntity>

    @Query("SELECT songKey FROM favorite_songs")
    suspend fun getFavoriteSongKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favoriteSong: FavoriteSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteSongEntity>)

    @Query("SELECT COUNT(*) FROM favorite_songs WHERE songKey = :songKey")
    suspend fun countFavoriteByKey(songKey: String): Int

    @Query(
        """
    UPDATE favorite_songs
    SET 
        songKey = :newSongKey,
        title = :title,
        artist = :artist,
        album = :album,
        duration = :duration
    WHERE songKey = :oldSongKey
    """
    )
    suspend fun updateFavoriteSongReference(
        oldSongKey: String,
        newSongKey: String,
        title: String,
        artist: String,
        album: String,
        duration: Long
    )

    @Query("DELETE FROM favorite_songs WHERE songKey = :songKey")
    suspend fun deleteFavoriteByKey(songKey: String)

    @Query("DELETE FROM favorite_songs")
    suspend fun deleteAllFavorites()
}

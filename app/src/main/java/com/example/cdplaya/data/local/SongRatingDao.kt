package com.example.cdplaya.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongRatingDao {
    @Query("SELECT * FROM song_ratings WHERE trackIdentityId = :trackIdentityId")
    suspend fun getByTrackIdentityId(trackIdentityId: Long): SongRatingEntity?

    @Query("SELECT * FROM song_ratings WHERE trackIdentityId = :trackIdentityId")
    fun observeByTrackIdentityId(trackIdentityId: Long): Flow<SongRatingEntity?>

    @Query("SELECT * FROM song_ratings WHERE trackIdentityId IN (:trackIdentityIds)")
    suspend fun getByTrackIdentityIds(trackIdentityIds: List<Long>): List<SongRatingEntity>

    @Query("SELECT * FROM song_ratings ORDER BY trackIdentityId ASC")
    fun observeAll(): Flow<List<SongRatingEntity>>

    @Query("SELECT * FROM song_ratings ORDER BY trackIdentityId ASC")
    suspend fun getAllForBackup(): List<SongRatingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: SongRatingEntity)

    @Insert
    suspend fun insert(ratings: List<SongRatingEntity>)

    @Query("DELETE FROM song_ratings WHERE trackIdentityId = :trackIdentityId")
    suspend fun deleteByTrackIdentityId(trackIdentityId: Long): Int

    @Query("DELETE FROM song_ratings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM song_ratings")
    suspend fun count(): Long
}

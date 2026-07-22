package com.example.cdplaya.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SongPlayStatsDao {

    @Query("SELECT * FROM song_play_stats ORDER BY lastPlayedAt DESC")
    suspend fun getRecentlyPlayed(): List<SongPlayStatsEntity>

    @Query("SELECT * FROM song_play_stats ORDER BY playCount DESC, lastPlayedAt DESC")
    suspend fun getMostPlayed(): List<SongPlayStatsEntity>

    @Query("SELECT * FROM song_play_stats WHERE referenceKey = :referenceKey LIMIT 1")
    suspend fun getStatsByReferenceKey(referenceKey: String): SongPlayStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceStats(songPlayStats: SongPlayStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceStats(stats: List<SongPlayStatsEntity>)

    @Query("DELETE FROM song_play_stats WHERE referenceKey = :referenceKey")
    suspend fun deleteStatsByReferenceKey(referenceKey: String)

    @Query("DELETE FROM song_play_stats")
    suspend fun deleteAllStats()
}

package com.example.cdplaya.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DatabaseMarkerEntity::class,
        FavoriteSongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        SongPlayStatsEntity::class,
        CachedSongEntity::class,
        ListeningTrackIdentityEntity::class,
        LocalTrackBindingEntity::class,
        ListeningEventEntity::class,
        LegacyListeningBaselineEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(ListeningHistoryTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteSongDao(): FavoriteSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songPlayStatsDao(): SongPlayStatsDao
    abstract fun cachedSongDao(): CachedSongDao
    abstract fun listeningTrackIdentityDao(): ListeningTrackIdentityDao
    abstract fun localTrackBindingDao(): LocalTrackBindingDao
    abstract fun listeningEventDao(): ListeningEventDao
    abstract fun legacyListeningBaselineDao(): LegacyListeningBaselineDao
}

package com.example.cdplaya.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListeningTrackIdentityDao {
    @Insert
    suspend fun insert(identity: ListeningTrackIdentityEntity): Long

    @Query("SELECT * FROM listening_track_identities WHERE id = :id")
    suspend fun getById(id: Long): ListeningTrackIdentityEntity?

    @Query("SELECT * FROM listening_track_identities ORDER BY id")
    suspend fun getAll(): List<ListeningTrackIdentityEntity>

    @Query("DELETE FROM listening_track_identities")
    suspend fun deleteAll()
}

@Dao
interface LocalTrackBindingDao {
    @Insert
    suspend fun insert(binding: LocalTrackBindingEntity): Long

    @Query("SELECT * FROM local_track_bindings WHERE id = :id")
    suspend fun getById(id: Long): LocalTrackBindingEntity?

    @Query("SELECT * FROM local_track_bindings WHERE referenceKey = :referenceKey LIMIT 1")
    suspend fun getByReferenceKey(referenceKey: String): LocalTrackBindingEntity?

    @Query("SELECT * FROM local_track_bindings WHERE trackIdentityId = :trackIdentityId ORDER BY id")
    suspend fun getForTrackIdentity(trackIdentityId: Long): List<LocalTrackBindingEntity>

    @Query("DELETE FROM local_track_bindings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM local_track_bindings ORDER BY trackIdentityId ASC, id ASC")
    suspend fun getAllForBackup(): List<LocalTrackBindingEntity>

    @Query("DELETE FROM local_track_bindings")
    suspend fun deleteAll()
}

@Dao
interface ListeningEventDao {
    @Insert
    suspend fun insert(event: ListeningEventEntity): Long

    @Insert
    suspend fun insert(events: List<ListeningEventEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringConflict(event: ListeningEventEntity): Long

    @Query("SELECT * FROM listening_events WHERE eventUuid = :eventUuid LIMIT 1")
    suspend fun getByUuid(eventUuid: String): ListeningEventEntity?

    @Query("SELECT * FROM listening_events WHERE playbackSessionId = :playbackSessionId LIMIT 1")
    suspend fun getByPlaybackSessionId(playbackSessionId: String): ListeningEventEntity?

    @Query("SELECT COUNT(*) FROM listening_events")
    suspend fun count(): Long

    @Query(
        "SELECT * FROM listening_events " +
            "ORDER BY startedAt ASC, id ASC LIMIT :limit OFFSET :offset"
    )
    suspend fun getBackupPage(limit: Int, offset: Int): List<ListeningEventEntity>

    @Query("DELETE FROM listening_events")
    suspend fun deleteAll()
}

@Dao
interface LegacyListeningBaselineDao {
    @Insert
    suspend fun insert(baseline: LegacyListeningBaselineEntity)

    @Insert
    suspend fun insert(baselines: List<LegacyListeningBaselineEntity>)

    @Query("SELECT * FROM legacy_listening_baselines WHERE trackIdentityId = :trackIdentityId")
    suspend fun getByTrackIdentityId(trackIdentityId: Long): LegacyListeningBaselineEntity?

    @Query("SELECT * FROM legacy_listening_baselines WHERE legacyReferenceKey = :referenceKey LIMIT 1")
    suspend fun getByLegacyReferenceKey(referenceKey: String): LegacyListeningBaselineEntity?

    @Query("SELECT COUNT(*) FROM legacy_listening_baselines")
    suspend fun count(): Long

    @Query("SELECT * FROM legacy_listening_baselines ORDER BY trackIdentityId ASC")
    suspend fun getAllForBackup(): List<LegacyListeningBaselineEntity>

    @Query("DELETE FROM legacy_listening_baselines")
    suspend fun deleteAll()
}

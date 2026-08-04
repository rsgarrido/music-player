package com.example.cdplaya.data

import androidx.room.withTransaction
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.SongRatingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SongRating(
    val trackIdentityId: Long,
    val value: Int,
    val ratedAt: Long,
    val updatedAt: Long
)

/** Identity-owned ratings. Favorites and legacy aggregate play statistics are deliberately absent. */
class SongRatingRepository(
    private val database: AppDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val nativeTrackResolver: ListeningNativeTrackResolver =
        ListeningNativeTrackResolver(database, nowMillis)
) {
    suspend fun getRating(trackIdentityId: Long): SongRating? =
        database.songRatingDao().getByTrackIdentityId(trackIdentityId)?.toDomain()

    fun observeRating(trackIdentityId: Long): Flow<SongRating?> =
        database.songRatingDao().observeByTrackIdentityId(trackIdentityId)
            .map { it?.toDomain() }

    fun observeAllRatings(): Flow<List<SongRating>> =
        database.songRatingDao().observeAll().map { rows -> rows.map(SongRatingEntity::toDomain) }

    suspend fun getRatings(trackIdentityIds: Collection<Long>): Map<Long, SongRating> {
        val uniqueIds = trackIdentityIds.distinct()
        if (uniqueIds.isEmpty()) return emptyMap()
        return uniqueIds.chunked(MAX_SQLITE_IN_ARGUMENTS)
            .flatMap { database.songRatingDao().getByTrackIdentityIds(it) }
            .associate { entity -> entity.trackIdentityId to entity.toDomain() }
    }

    suspend fun getRatingForSong(song: Song): SongRating? {
        val binding = database.localTrackBindingDao().getByReferenceKey(song.membershipKey())
            ?: return null
        return getRating(binding.trackIdentityId)
    }

    suspend fun setRating(song: Song, rating: Int): SongRating {
        validateRating(rating)
        return database.withTransaction {
            val resolved = nativeTrackResolver.resolveOrCreate(
                referenceKey = song.membershipKey(),
                reference = song.toSongReference()
            )
            setRatingWithinTransaction(resolved.trackIdentityId, rating)
        }
    }

    suspend fun setRating(trackIdentityId: Long, rating: Int): SongRating {
        validateRating(rating)
        return database.withTransaction {
            require(database.listeningTrackIdentityDao().getById(trackIdentityId) != null) {
                "Cannot rate a missing listening-track identity"
            }
            setRatingWithinTransaction(trackIdentityId, rating)
        }
    }

    suspend fun clearRating(song: Song): Boolean = database.withTransaction {
        val binding = database.localTrackBindingDao().getByReferenceKey(song.membershipKey())
            ?: return@withTransaction false
        database.songRatingDao().deleteByTrackIdentityId(binding.trackIdentityId) > 0
    }

    suspend fun clearRating(trackIdentityId: Long): Boolean =
        database.songRatingDao().deleteByTrackIdentityId(trackIdentityId) > 0

    private suspend fun setRatingWithinTransaction(trackIdentityId: Long, rating: Int): SongRating {
        val current = database.songRatingDao().getByTrackIdentityId(trackIdentityId)
        if (current?.rating == rating) return current.toDomain()
        val now = nowMillis()
        val entity = SongRatingEntity(
            trackIdentityId = trackIdentityId,
            rating = rating,
            ratedAt = current?.ratedAt ?: now,
            updatedAt = now
        )
        database.songRatingDao().upsert(entity)
        return entity.toDomain()
    }

    private fun validateRating(rating: Int) {
        require(rating in 1..5) { "Song rating must be between 1 and 5" }
    }

    private companion object {
        const val MAX_SQLITE_IN_ARGUMENTS = 900
    }
}

private fun SongRatingEntity.toDomain() = SongRating(
    trackIdentityId = trackIdentityId,
    value = rating,
    ratedAt = ratedAt,
    updatedAt = updatedAt
)

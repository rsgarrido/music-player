package com.example.cdplaya.data

import com.example.cdplaya.data.local.LegacyListeningBaselineDao
import com.example.cdplaya.data.local.LegacyListeningBaselineEntity
import com.example.cdplaya.data.local.ListeningEventDao
import com.example.cdplaya.data.local.ListeningEventEntity
import com.example.cdplaya.data.local.ListeningTrackIdentityDao
import com.example.cdplaya.data.local.ListeningTrackIdentityEntity
import com.example.cdplaya.data.local.LocalTrackBindingDao
import com.example.cdplaya.data.local.LocalTrackBindingEntity

class ListeningTrackIdentityRepository(
    private val identityDao: ListeningTrackIdentityDao,
    private val bindingDao: LocalTrackBindingDao
) {
    suspend fun insertIdentity(identity: ListeningTrackIdentityEntity): Long =
        identityDao.insert(identity)

    suspend fun getIdentity(id: Long): ListeningTrackIdentityEntity? = identityDao.getById(id)

    suspend fun insertLocalBinding(binding: LocalTrackBindingEntity): Long =
        bindingDao.insert(binding)

    suspend fun getLocalBinding(referenceKey: String): LocalTrackBindingEntity? =
        bindingDao.getByReferenceKey(referenceKey)
}

class ListeningEventRepository(
    private val eventDao: ListeningEventDao
) {
    suspend fun insert(event: ListeningEventEntity): Long = eventDao.insert(event)

    suspend fun getByUuid(eventUuid: String): ListeningEventEntity? =
        eventDao.getByUuid(eventUuid)

    suspend fun getByPlaybackSessionId(playbackSessionId: String): ListeningEventEntity? =
        eventDao.getByPlaybackSessionId(playbackSessionId)

    suspend fun count(): Long = eventDao.count()
}

class LegacyListeningBaselineRepository(
    private val baselineDao: LegacyListeningBaselineDao
) {
    suspend fun insert(baseline: LegacyListeningBaselineEntity) = baselineDao.insert(baseline)

    suspend fun getByTrackIdentityId(trackIdentityId: Long): LegacyListeningBaselineEntity? =
        baselineDao.getByTrackIdentityId(trackIdentityId)

    suspend fun count(): Long = baselineDao.count()
}

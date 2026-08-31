package com.furgnbi.android.data.repository

import com.furgnbi.android.data.local.dao.AlarmDao
import com.furgnbi.android.domain.model.AlarmEvent
import com.furgnbi.android.domain.model.AlarmType
import com.furgnbi.android.domain.model.toDomain
import com.furgnbi.android.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AlarmRepository {
    suspend fun save(alarm: AlarmEvent): Long
    suspend fun findActive(deviceId: String, alarmType: AlarmType): AlarmEvent?
    suspend fun markResolved(id: Long, resolvedAt: Long)
    suspend fun getUnpublished(limit: Int = 50): List<AlarmEvent>
    suspend fun markAsPublished(id: Long)
    fun observeActive(deviceId: String): Flow<List<AlarmEvent>>
    fun observeActiveCount(deviceId: String): Flow<Int>
}

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override suspend fun save(alarm: AlarmEvent): Long =
        alarmDao.insert(alarm.toEntity())

    override suspend fun findActive(deviceId: String, alarmType: AlarmType): AlarmEvent? =
        alarmDao.findActive(deviceId, alarmType.name)?.toDomain()

    override suspend fun markResolved(id: Long, resolvedAt: Long) =
        alarmDao.markResolved(id, resolvedAt)

    override suspend fun getUnpublished(limit: Int): List<AlarmEvent> =
        alarmDao.getUnpublished(limit).map { it.toDomain() }

    override suspend fun markAsPublished(id: Long) =
        alarmDao.markAsPublished(id)

    override fun observeActive(deviceId: String): Flow<List<AlarmEvent>> =
        alarmDao.observeActive(deviceId).map { list -> list.map { it.toDomain() } }

    override fun observeActiveCount(deviceId: String): Flow<Int> =
        alarmDao.observeActiveCount(deviceId)
}

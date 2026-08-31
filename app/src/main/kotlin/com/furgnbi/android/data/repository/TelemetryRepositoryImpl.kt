package com.furgnbi.android.data.repository

import com.furgnbi.android.data.local.dao.TelemetryDao
import com.furgnbi.android.domain.model.TelemetryReading
import com.furgnbi.android.domain.model.toDomain
import com.furgnbi.android.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface TelemetryRepository {
    suspend fun save(reading: TelemetryReading): Long
    suspend fun getLatest(deviceId: String): TelemetryReading?
    fun observeLatest(deviceId: String): Flow<TelemetryReading>
    suspend fun markAsPublished(id: Long)
    suspend fun getUnpublished(limit: Int = 50): List<TelemetryReading>
}

@Singleton
class TelemetryRepositoryImpl @Inject constructor(
    private val telemetryDao: TelemetryDao
) : TelemetryRepository {

    override suspend fun save(reading: TelemetryReading): Long =
        telemetryDao.insert(reading.toEntity())

    override suspend fun getLatest(deviceId: String): TelemetryReading? =
        telemetryDao.getLatest(deviceId)?.toDomain()

    override fun observeLatest(deviceId: String): Flow<TelemetryReading> =
        telemetryDao.observeLatest(deviceId).filterNotNull().map { it.toDomain() }

    override suspend fun markAsPublished(id: Long) =
        telemetryDao.markAsPublished(id)

    override suspend fun getUnpublished(limit: Int): List<TelemetryReading> =
        telemetryDao.getUnpublished(limit).map { it.toDomain() }
}

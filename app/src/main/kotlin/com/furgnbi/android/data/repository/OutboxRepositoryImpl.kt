package com.furgnbi.android.data.repository

import com.furgnbi.android.data.local.dao.OutboxDao
import com.furgnbi.android.domain.model.OutboxMessage
import com.furgnbi.android.domain.model.OutboxStatus
import com.furgnbi.android.domain.model.toEntity
import com.furgnbi.android.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface OutboxRepository {
    suspend fun enqueue(message: OutboxMessage): Long
    suspend fun fetchPending(limit: Int = 20): List<OutboxMessage>
    suspend fun markAsSent(id: Long)
    suspend fun incrementRetryCount(id: Long)
    suspend fun updateStatus(id: Long, status: OutboxStatus)
    suspend fun countPending(): Int
    fun observePendingCount(): Flow<Int>
    suspend fun pruneSent()
}

@Singleton
class OutboxRepositoryImpl @Inject constructor(
    private val outboxDao: OutboxDao
) : OutboxRepository {

    override suspend fun enqueue(message: OutboxMessage): Long =
        outboxDao.insert(message.toEntity())

    override suspend fun fetchPending(limit: Int): List<OutboxMessage> =
        outboxDao.fetchPending(limit).map { it.toDomain() }

    override suspend fun markAsSent(id: Long) =
        outboxDao.markAsSent(id)

    override suspend fun incrementRetryCount(id: Long) =
        outboxDao.incrementRetryCount(id)

    override suspend fun updateStatus(id: Long, status: OutboxStatus) =
        outboxDao.updateStatus(id, status.name)

    override suspend fun countPending(): Int =
        outboxDao.countPending()

    override fun observePendingCount(): Flow<Int> =
        outboxDao.observePendingCount()

    override suspend fun pruneSent() =
        outboxDao.pruneSent()
}

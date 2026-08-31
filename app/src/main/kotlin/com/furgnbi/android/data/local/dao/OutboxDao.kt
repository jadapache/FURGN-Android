package com.furgnbi.android.data.local.dao

import androidx.room.*
import com.furgnbi.android.data.local.entity.OutboxQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: OutboxQueueEntity): Long

    @Query("SELECT * FROM outbox_queue WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :limit")
    suspend fun fetchPending(limit: Int = 20): List<OutboxQueueEntity>

    @Query("UPDATE outbox_queue SET status = 'SENT', last_attempt_at = :now WHERE id = :id")
    suspend fun markAsSent(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE outbox_queue SET retry_count = retry_count + 1, last_attempt_at = :now WHERE id = :id")
    suspend fun incrementRetryCount(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE outbox_queue SET status = :status, last_attempt_at = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM outbox_queue WHERE status = 'PENDING'")
    suspend fun countPending(): Int

    @Query("SELECT COUNT(*) FROM outbox_queue WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM outbox_queue WHERE status = 'SENT'")
    suspend fun pruneSent()
}

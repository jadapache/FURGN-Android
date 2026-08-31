package com.furgnbi.android.data.local.dao

import androidx.room.*
import com.furgnbi.android.data.local.entity.AlarmRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AlarmRecordEntity): Long

    @Query("""
        SELECT * FROM alarm_records 
        WHERE device_id = :deviceId 
          AND alarm_type = :alarmType 
          AND resolved_at IS NULL 
        LIMIT 1
    """)
    suspend fun findActive(deviceId: String, alarmType: String): AlarmRecordEntity?

    @Query("UPDATE alarm_records SET resolved_at = :resolvedAt WHERE id = :id")
    suspend fun markResolved(id: Long, resolvedAt: Long)

    @Query("SELECT * FROM alarm_records WHERE is_published = 0 ORDER BY triggered_at ASC LIMIT :limit")
    suspend fun getUnpublished(limit: Int = 50): List<AlarmRecordEntity>

    @Query("UPDATE alarm_records SET is_published = 1 WHERE id = :id")
    suspend fun markAsPublished(id: Long)

    @Query("SELECT * FROM alarm_records WHERE device_id = :deviceId AND resolved_at IS NULL ORDER BY triggered_at DESC")
    fun observeActive(deviceId: String): Flow<List<AlarmRecordEntity>>

    @Query("SELECT COUNT(*) FROM alarm_records WHERE device_id = :deviceId AND resolved_at IS NULL")
    fun observeActiveCount(deviceId: String): Flow<Int>
}

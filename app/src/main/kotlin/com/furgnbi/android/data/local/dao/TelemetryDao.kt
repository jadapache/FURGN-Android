package com.furgnbi.android.data.local.dao

import androidx.room.*
import com.furgnbi.android.data.local.entity.TelemetryRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TelemetryRecordEntity): Long

    @Query("UPDATE telemetry_records SET is_published = 1 WHERE id = :id")
    suspend fun markAsPublished(id: Long)

    @Query("UPDATE telemetry_records SET is_published = 1 WHERE id IN (:ids)")
    suspend fun markAllAsPublished(ids: List<Long>)

    @Query("SELECT * FROM telemetry_records WHERE device_id = :deviceId ORDER BY captured_at DESC LIMIT 1")
    suspend fun getLatest(deviceId: String): TelemetryRecordEntity?

    @Query("SELECT * FROM telemetry_records WHERE device_id = :deviceId ORDER BY captured_at DESC LIMIT 1")
    fun observeLatest(deviceId: String): Flow<TelemetryRecordEntity?>

    @Query("SELECT * FROM telemetry_records WHERE is_published = 0 ORDER BY captured_at ASC LIMIT :limit")
    suspend fun getUnpublished(limit: Int = 50): List<TelemetryRecordEntity>

    @Query("SELECT COUNT(*) FROM telemetry_records WHERE is_published = 0")
    fun observeUnpublishedCount(): Flow<Int>
}

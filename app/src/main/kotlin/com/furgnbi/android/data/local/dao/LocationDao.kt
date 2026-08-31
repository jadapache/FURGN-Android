package com.furgnbi.android.data.local.dao

import androidx.room.*
import com.furgnbi.android.data.local.entity.LocationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: LocationRecordEntity): Long

    @Query("UPDATE location_records SET is_published = 1 WHERE id = :id")
    suspend fun markAsPublished(id: Long)

    @Query("SELECT * FROM location_records WHERE device_id = :deviceId ORDER BY captured_at DESC LIMIT 1")
    suspend fun getLastKnown(deviceId: String): LocationRecordEntity?

    @Query("SELECT * FROM location_records WHERE device_id = :deviceId ORDER BY captured_at DESC LIMIT 1")
    fun observeLastKnown(deviceId: String): Flow<LocationRecordEntity?>

    @Query("SELECT * FROM location_records WHERE is_published = 0 ORDER BY captured_at ASC LIMIT :limit")
    suspend fun getUnpublished(limit: Int = 50): List<LocationRecordEntity>
}

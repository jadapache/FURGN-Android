package com.furgnbi.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_records")
data class LocationRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
    @ColumnInfo(name = "speed_kmh") val speedKmh: Float?,
    @ColumnInfo(name = "heading") val heading: Float?,
    @ColumnInfo(name = "gps_available") val gpsAvailable: Boolean = true,
    @ColumnInfo(name = "captured_at") val capturedAt: Long,
    @ColumnInfo(name = "is_published") val isPublished: Boolean = false
)

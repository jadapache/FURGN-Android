package com.furgnbi.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_records")
data class AlarmRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "alarm_type") val alarmType: String,
    @ColumnInfo(name = "severity") val severity: String,
    @ColumnInfo(name = "trigger_value") val triggerValue: Float?,
    @ColumnInfo(name = "triggered_at") val triggeredAt: Long,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long? = null,
    @ColumnInfo(name = "is_published") val isPublished: Boolean = false
)

package com.furgnbi.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_records")
data class TelemetryRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "temperature_c") val temperatureC: Float?,
    @ColumnInfo(name = "temperature_2_c") val temperature2C: Float?,
    @ColumnInfo(name = "humidity_pct") val humidityPct: Float?,
    @ColumnInfo(name = "voltage_v") val voltageV: Float?,
    @ColumnInfo(name = "modbus_register") val modbusRegister: Int?,
    @ColumnInfo(name = "captured_at") val capturedAt: Long, // epoch millis
    @ColumnInfo(name = "is_published") val isPublished: Boolean = false
)

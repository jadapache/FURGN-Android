package com.furgnbi.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plc_parameters")
data class PlcParameterEntity(
    @PrimaryKey
    val id: Int, // 1–23 matching spec table
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "modbus_address") val modbusAddress: Int,
    @ColumnInfo(name = "modbus_data_type") val modbusDataType: String, // "REAL" | "UINT16"
    @ColumnInfo(name = "register_count") val registerCount: Int, // 2 for REAL, 1 for UINT16
    @ColumnInfo(name = "min_value") val minValue: Double,
    @ColumnInfo(name = "max_value") val maxValue: Double,
    @ColumnInfo(name = "unit") val unit: String,
    @ColumnInfo(name = "default_value") val defaultValue: Double,
    @ColumnInfo(name = "current_value") val currentValue: Double? = null,
    @ColumnInfo(name = "is_read_only") val isReadOnly: Boolean = false,
    @ColumnInfo(name = "last_read_at") val lastReadAt: Long? = null,
    @ColumnInfo(name = "description") val description: String = ""
)

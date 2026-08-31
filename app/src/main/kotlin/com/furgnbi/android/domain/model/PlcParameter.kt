package com.furgnbi.android.domain.model

data class PlcParameter(
    val id: Int,
    val name: String,
    val modbusAddress: Int,
    val modbusDataType: ModbusDataType,
    val registerCount: Int,
    val minValue: Double,
    val maxValue: Double,
    val unit: String,
    val defaultValue: Double,
    val currentValue: Double? = null,
    val isReadOnly: Boolean = false,
    val lastReadAt: Long? = null,
    val description: String = ""
)

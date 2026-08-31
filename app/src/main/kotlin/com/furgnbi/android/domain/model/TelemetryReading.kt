package com.furgnbi.android.domain.model

data class TelemetryReading(
    val deviceId: String,
    val temperatureC: Float?,
    val temperature2C: Float?,
    val humidityPct: Float?,
    val voltageV: Float?,
    val modbusRegister: Int?,
    val capturedAt: Long, // epoch millis
    // Coil states read in same Modbus cycle
    val fan1On: Boolean = false,
    val fan2On: Boolean = false,
    val fan3On: Boolean = false,
    val fan4On: Boolean = false,
    val fan5On: Boolean = false,
    val fan6On: Boolean = false,
    val gateOpen: Boolean = false,
    val alarmActive: Boolean = false
)

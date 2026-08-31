package com.furgnbi.android.domain.model

enum class AlarmType {
    HIGH_TEMP,
    HIGH_TEMP_2,
    HIGH_HUMIDITY,
    LOW_VOLTAGE,
    LOW_VOLTAGE_CRITICAL
}

enum class AlarmSeverity {
    WARNING,
    CRITICAL
}

enum class OutboxStatus {
    PENDING,
    SENT,
    FAILED
}

enum class ModbusDataType {
    REAL,    // IEEE 754 float — 2 holding registers
    UINT16   // Unsigned 16-bit integer — 1 holding register
}

enum class ModbusConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class MqttConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

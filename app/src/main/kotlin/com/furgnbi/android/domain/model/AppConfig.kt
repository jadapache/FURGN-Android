package com.furgnbi.android.domain.model

data class AppConfig(
    val deviceId: String = "FURG-001-AND",
    val brokerUrl: String = "tcp://broker.furgnbi.com:1883",
    val mqttUsername: String = "",
    val mqttPassword: String = "",
    val mqttClientId: String = "android_${System.currentTimeMillis()}",
    val modbusHost: String = "192.168.1.100",
    val modbusPort: Int = 502,
    val modbusUnitId: Int = 1,
    val samplingIntervalMs: Long = 5_000L,
    val publishIntervalMs: Long = 10_000L
)

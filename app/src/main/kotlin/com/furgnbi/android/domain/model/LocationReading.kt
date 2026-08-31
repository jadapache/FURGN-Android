package com.furgnbi.android.domain.model

data class LocationReading(
    val deviceId: String,
    val latitude: Double?,
    val longitude: Double?,
    val speedKmh: Float?,
    val heading: Float?,
    val gpsAvailable: Boolean,
    val capturedAt: Long // epoch millis
)

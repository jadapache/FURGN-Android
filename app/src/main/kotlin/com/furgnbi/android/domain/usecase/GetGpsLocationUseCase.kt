package com.furgnbi.android.domain.usecase

import android.util.Log
import com.furgnbi.android.data.local.AppDatabase
import com.furgnbi.android.data.repository.AppConfigRepository
import com.furgnbi.android.data.repository.GpsRepository
import com.furgnbi.android.data.repository.OutboxRepository
import com.furgnbi.android.di.IoDispatcher
import com.furgnbi.android.domain.model.LocationReading
import com.furgnbi.android.domain.model.OutboxStatus
import com.furgnbi.android.domain.model.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides a reactive stream of GPS location readings.
 * Each emission also persists the reading atomically (location_records + outbox_queue).
 * Handles permission denial and GPS unavailability gracefully (Requisito 3.1–3.5).
 */
@Singleton
class GetGpsLocationUseCase @Inject constructor(
    private val gpsRepository: GpsRepository,
    private val appConfigRepository: AppConfigRepository,
    private val appDatabase: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    operator fun invoke(deviceId: String): Flow<LocationReading> =
        gpsRepository.observeLocation(deviceId)
            .onEach { reading -> persistLocation(reading) }
            .catch { e ->
                Log.e("GetGpsLocationUseCase", "GPS stream error: ${e.message}")
                // Emit a null-coordinates reading to signal GPS unavailability
                emit(LocationReading(
                    deviceId = deviceId,
                    latitude = null,
                    longitude = null,
                    speedKmh = null,
                    heading = null,
                    gpsAvailable = false,
                    capturedAt = System.currentTimeMillis()
                ))
            }

    private suspend fun persistLocation(reading: LocationReading) {
        val config = appConfigRepository.getConfig()
        runCatching {
            appDatabase.runInTransaction {
                val payload = buildLocationPayload(reading, config.deviceId)
                kotlinx.coroutines.runBlocking {
                    appDatabase.locationDao().insert(reading.toEntity())
                    appDatabase.outboxDao().insert(
                        com.furgnbi.android.data.local.entity.OutboxQueueEntity(
                            topic = "furgones/${config.deviceId}/location",
                            payloadJson = payload,
                            qos = 1,
                            status = OutboxStatus.PENDING.name,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }.onFailure { e ->
            Log.e("GetGpsLocationUseCase", "Failed to persist location: ${e.message}")
        }
    }

    private fun buildLocationPayload(reading: LocationReading, deviceId: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return """{"device_id":"$deviceId","timestamp":"${sdf.format(Date(reading.capturedAt))}","latitude":${reading.latitude},"longitude":${reading.longitude},"speed_kmh":${reading.speedKmh},"heading":${reading.heading},"gps_available":${reading.gpsAvailable}}"""
    }
}

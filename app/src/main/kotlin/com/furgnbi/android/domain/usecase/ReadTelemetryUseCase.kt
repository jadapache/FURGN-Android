package com.furgnbi.android.domain.usecase

import android.util.Log
import com.furgnbi.android.data.local.AppDatabase
import com.furgnbi.android.data.modbus.ModbusRepository
import com.furgnbi.android.data.repository.AppConfigRepository
import com.furgnbi.android.data.repository.OutboxRepository
import com.furgnbi.android.data.repository.TelemetryRepository
import com.furgnbi.android.di.IoDispatcher
import com.furgnbi.android.domain.model.OutboxMessage
import com.furgnbi.android.domain.model.OutboxStatus
import com.furgnbi.android.domain.model.TelemetryReading
import com.furgnbi.android.domain.model.toEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads telemetry from the PLC via Modbus and persists it atomically:
 * - inserts one row in telemetry_records
 * - inserts one row in outbox_queue (PENDING)
 * all within a single Room transaction (Correctness Property 1, Requisito 4.1–4.4).
 */
@Singleton
class ReadTelemetryUseCase @Inject constructor(
    private val modbusRepository: ModbusRepository,
    private val telemetryRepository: TelemetryRepository,
    private val outboxRepository: OutboxRepository,
    private val appConfigRepository: AppConfigRepository,
    private val appDatabase: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend operator fun invoke(): Result<TelemetryReading> = withContext(ioDispatcher) {
        runCatching {
            val config = appConfigRepository.getConfig()

            // 1. Read from PLC via Modbus (FC04 + FC01)
            val reading = modbusRepository.readTelemetry().copy(deviceId = config.deviceId)

            // 2. Build MQTT payload
            val payload = buildTelemetryPayload(reading)

            // 3. Persist in a single atomic Room transaction
            appDatabase.runInTransaction {
                // Insert telemetry record
                appDatabase.telemetryDao().let { dao ->
                    kotlinx.coroutines.runBlocking {
                        dao.insert(reading.toEntity())
                    }
                }
                // Insert outbox entry
                appDatabase.outboxDao().let { dao ->
                    kotlinx.coroutines.runBlocking {
                        dao.insert(
                            com.furgnbi.android.data.local.entity.OutboxQueueEntity(
                                topic = "furgones/${config.deviceId}/telemetry",
                                payloadJson = payload,
                                qos = 1,
                                retryCount = 0,
                                status = OutboxStatus.PENDING.name,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            reading
        }.onFailure { e ->
            Log.e("ReadTelemetryUseCase", "Failed to read telemetry: ${e.message}")
        }
    }

    private fun buildTelemetryPayload(reading: TelemetryReading): String {
        val adapter = moshi.adapter(Map::class.java)
        val map = mapOf(
            "device_id" to reading.deviceId,
            "timestamp" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(reading.capturedAt)),
            "temperature_c" to reading.temperatureC,
            "temperature_2_c" to reading.temperature2C,
            "humidity_pct" to reading.humidityPct,
            "voltage_v" to reading.voltageV,
            "modbus_register" to reading.modbusRegister
        )
        return adapter.toJson(map)
    }
}

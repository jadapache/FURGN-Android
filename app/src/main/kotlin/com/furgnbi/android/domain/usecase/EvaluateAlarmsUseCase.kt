package com.furgnbi.android.domain.usecase

import android.util.Log
import com.furgnbi.android.data.local.AppDatabase
import com.furgnbi.android.data.repository.AlarmRepository
import com.furgnbi.android.data.repository.AppConfigRepository
import com.furgnbi.android.data.repository.ParameterRepository
import com.furgnbi.android.di.IoDispatcher
import com.furgnbi.android.domain.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates a TelemetryReading against PLC parameter thresholds.
 * Generates alarm events for threshold breaches.
 * Implements anti-duplication: skips insertion if an active alarm of the same type exists.
 * Both alarm_records and outbox_queue insertions happen in a single Room transaction (Property 1 / Requisito 9.1–9.5).
 *
 * Default thresholds (used when ParameterRepository has no stored value yet):
 *   HIGH_TEMP    warning  ≥ 35.0°C   (param 18 threshold)
 *   HIGH_TEMP    critical ≥ 38.0°C
 *   HIGH_HUMIDITY warning ≥ 80.0%    (param 19 threshold)
 *   LOW_VOLTAGE  warning  < 22.0V    (param 7 threshold)
 */
@Singleton
class EvaluateAlarmsUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val parameterRepository: ParameterRepository,
    private val appConfigRepository: AppConfigRepository,
    private val appDatabase: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val DEFAULT_TEMP_CRITICAL = 38.0
        private const val DEFAULT_TEMP_WARNING  = 35.0
        private const val DEFAULT_HUM_WARNING   = 80.0
        private const val DEFAULT_VOLT_WARNING  = 22.0
    }

    suspend operator fun invoke(reading: TelemetryReading): List<AlarmEvent> =
        withContext(ioDispatcher) {
            val config = appConfigRepository.getConfig()
            val deviceId = config.deviceId

            val tempCriticalThreshold = parameterRepository.getParameter(18)
                ?.currentValue ?: DEFAULT_TEMP_CRITICAL
            val tempWarningThreshold  = parameterRepository.getParameter(18)
                ?.minValue ?: DEFAULT_TEMP_WARNING
            val humWarningThreshold   = parameterRepository.getParameter(19)
                ?.currentValue ?: DEFAULT_HUM_WARNING
            val voltWarningThreshold  = parameterRepository.getParameter(7)
                ?.currentValue ?: DEFAULT_VOLT_WARNING

            val pendingAlarms = mutableListOf<AlarmEvent>()

            // ── Temperature evaluation ──────────────────────────────────────
            reading.temperatureC?.let { temp ->
                val type = AlarmType.HIGH_TEMP
                val existing = alarmRepository.findActive(deviceId, type)
                if (temp >= tempCriticalThreshold && existing == null) {
                    pendingAlarms.add(AlarmEvent(
                        deviceId = deviceId,
                        alarmType = type,
                        severity = AlarmSeverity.CRITICAL,
                        triggerValue = temp,
                        triggeredAt = reading.capturedAt
                    ))
                } else if (temp >= tempWarningThreshold && temp < tempCriticalThreshold && existing == null) {
                    pendingAlarms.add(AlarmEvent(
                        deviceId = deviceId,
                        alarmType = type,
                        severity = AlarmSeverity.WARNING,
                        triggerValue = temp,
                        triggeredAt = reading.capturedAt
                    ))
                }
            }

            // ── Humidity evaluation ─────────────────────────────────────────
            reading.humidityPct?.let { hum ->
                val type = AlarmType.HIGH_HUMIDITY
                val existing = alarmRepository.findActive(deviceId, type)
                if (hum >= humWarningThreshold && existing == null) {
                    pendingAlarms.add(AlarmEvent(
                        deviceId = deviceId,
                        alarmType = type,
                        severity = AlarmSeverity.WARNING,
                        triggerValue = hum,
                        triggeredAt = reading.capturedAt
                    ))
                }
            }

            // ── Voltage evaluation ──────────────────────────────────────────
            reading.voltageV?.let { volt ->
                val type = AlarmType.LOW_VOLTAGE
                val existing = alarmRepository.findActive(deviceId, type)
                if (volt < voltWarningThreshold && existing == null) {
                    pendingAlarms.add(AlarmEvent(
                        deviceId = deviceId,
                        alarmType = type,
                        severity = AlarmSeverity.WARNING,
                        triggerValue = volt,
                        triggeredAt = reading.capturedAt
                    ))
                }
            }

            // ── Persist in atomic Room transaction ──────────────────────────
            if (pendingAlarms.isNotEmpty()) {
                appDatabase.runInTransaction {
                    pendingAlarms.forEach { alarm ->
                        kotlinx.coroutines.runBlocking {
                            val id = appDatabase.alarmDao().insert(alarm.toEntity())
                            appDatabase.outboxDao().insert(
                                com.furgnbi.android.data.local.entity.OutboxQueueEntity(
                                    topic = "furgones/${deviceId}/alarms",
                                    payloadJson = buildAlarmPayload(alarm),
                                    qos = 1,
                                    status = OutboxStatus.PENDING.name,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }

            pendingAlarms
        }

    private fun buildAlarmPayload(alarm: AlarmEvent): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return """{"device_id":"${alarm.deviceId}","alarm_type":"${alarm.alarmType.name}","severity":"${alarm.severity.name}","trigger_value":${alarm.triggerValue},"triggered_at":"${sdf.format(java.util.Date(alarm.triggeredAt))}"}"""
    }
}

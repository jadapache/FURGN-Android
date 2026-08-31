package com.furgnbi.android.domain.model

import com.furgnbi.android.data.local.entity.*

// ── TelemetryReading ────────────────────────────────────────────────────────

fun TelemetryReading.toEntity(): TelemetryRecordEntity = TelemetryRecordEntity(
    deviceId = deviceId,
    temperatureC = temperatureC,
    temperature2C = temperature2C,
    humidityPct = humidityPct,
    voltageV = voltageV,
    modbusRegister = modbusRegister,
    capturedAt = capturedAt,
    isPublished = false
)

fun TelemetryRecordEntity.toDomain(): TelemetryReading = TelemetryReading(
    deviceId = deviceId,
    temperatureC = temperatureC,
    temperature2C = temperature2C,
    humidityPct = humidityPct,
    voltageV = voltageV,
    modbusRegister = modbusRegister,
    capturedAt = capturedAt
)

// ── LocationReading ─────────────────────────────────────────────────────────

fun LocationReading.toEntity(): LocationRecordEntity = LocationRecordEntity(
    deviceId = deviceId,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    heading = heading,
    gpsAvailable = gpsAvailable,
    capturedAt = capturedAt,
    isPublished = false
)

fun LocationRecordEntity.toDomain(): LocationReading = LocationReading(
    deviceId = deviceId,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    heading = heading,
    gpsAvailable = gpsAvailable,
    capturedAt = capturedAt
)

// ── AlarmEvent ──────────────────────────────────────────────────────────────

fun AlarmEvent.toEntity(): AlarmRecordEntity = AlarmRecordEntity(
    deviceId = deviceId,
    alarmType = alarmType.name,
    severity = severity.name,
    triggerValue = triggerValue,
    triggeredAt = triggeredAt,
    resolvedAt = resolvedAt,
    isPublished = isPublished
)

fun AlarmRecordEntity.toDomain(): AlarmEvent = AlarmEvent(
    id = id,
    deviceId = deviceId,
    alarmType = AlarmType.valueOf(alarmType),
    severity = AlarmSeverity.valueOf(severity),
    triggerValue = triggerValue,
    triggeredAt = triggeredAt,
    resolvedAt = resolvedAt,
    isPublished = isPublished
)

// ── OutboxMessage ───────────────────────────────────────────────────────────

fun OutboxMessage.toEntity(): OutboxQueueEntity = OutboxQueueEntity(
    topic = topic,
    payloadJson = payloadJson,
    qos = qos,
    retryCount = retryCount,
    status = status.name,
    createdAt = createdAt,
    lastAttemptAt = lastAttemptAt
)

fun OutboxQueueEntity.toDomain(): OutboxMessage = OutboxMessage(
    id = id,
    topic = topic,
    payloadJson = payloadJson,
    qos = qos,
    retryCount = retryCount,
    status = OutboxStatus.valueOf(status),
    createdAt = createdAt,
    lastAttemptAt = lastAttemptAt
)

// ── PlcParameter ────────────────────────────────────────────────────────────

fun PlcParameter.toEntity(): PlcParameterEntity = PlcParameterEntity(
    id = id,
    name = name,
    modbusAddress = modbusAddress,
    modbusDataType = modbusDataType.name,
    registerCount = registerCount,
    minValue = minValue,
    maxValue = maxValue,
    unit = unit,
    defaultValue = defaultValue,
    currentValue = currentValue,
    isReadOnly = isReadOnly,
    lastReadAt = lastReadAt,
    description = description
)

fun PlcParameterEntity.toDomain(): PlcParameter = PlcParameter(
    id = id,
    name = name,
    modbusAddress = modbusAddress,
    modbusDataType = ModbusDataType.valueOf(modbusDataType),
    registerCount = registerCount,
    minValue = minValue,
    maxValue = maxValue,
    unit = unit,
    defaultValue = defaultValue,
    currentValue = currentValue,
    isReadOnly = isReadOnly,
    lastReadAt = lastReadAt,
    description = description
)

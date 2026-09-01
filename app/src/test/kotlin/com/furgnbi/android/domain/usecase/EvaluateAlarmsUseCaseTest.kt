package com.furgnbi.android.domain.usecase

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.furgnbi.android.data.local.AppDatabase
import com.furgnbi.android.data.local.dao.*
import com.furgnbi.android.data.local.entity.*
import com.furgnbi.android.data.repository.AlarmRepository
import com.furgnbi.android.data.repository.AppConfigRepository
import com.furgnbi.android.data.repository.ParameterRepository
import com.furgnbi.android.domain.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EvaluateAlarmsUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeAlarmDao : AlarmDao {
        val inserted = mutableListOf<AlarmRecordEntity>()
        override suspend fun insert(entity: AlarmRecordEntity): Long {
            inserted.add(entity)
            return inserted.size.toLong()
        }
        override suspend fun findActive(deviceId: String, alarmType: String): AlarmRecordEntity? = null
        override suspend fun markResolved(id: Long, resolvedAt: Long) {}
        override suspend fun getUnpublished(limit: Int): List<AlarmRecordEntity> = emptyList()
        override suspend fun markAsPublished(id: Long) {}
        override fun observeActive(deviceId: String): Flow<List<AlarmRecordEntity>> = flowOf(emptyList())
        override fun observeActiveCount(deviceId: String): Flow<Int> = flowOf(0)
    }

    class FakeOutboxDao : OutboxDao {
        val inserted = mutableListOf<OutboxQueueEntity>()
        override suspend fun insert(entity: OutboxQueueEntity): Long {
            inserted.add(entity)
            return inserted.size.toLong()
        }
        override suspend fun fetchPending(limit: Int): List<OutboxQueueEntity> = emptyList()
        override suspend fun markAsSent(id: Long, now: Long) {}
        override suspend fun incrementRetryCount(id: Long, now: Long) {}
        override suspend fun updateStatus(id: Long, status: String, now: Long) {}
        override suspend fun countPending(): Int = 0
        override fun observePendingCount(): Flow<Int> = flowOf(0)
        override suspend fun pruneSent() {}
    }

    class FakeDatabase : AppDatabase() {
        val fakeAlarmDao = FakeAlarmDao()
        val fakeOutboxDao = FakeOutboxDao()

        override fun telemetryDao(): TelemetryDao = throw UnsupportedOperationException()
        override fun locationDao(): LocationDao = throw UnsupportedOperationException()
        override fun alarmDao(): AlarmDao = fakeAlarmDao
        override fun outboxDao(): OutboxDao = fakeOutboxDao
        override fun plcParameterDao(): PlcParameterDao = throw UnsupportedOperationException()
        override fun appConfigDao(): AppConfigDao = throw UnsupportedOperationException()

        override fun runInTransaction(body: Runnable) {
            body.run()
        }

        override fun createInvalidationTracker(): InvalidationTracker {
            return InvalidationTracker(this, emptyMap(), emptyMap(), "alarm_records", "outbox_queue")
        }

        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
            throw UnsupportedOperationException()
        }

        override fun clearAllTables() {}
    }

    class FakeAlarmRepository : AlarmRepository {
        val alarms = mutableListOf<AlarmEvent>()

        override suspend fun save(alarm: AlarmEvent): Long {
            alarms.add(alarm)
            return alarms.size.toLong()
        }

        override suspend fun findActive(deviceId: String, alarmType: AlarmType): AlarmEvent? {
            return alarms.find { it.deviceId == deviceId && it.alarmType == alarmType && it.resolvedAt == null }
        }

        override suspend fun markResolved(id: Long, resolvedAt: Long) {}
        override suspend fun getUnpublished(limit: Int): List<AlarmEvent> = emptyList()
        override suspend fun markAsPublished(id: Long) {}
        override fun observeActive(deviceId: String): Flow<List<AlarmEvent>> = flowOf(alarms.filter { it.resolvedAt == null })
        override fun observeActiveCount(deviceId: String): Flow<Int> = flowOf(alarms.count { it.resolvedAt == null })
    }

    class FakeParameterRepository : ParameterRepository {
        override suspend fun getAllParameters(): List<PlcParameter> = emptyList()
        override fun observeParameters(): Flow<List<PlcParameter>> = flowOf(emptyList())
        override suspend fun getParameter(id: Int): PlcParameter? = when (id) {
            18 -> PlcParameter(
                id = 18,
                name = "Alarma Temp Alta",
                modbusAddress = 40028,
                modbusDataType = ModbusDataType.REAL,
                registerCount = 2,
                minValue = 35.0,
                maxValue = 50.0,
                unit = "°C",
                defaultValue = 35.0,
                currentValue = 38.0
            )
            19 -> PlcParameter(
                id = 19,
                name = "Alarma Humedad",
                modbusAddress = 40030,
                modbusDataType = ModbusDataType.REAL,
                registerCount = 2,
                minValue = 50.0,
                maxValue = 100.0,
                unit = "%",
                defaultValue = 80.0,
                currentValue = 80.0
            )
            7 -> PlcParameter(
                id = 7,
                name = "Umbral Voltaje",
                modbusAddress = 40011,
                modbusDataType = ModbusDataType.REAL,
                registerCount = 2,
                minValue = 10.0,
                maxValue = 30.0,
                unit = "V",
                defaultValue = 22.0,
                currentValue = 22.0
            )
            else -> null
        }
        override suspend fun updateParameter(parameter: PlcParameter) {}
        override suspend fun updateValue(id: Int, value: Double) {}
        override suspend fun insertAll(parameters: List<PlcParameter>) {}
        override suspend fun count(): Int = 0
    }

    class FakeAppConfigRepository : AppConfigRepository {
        override fun getConfig(): AppConfig = AppConfig(deviceId = "TEST-FURG-01")
        override fun saveConfig(config: AppConfig) {}
        override fun getValue(key: String): String? = null
        override fun setValue(key: String, value: String) {}
    }

    @Test
    fun testHighTemperatureTriggersWarningAlarm() = runTest(testDispatcher) {
        val alarmRepo = FakeAlarmRepository()
        val paramRepo = FakeParameterRepository()
        val configRepo = FakeAppConfigRepository()
        val db = FakeDatabase()

        val useCase = EvaluateAlarmsUseCase(alarmRepo, paramRepo, configRepo, db, testDispatcher)

        val reading = TelemetryReading(
            deviceId = "TEST-FURG-01",
            temperatureC = 36.5f, // >= 35.0 warning threshold
            temperature2C = 4.0f,
            humidityPct = 60.0f,
            voltageV = 24.0f,
            modbusRegister = 0,
            capturedAt = 1000L
        )

        val alarms = useCase(reading)
        assertEquals(1, alarms.size)
        assertEquals(AlarmType.HIGH_TEMP, alarms[0].alarmType)
        assertEquals(AlarmSeverity.WARNING, alarms[0].severity)
        assertEquals(1, db.fakeAlarmDao.inserted.size)
        assertEquals(1, db.fakeOutboxDao.inserted.size)
    }

    @Test
    fun testAntiDuplicationDoesNotCreateDuplicateActiveAlarm() = runTest(testDispatcher) {
        val alarmRepo = FakeAlarmRepository()
        val paramRepo = FakeParameterRepository()
        val configRepo = FakeAppConfigRepository()
        val db = FakeDatabase()

        // Seed an active high temp alarm
        alarmRepo.save(AlarmEvent(
            deviceId = "TEST-FURG-01",
            alarmType = AlarmType.HIGH_TEMP,
            severity = AlarmSeverity.WARNING,
            triggerValue = 36.0f,
            triggeredAt = 500L,
            resolvedAt = null
        ))

        val useCase = EvaluateAlarmsUseCase(alarmRepo, paramRepo, configRepo, db, testDispatcher)

        val reading = TelemetryReading(
            deviceId = "TEST-FURG-01",
            temperatureC = 37.0f, // still high
            temperature2C = 4.0f,
            humidityPct = 60.0f,
            voltageV = 24.0f,
            modbusRegister = 0,
            capturedAt = 1000L
        )

        val triggeredAlarms = useCase(reading)
        // Should not trigger a second duplicate alarm
        assertEquals(0, triggeredAlarms.size)
        assertEquals(0, db.fakeAlarmDao.inserted.size)
    }
}

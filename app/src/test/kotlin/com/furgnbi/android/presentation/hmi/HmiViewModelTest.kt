package com.furgnbi.android.presentation.hmi

import com.furgnbi.android.data.modbus.ModbusRepository
import com.furgnbi.android.data.repository.*
import com.furgnbi.android.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class HmiViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    class FakeTelemetryRepo : TelemetryRepository {
        val latest = MutableStateFlow<TelemetryReading?>(null)
        override suspend fun save(reading: TelemetryReading): Long = 1L
        override suspend fun getLatest(deviceId: String): TelemetryReading? = latest.value
        override fun observeLatest(deviceId: String): Flow<TelemetryReading> = flowOf()
        override suspend fun markAsPublished(id: Long) {}
        override suspend fun getUnpublished(limit: Int): List<TelemetryReading> = emptyList()
    }

    class FakeAlarmRepo : AlarmRepository {
        override suspend fun save(alarm: AlarmEvent): Long = 1L
        override suspend fun findActive(deviceId: String, alarmType: AlarmType): AlarmEvent? = null
        override suspend fun markResolved(id: Long, resolvedAt: Long) {}
        override suspend fun getUnpublished(limit: Int): List<AlarmEvent> = emptyList()
        override suspend fun markAsPublished(id: Long) {}
        override fun observeActive(deviceId: String): Flow<List<AlarmEvent>> = flowOf(emptyList())
        override fun observeActiveCount(deviceId: String): Flow<Int> = flowOf(0)
    }

    class FakeGpsRepo : GpsRepository {
        override fun observeLocation(deviceId: String): Flow<LocationReading> = flowOf()
        override suspend fun getLastKnownLocation(deviceId: String): LocationReading? = null
        override val isGpsAvailable: StateFlow<Boolean> = MutableStateFlow(true)
    }

    class FakeMqttRepo : MqttRepository {
        override val connectionState: StateFlow<MqttConnectionState> = MutableStateFlow(MqttConnectionState.CONNECTED)
        override suspend fun connect() {}
        override suspend fun disconnect() {}
        override suspend fun publish(topic: String, payload: String, qos: Int) {}
    }

    class FakeModbusRepo : ModbusRepository {
        override val connectionState: StateFlow<ModbusConnectionState> = MutableStateFlow(ModbusConnectionState.CONNECTED)
        override suspend fun connect(host: String, port: Int, unitId: Int) {}
        override suspend fun disconnect() {}
        override suspend fun readTelemetry(): TelemetryReading = TelemetryReading("1", 0f, 0f, 0f, 0f, 0, 0L)
        override suspend fun readCoilStates(): Map<String, Boolean> = emptyMap()
        override suspend fun readHoldingRegisterFloat(address: Int): Float = 0f
        override suspend fun readHoldingRegisterUInt16(address: Int): Int = 0
        override suspend fun writeHoldingRegisterFloat(address: Int, value: Float) {}
        override suspend fun writeHoldingRegisterUInt16(address: Int, value: Int) {}
        override suspend fun writeSingleCoil(address: Int, value: Boolean) {}
    }

    class FakeAppConfigRepo : AppConfigRepository {
        override fun getConfig(): AppConfig = AppConfig(deviceId = "FURG-999-TEST")
        override fun saveConfig(config: AppConfig) {}
        override fun getValue(key: String): String? = null
        override fun setValue(key: String, value: String) {}
    }

    @Test
    fun testUpdateFromTelemetryProperty10() {
        val vm = HmiViewModel(
            FakeTelemetryRepo(),
            FakeAlarmRepo(),
            FakeGpsRepo(),
            FakeMqttRepo(),
            FakeModbusRepo(),
            FakeAppConfigRepo()
        )

        val random = Random(999)
        for (i in 0 until 100) {
            val f1 = random.nextBoolean()
            val f2 = random.nextBoolean()
            val f3 = random.nextBoolean()
            val f4 = random.nextBoolean()
            val f5 = random.nextBoolean()
            val f6 = random.nextBoolean()
            val gate = random.nextBoolean()
            val t1 = random.nextFloat() * 40f - 20f
            val t2 = random.nextFloat() * 20f
            val hum = random.nextFloat() * 100f
            val volt = random.nextFloat() * 15f + 15f

            val reading = TelemetryReading(
                deviceId = "FURG-999-TEST",
                temperatureC = t1,
                temperature2C = t2,
                humidityPct = hum,
                voltageV = volt,
                modbusRegister = 0,
                capturedAt = 10000L + i,
                fan1On = f1,
                fan2On = f2,
                fan3On = f3,
                fan4On = f4,
                fan5On = f5,
                fan6On = f6,
                gateOpen = gate
            )

            vm.updateFromTelemetry(reading)
            val state = vm.uiState.value

            assertEquals(f1, state.fan1On)
            assertEquals(f2, state.fan2On)
            assertEquals(f3, state.fan3On)
            assertEquals(f4, state.fan4On)
            assertEquals(f5, state.fan5On)
            assertEquals(f6, state.fan6On)
            assertEquals(gate, state.isGateOpen)
            assertEquals(t1, state.temperature1)
            assertEquals(t2, state.temperature2)
            assertEquals(hum, state.humidity)
            assertEquals(volt, state.voltage)
        }
    }
}

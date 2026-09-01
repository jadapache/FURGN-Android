package com.furgnbi.android.domain.usecase

import com.furgnbi.android.data.modbus.ModbusRepository
import com.furgnbi.android.data.repository.ParameterRepository
import com.furgnbi.android.domain.model.ModbusConnectionState
import com.furgnbi.android.domain.model.ModbusDataType
import com.furgnbi.android.domain.model.PlcParameter
import com.furgnbi.android.domain.model.TelemetryReading
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WriteParameterUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeParameterRepository : ParameterRepository {
        val parameters = mutableMapOf<Int, PlcParameter>()

        override suspend fun getAllParameters(): List<PlcParameter> = parameters.values.toList()
        override fun observeParameters(): Flow<List<PlcParameter>> = flowOf(parameters.values.toList())
        override suspend fun getParameter(id: Int): PlcParameter? = parameters[id]
        override suspend fun updateParameter(parameter: PlcParameter) { parameters[parameter.id] = parameter }
        override suspend fun updateValue(id: Int, value: Double) {
            parameters[id]?.let { parameters[id] = it.copy(currentValue = value) }
        }
        override suspend fun insertAll(parameters: List<PlcParameter>) {
            parameters.forEach { this.parameters[it.id] = it }
        }
        override suspend fun count(): Int = parameters.size
    }

    class FakeModbusRepository : ModbusRepository {
        override val connectionState: StateFlow<ModbusConnectionState> = MutableStateFlow(ModbusConnectionState.CONNECTED)
        val writtenFloats = mutableMapOf<Int, Float>()
        val writtenUInts = mutableMapOf<Int, Int>()
        var simulatedEchoFloat: Float? = null
        var simulatedEchoUInt: Int? = null
        var shouldFailWrite = false

        override suspend fun connect(host: String, port: Int, unitId: Int) {}
        override suspend fun disconnect() {}
        override suspend fun readTelemetry(): TelemetryReading = TelemetryReading("1", 0f, 0f, 0f, 0f, 0, 0L)
        override suspend fun readCoilStates(): Map<String, Boolean> = emptyMap()

        override suspend fun readHoldingRegisterFloat(address: Int): Float = simulatedEchoFloat ?: (writtenFloats[address] ?: 0f)
        override suspend fun readHoldingRegisterUInt16(address: Int): Int = simulatedEchoUInt ?: (writtenUInts[address] ?: 0)

        override suspend fun writeHoldingRegisterFloat(address: Int, value: Float) {
            if (shouldFailWrite) throw RuntimeException("Modbus write error")
            writtenFloats[address] = value
        }

        override suspend fun writeHoldingRegisterUInt16(address: Int, value: Int) {
            if (shouldFailWrite) throw RuntimeException("Modbus write error")
            writtenUInts[address] = value
        }

        override suspend fun writeSingleCoil(address: Int, value: Boolean) {}
    }

    @Test
    fun testOutOfRangeRejectedWithoutModbusCall() = runTest(testDispatcher) {
        val paramRepo = FakeParameterRepository()
        val modbusRepo = FakeModbusRepository()
        val useCase = WriteParameterUseCase(modbusRepo, paramRepo, testDispatcher)

        val param = PlcParameter(
            id = 1,
            name = "Setpoint Temp",
            modbusAddress = 40001,
            modbusDataType = ModbusDataType.REAL,
            registerCount = 2,
            minValue = -30.0,
            maxValue = 30.0,
            unit = "°C",
            defaultValue = -18.0,
            currentValue = -18.0
        )
        paramRepo.insertAll(listOf(param))

        // Attempt write value 50.0 (out of [-30, 30])
        val result = useCase(param, 50.0)

        assertTrue(result is WriteParameterUseCase.WriteResult.ValidationError)
        assertEquals(0, modbusRepo.writtenFloats.size)
        assertEquals(-18.0, paramRepo.getParameter(1)?.currentValue)
    }

    @Test
    fun testReadOnlyParameterRejected() = runTest(testDispatcher) {
        val paramRepo = FakeParameterRepository()
        val modbusRepo = FakeModbusRepository()
        val useCase = WriteParameterUseCase(modbusRepo, paramRepo, testDispatcher)

        val param = PlcParameter(
            id = 23,
            name = "Firmware",
            modbusAddress = 40035,
            modbusDataType = ModbusDataType.UINT16,
            registerCount = 1,
            minValue = 0.0,
            maxValue = 9999.0,
            unit = "v",
            defaultValue = 100.0,
            isReadOnly = true
        )

        val result = useCase(param, 200.0)
        assertTrue(result is WriteParameterUseCase.WriteResult.ValidationError)
    }

    @Test
    fun testSuccessfulWriteUpdatesCacheWithEchoValue() = runTest(testDispatcher) {
        val paramRepo = FakeParameterRepository()
        val modbusRepo = FakeModbusRepository()
        val useCase = WriteParameterUseCase(modbusRepo, paramRepo, testDispatcher)

        val param = PlcParameter(
            id = 1,
            name = "Setpoint Temp",
            modbusAddress = 40001,
            modbusDataType = ModbusDataType.REAL,
            registerCount = 2,
            minValue = -30.0,
            maxValue = 30.0,
            unit = "°C",
            defaultValue = -18.0,
            currentValue = -18.0
        )
        paramRepo.insertAll(listOf(param))

        val result = useCase(param, -20.5)

        assertTrue(result is WriteParameterUseCase.WriteResult.Success)
        assertEquals(-20.5f, modbusRepo.writtenFloats[40001])
        assertEquals(-20.5, paramRepo.getParameter(1)?.currentValue ?: 0.0, 0.01)
    }
}

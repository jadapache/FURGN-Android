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
class ReadAllParametersUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeParamRepo : ParameterRepository {
        val params = mutableMapOf<Int, PlcParameter>()

        override suspend fun getAllParameters(): List<PlcParameter> = params.values.toList()
        override fun observeParameters(): Flow<List<PlcParameter>> = flowOf(params.values.toList())
        override suspend fun getParameter(id: Int): PlcParameter? = params[id]
        override suspend fun updateParameter(parameter: PlcParameter) { params[parameter.id] = parameter }
        override suspend fun updateValue(id: Int, value: Double) { params[id]?.let { params[id] = it.copy(currentValue = value) } }
        override suspend fun insertAll(parameters: List<PlcParameter>) { parameters.forEach { params[it.id] = it } }
        override suspend fun count(): Int = params.size
    }

    class FakeModbusRepo : ModbusRepository {
        override val connectionState: StateFlow<ModbusConnectionState> = MutableStateFlow(ModbusConnectionState.CONNECTED)
        override suspend fun connect(host: String, port: Int, unitId: Int) {}
        override suspend fun disconnect() {}
        override suspend fun readTelemetry(): TelemetryReading = TelemetryReading("1", 0f, 0f, 0f, 0f, 0, 0L)
        override suspend fun readCoilStates(): Map<String, Boolean> = emptyMap()

        override suspend fun readHoldingRegisterFloat(address: Int): Float = when (address) {
            40001 -> -18.2f
            40003 -> 3.9f
            else -> 0.0f
        }

        override suspend fun readHoldingRegisterUInt16(address: Int): Int = when (address) {
            40009 -> 35
            40035 -> 102
            else -> 0
        }

        override suspend fun writeHoldingRegisterFloat(address: Int, value: Float) {}
        override suspend fun writeHoldingRegisterUInt16(address: Int, value: Int) {}
        override suspend fun writeSingleCoil(address: Int, value: Boolean) {}
    }

    @Test
    fun testReadAllUpdatesAllParameters() = runTest(testDispatcher) {
        val repo = FakeParamRepo()
        val modbus = FakeModbusRepo()
        val useCase = ReadAllParametersUseCase(modbus, repo, testDispatcher)

        repo.insertAll(listOf(
            PlcParameter(1, "SP Temp 1", 40001, ModbusDataType.REAL, 2, -30.0, 30.0, "°C", -18.0),
            PlcParameter(2, "SP Temp 2", 40003, ModbusDataType.REAL, 2, -30.0, 30.0, "°C", 4.0),
            PlcParameter(5, "Tiempo Defrost", 40009, ModbusDataType.UINT16, 1, 5.0, 120.0, "min", 30.0),
            PlcParameter(23, "Firmware", 40035, ModbusDataType.UINT16, 1, 0.0, 9999.0, "v", 100.0, isReadOnly = true)
        ))

        val result = useCase()
        assertTrue(result.isSuccess)

        val updated = repo.getAllParameters().associateBy { it.id }
        assertEquals(-18.2, updated[1]?.currentValue ?: 0.0, 0.01)
        assertEquals(3.9, updated[2]?.currentValue ?: 0.0, 0.01)
        assertEquals(35.0, updated[5]?.currentValue ?: 0.0, 0.01)
        assertEquals(102.0, updated[23]?.currentValue ?: 0.0, 0.01)
    }
}

package com.furgnbi.android.data.modbus

import com.furgnbi.android.domain.model.ModbusConnectionState
import com.furgnbi.android.domain.model.TelemetryReading
import kotlinx.coroutines.flow.StateFlow

interface ModbusRepository {
    val connectionState: StateFlow<ModbusConnectionState>
    suspend fun connect(host: String, port: Int, unitId: Int)
    suspend fun disconnect()
    suspend fun readTelemetry(): TelemetryReading
    suspend fun readCoilStates(): Map<String, Boolean>
    suspend fun readHoldingRegisterFloat(address: Int): Float
    suspend fun readHoldingRegisterUInt16(address: Int): Int
    suspend fun writeHoldingRegisterFloat(address: Int, value: Float)
    suspend fun writeHoldingRegisterUInt16(address: Int, value: Int)
    suspend fun writeSingleCoil(address: Int, value: Boolean)
}

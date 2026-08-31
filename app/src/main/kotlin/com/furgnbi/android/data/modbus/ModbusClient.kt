package com.furgnbi.android.data.modbus

import com.furgnbi.android.domain.model.ModbusConnectionState
import kotlinx.coroutines.flow.StateFlow

interface ModbusClient {
    val connectionState: StateFlow<ModbusConnectionState>
    suspend fun connect(host: String, port: Int = 502, unitId: Int = 1)
    suspend fun disconnect()
    suspend fun readInputRegisters(startAddress: Int, count: Int): List<Int>
    suspend fun readCoils(startAddress: Int, count: Int): List<Boolean>
    suspend fun readHoldingRegisters(startAddress: Int, count: Int): List<Int>
    suspend fun writeSingleRegister(address: Int, value: Int)
    suspend fun writeMultipleRegisters(address: Int, values: List<Int>)
    suspend fun writeSingleCoil(address: Int, value: Boolean)
}

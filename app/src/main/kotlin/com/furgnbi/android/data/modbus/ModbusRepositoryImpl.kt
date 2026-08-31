package com.furgnbi.android.data.modbus

import com.furgnbi.android.domain.model.ModbusConnectionState
import com.furgnbi.android.domain.model.TelemetryReading
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modbus repository implementation integrating IEEE754Decoder for REAL registers.
 *
 * Register map from spec:
 *   Input Registers (FC04):
 *     30001-30002: T1 (REAL)
 *     30003-30004: T2 (REAL)
 *     30005-30006: Humidity (REAL)
 *     30007-30008: Voltage (REAL)
 *   Coils (FC01):
 *     00001-00008: Fan1..Fan6, GateOpen, AlarmActive
 */
@Singleton
class ModbusRepositoryImpl @Inject constructor(
    private val client: ModbusTcpClient
) : ModbusRepository {

    override val connectionState: StateFlow<ModbusConnectionState> = client.connectionState

    override suspend fun connect(host: String, port: Int, unitId: Int) =
        client.connect(host, port, unitId)

    override suspend fun disconnect() = client.disconnect()

    override suspend fun readTelemetry(): TelemetryReading {
        // Read 8 input registers starting at 0 (Modbus address 30001 = offset 0 in FC04)
        val registers = client.readInputRegisters(startAddress = 0, count = 8)
        val coils = client.readCoils(startAddress = 0, count = 8)

        val t1 = IEEE754Decoder.decodeIEEE754BigEndian(registers[0], registers[1])
        val t2 = IEEE754Decoder.decodeIEEE754BigEndian(registers[2], registers[3])
        val hum = IEEE754Decoder.decodeIEEE754BigEndian(registers[4], registers[5])
        val volt = IEEE754Decoder.decodeIEEE754BigEndian(registers[6], registers[7])

        return TelemetryReading(
            deviceId = "",  // injected by use case from AppConfig
            temperatureC = t1,
            temperature2C = t2,
            humidityPct = hum,
            voltageV = volt,
            modbusRegister = 30001,
            capturedAt = System.currentTimeMillis(),
            fan1On  = coils.getOrElse(0) { false },
            fan2On  = coils.getOrElse(1) { false },
            fan3On  = coils.getOrElse(2) { false },
            fan4On  = coils.getOrElse(3) { false },
            fan5On  = coils.getOrElse(4) { false },
            fan6On  = coils.getOrElse(5) { false },
            gateOpen     = coils.getOrElse(6) { false },
            alarmActive  = coils.getOrElse(7) { false }
        )
    }

    override suspend fun readCoilStates(): Map<String, Boolean> {
        val coils = client.readCoils(startAddress = 0, count = 8)
        return mapOf(
            "fan1" to coils.getOrElse(0) { false },
            "fan2" to coils.getOrElse(1) { false },
            "fan3" to coils.getOrElse(2) { false },
            "fan4" to coils.getOrElse(3) { false },
            "fan5" to coils.getOrElse(4) { false },
            "fan6" to coils.getOrElse(5) { false },
            "gate" to coils.getOrElse(6) { false },
            "alarm" to coils.getOrElse(7) { false }
        )
    }

    override suspend fun readHoldingRegisterFloat(address: Int): Float {
        // address is 1-based Modbus address; offset = address - 40001
        val offset = address - 40001
        val regs = client.readHoldingRegisters(startAddress = offset, count = 2)
        return IEEE754Decoder.decodeIEEE754BigEndian(regs[0], regs[1])
    }

    override suspend fun readHoldingRegisterUInt16(address: Int): Int {
        val offset = address - 40001
        val regs = client.readHoldingRegisters(startAddress = offset, count = 1)
        return regs[0]
    }

    override suspend fun writeHoldingRegisterFloat(address: Int, value: Float) {
        val offset = address - 40001
        val words = IEEE754Decoder.encodeAsList(value)
        client.writeMultipleRegisters(startAddress = offset, values = words)
    }

    override suspend fun writeHoldingRegisterUInt16(address: Int, value: Int) {
        val offset = address - 40001
        client.writeSingleRegister(address = offset, value = value)
    }

    override suspend fun writeSingleCoil(address: Int, value: Boolean) =
        client.writeSingleCoil(address - 1, value) // coil addresses are 1-based
}

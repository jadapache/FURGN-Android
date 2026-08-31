package com.furgnbi.android.data.modbus

import android.util.Log
import com.furgnbi.android.domain.model.ModbusConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modbus TCP client implementation using raw Java sockets.
 * Implements Modbus Application Protocol (MBAP) framing over TCP/IP.
 *
 * PLC MICRO 820 acts as Modbus TCP server on port 502.
 * Function codes supported:
 *   FC01 — Read Coils
 *   FC03 — Read Holding Registers
 *   FC04 — Read Input Registers
 *   FC05 — Write Single Coil
 *   FC06 — Write Single Register
 *   FC16 — Write Multiple Registers
 */
@Singleton
class ModbusTcpClient @Inject constructor() : ModbusClient {

    companion object {
        private const val TAG = "ModbusTcpClient"
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 5_000
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1_000L
    }

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var currentUnitId: Int = 1
    private var transactionId: Int = 0

    private val _connectionState = MutableStateFlow(ModbusConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ModbusConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(host: String, port: Int, unitId: Int) {
        withContext(Dispatchers.IO) {
            try {
                _connectionState.value = ModbusConnectionState.CONNECTING
                disconnect()
                currentUnitId = unitId
                val s = Socket()
                s.connect(java.net.InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                s.soTimeout = READ_TIMEOUT_MS
                socket = s
                inputStream = s.getInputStream()
                outputStream = s.getOutputStream()
                _connectionState.value = ModbusConnectionState.CONNECTED
                Log.i(TAG, "Connected to Modbus TCP server at $host:$port")
            } catch (e: Exception) {
                _connectionState.value = ModbusConnectionState.ERROR
                Log.e(TAG, "Failed to connect: ${e.message}")
                throw e
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching {
                inputStream?.close()
                outputStream?.close()
                socket?.close()
            }
            socket = null
            inputStream = null
            outputStream = null
            _connectionState.value = ModbusConnectionState.DISCONNECTED
        }
    }

    // ── FC04: Read Input Registers ──────────────────────────────────────────
    override suspend fun readInputRegisters(startAddress: Int, count: Int): List<Int> =
        retryWithBackoff { sendRequest(buildReadRequest(0x04, startAddress, count), count) }

    // ── FC01: Read Coils ────────────────────────────────────────────────────
    override suspend fun readCoils(startAddress: Int, count: Int): List<Boolean> =
        retryWithBackoff {
            val raw = sendRequest(buildReadRequest(0x01, startAddress, count), count, isCoil = true)
            List(count) { i -> (raw[i / 16] shr (i % 16)) and 1 == 1 }
        }

    // ── FC03: Read Holding Registers ────────────────────────────────────────
    override suspend fun readHoldingRegisters(startAddress: Int, count: Int): List<Int> =
        retryWithBackoff { sendRequest(buildReadRequest(0x03, startAddress, count), count) }

    // ── FC06: Write Single Register ─────────────────────────────────────────
    override suspend fun writeSingleRegister(address: Int, value: Int) {
        retryWithBackoff<Unit> {
            val request = buildWriteSingleRegisterRequest(address, value)
            sendRawRequest(request)
        }
    }

    // ── FC16: Write Multiple Registers ──────────────────────────────────────
    override suspend fun writeMultipleRegisters(address: Int, values: List<Int>) {
        retryWithBackoff<Unit> {
            val request = buildWriteMultipleRegistersRequest(address, values)
            sendRawRequest(request)
        }
    }

    // ── FC05: Write Single Coil ─────────────────────────────────────────────
    override suspend fun writeSingleCoil(address: Int, value: Boolean) {
        retryWithBackoff<Unit> {
            val coilValue = if (value) 0xFF00 else 0x0000
            val request = buildWriteSingleCoilRequest(address, coilValue)
            sendRawRequest(request)
        }
    }

    // ── MBAP frame builders ─────────────────────────────────────────────────

    private fun nextTransactionId(): Int {
        transactionId = (transactionId + 1) and 0xFFFF
        return transactionId
    }

    private fun buildReadRequest(functionCode: Int, startAddress: Int, count: Int): ByteArray {
        val tid = nextTransactionId()
        return byteArrayOf(
            (tid shr 8).toByte(), (tid and 0xFF).toByte(), // Transaction ID
            0x00, 0x00,                                    // Protocol ID = 0
            0x00, 0x06,                                    // Length = 6
            currentUnitId.toByte(),                        // Unit ID
            functionCode.toByte(),                         // Function code
            (startAddress shr 8).toByte(), (startAddress and 0xFF).toByte(),
            (count shr 8).toByte(), (count and 0xFF).toByte()
        )
    }

    private fun buildWriteSingleRegisterRequest(address: Int, value: Int): ByteArray {
        val tid = nextTransactionId()
        return byteArrayOf(
            (tid shr 8).toByte(), (tid and 0xFF).toByte(),
            0x00, 0x00, 0x00, 0x06,
            currentUnitId.toByte(), 0x06,
            (address shr 8).toByte(), (address and 0xFF).toByte(),
            (value shr 8).toByte(), (value and 0xFF).toByte()
        )
    }

    private fun buildWriteSingleCoilRequest(address: Int, coilValue: Int): ByteArray {
        val tid = nextTransactionId()
        return byteArrayOf(
            (tid shr 8).toByte(), (tid and 0xFF).toByte(),
            0x00, 0x00, 0x00, 0x06,
            currentUnitId.toByte(), 0x05,
            (address shr 8).toByte(), (address and 0xFF).toByte(),
            (coilValue shr 8).toByte(), (coilValue and 0xFF).toByte()
        )
    }

    private fun buildWriteMultipleRegistersRequest(address: Int, values: List<Int>): ByteArray {
        val count = values.size
        val byteCount = count * 2
        val pduLength = 7 + byteCount
        val tid = nextTransactionId()
        val frame = ByteArray(6 + pduLength)
        frame[0] = (tid shr 8).toByte(); frame[1] = (tid and 0xFF).toByte()
        frame[2] = 0x00; frame[3] = 0x00
        frame[4] = (pduLength shr 8).toByte(); frame[5] = (pduLength and 0xFF).toByte()
        frame[6] = currentUnitId.toByte()
        frame[7] = 0x10
        frame[8] = (address shr 8).toByte(); frame[9] = (address and 0xFF).toByte()
        frame[10] = (count shr 8).toByte(); frame[11] = (count and 0xFF).toByte()
        frame[12] = byteCount.toByte()
        for (i in values.indices) {
            frame[13 + i * 2] = (values[i] shr 8).toByte()
            frame[14 + i * 2] = (values[i] and 0xFF).toByte()
        }
        return frame
    }

    // ── I/O helpers ─────────────────────────────────────────────────────────

    private suspend fun sendRequest(request: ByteArray, count: Int, isCoil: Boolean = false): List<Int> =
        withContext(Dispatchers.IO) {
            val out = outputStream ?: throw IllegalStateException("Not connected")
            val inp = inputStream ?: throw IllegalStateException("Not connected")
            out.write(request)
            out.flush()
            val header = ByteArray(6)
            inp.readFully(header)
            val pduLength = ((header[4].toInt() and 0xFF) shl 8) or (header[5].toInt() and 0xFF)
            val pdu = ByteArray(pduLength)
            inp.readFully(pdu)
            // pdu[0] = unit id, pdu[1] = function code, pdu[2] = byte count
            val values = mutableListOf<Int>()
            if (isCoil) {
                val byteCount = pdu[2].toInt() and 0xFF
                for (i in 0 until byteCount) {
                    values.add(pdu[3 + i].toInt() and 0xFF)
                }
            } else {
                val byteCount = pdu[2].toInt() and 0xFF
                for (i in 0 until byteCount / 2) {
                    val high = pdu[3 + i * 2].toInt() and 0xFF
                    val low = pdu[4 + i * 2].toInt() and 0xFF
                    values.add((high shl 8) or low)
                }
            }
            values
        }

    private suspend fun sendRawRequest(request: ByteArray) = withContext(Dispatchers.IO) {
        val out = outputStream ?: throw IllegalStateException("Not connected")
        val inp = inputStream ?: throw IllegalStateException("Not connected")
        out.write(request)
        out.flush()
        // Read response header + PDU (echo for FC05/FC06/FC16)
        val header = ByteArray(6)
        inp.readFully(header)
        val pduLength = ((header[4].toInt() and 0xFF) shl 8) or (header[5].toInt() and 0xFF)
        val pdu = ByteArray(pduLength)
        inp.readFully(pdu)
    }

    // ── Retry with exponential backoff ──────────────────────────────────────
    // Starts at 1s, doubles each attempt: 1s, 2s, 4s (Requisito 1.4, 1.5, 1.6)

    private suspend fun <T> retryWithBackoff(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: SocketTimeoutException) {
                attempt++
                if (attempt >= MAX_RETRIES) {
                    _connectionState.value = ModbusConnectionState.ERROR
                    Log.e(TAG, "Modbus timeout after $MAX_RETRIES attempts")
                    throw e
                }
                val delayMs = Math.pow(2.0, (attempt - 1).toDouble()).toLong() * INITIAL_RETRY_DELAY_MS
                Log.w(TAG, "Modbus timeout, retry $attempt in ${delayMs}ms")
                delay(delayMs)
            } catch (e: Exception) {
                attempt++
                if (attempt >= MAX_RETRIES) {
                    _connectionState.value = ModbusConnectionState.ERROR
                    throw e
                }
                val delayMs = Math.pow(2.0, (attempt - 1).toDouble()).toLong() * INITIAL_RETRY_DELAY_MS
                Log.w(TAG, "Modbus error: ${e.message}, retry $attempt in ${delayMs}ms")
                delay(delayMs)
            }
        }
    }

    private fun InputStream.readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read == -1) throw java.io.EOFException("Connection closed by server")
            offset += read
        }
    }
}

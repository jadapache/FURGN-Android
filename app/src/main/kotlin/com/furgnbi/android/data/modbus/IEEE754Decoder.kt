package com.furgnbi.android.data.modbus

/**
 * Encodes and decodes IEEE 754 single-precision floats as pairs of 16-bit Modbus registers.
 *
 * PLC MICRO 820 stores REAL values in Big-Endian word order:
 *   Register N   = high word (bits 31–16)
 *   Register N+1 = low word  (bits 15–0)
 *
 * Correctness property: decodeIEEE754BigEndian(encodeIEEE754BigEndian(x)) == x
 * for all finite Float values (Requisito 10.3).
 */
object IEEE754Decoder {

    /**
     * Decodes two consecutive Modbus holding registers into a Float.
     *
     * @param highWord Register N   (bits 31–16), raw unsigned 16-bit value
     * @param lowWord  Register N+1 (bits 15–0),  raw unsigned 16-bit value
     * @return Decoded Float value
     */
    fun decodeIEEE754BigEndian(highWord: Int, lowWord: Int): Float {
        val bits: Int = ((highWord and 0xFFFF) shl 16) or (lowWord and 0xFFFF)
        return java.lang.Float.intBitsToFloat(bits)
    }

    /**
     * Encodes a Float into two consecutive Modbus holding register values.
     *
     * @param value The Float to encode
     * @return Pair of (highWord, lowWord) — register N and register N+1
     */
    fun encodeIEEE754BigEndian(value: Float): Pair<Int, Int> {
        val bits: Int = java.lang.Float.floatToIntBits(value)
        val highWord: Int = (bits ushr 16) and 0xFFFF
        val lowWord: Int  = bits and 0xFFFF
        return Pair(highWord, lowWord)
    }

    /**
     * Convenience: encode and return as List<Int> for Modbus write operations.
     */
    fun encodeAsList(value: Float): List<Int> {
        val (high, low) = encodeIEEE754BigEndian(value)
        return listOf(high, low)
    }
}

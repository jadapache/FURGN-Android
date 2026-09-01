package com.furgnbi.android.data.modbus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IEEE754DecoderTest {

    @Test
    fun testKnownValuesRoundTrip() {
        val testValues = listOf(
            0.0f,
            -0.0f,
            1.0f,
            -1.0f,
            -18.5f,
            4.2f,
            85.0f,
            24.25f,
            Float.MIN_VALUE,
            Float.MAX_VALUE,
            -Float.MAX_VALUE,
            100.5f,
            0.0001f,
            -999.999f
        )

        for (v in testValues) {
            val (high, low) = IEEE754Decoder.encodeIEEE754BigEndian(v)
            val decoded = IEEE754Decoder.decodeIEEE754BigEndian(high, low)
            assertEquals("Mismatch for float $v", v, decoded, 0.00001f)
        }
    }

    @Test
    fun testRandomPropertyRoundTrip1000Iterations() {
        val random = Random(42)
        for (i in 0 until 2000) {
            val original = (random.nextFloat() - 0.5f) * 2000.0f
            val (high, low) = IEEE754Decoder.encodeIEEE754BigEndian(original)
            val decoded = IEEE754Decoder.decodeIEEE754BigEndian(high, low)
            assertEquals("Property round-trip failed at iteration $i", original, decoded, 0.00001f)
        }
    }

    @Test
    fun testEncodeAsList() {
        val words = IEEE754Decoder.encodeAsList(-18.0f)
        assertEquals(2, words.size)
        val decoded = IEEE754Decoder.decodeIEEE754BigEndian(words[0], words[1])
        assertEquals(-18.0f, decoded, 0.0001f)
    }
}

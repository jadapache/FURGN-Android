package com.furgnbi.android.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MqttBackoffTest {

    @Test
    fun testBackoffProgression() {
        // attempt 0 -> 1000ms
        assertEquals(1000L, MqttRepositoryImpl.calculateBackoffDelayMs(0))
        // attempt 1 -> 2000ms
        assertEquals(2000L, MqttRepositoryImpl.calculateBackoffDelayMs(1))
        // attempt 2 -> 4000ms
        assertEquals(4000L, MqttRepositoryImpl.calculateBackoffDelayMs(2))
        // attempt 3 -> 8000ms
        assertEquals(8000L, MqttRepositoryImpl.calculateBackoffDelayMs(3))
        // attempt 4 -> 16000ms
        assertEquals(16000L, MqttRepositoryImpl.calculateBackoffDelayMs(4))
        // attempt 5 -> 32000ms
        assertEquals(32000L, MqttRepositoryImpl.calculateBackoffDelayMs(5))
        // attempt 6 -> 60000ms (capped at 60s)
        assertEquals(60000L, MqttRepositoryImpl.calculateBackoffDelayMs(6))
        // attempt 10 -> 60000ms
        assertEquals(60000L, MqttRepositoryImpl.calculateBackoffDelayMs(10))
    }

    @Test
    fun testBackoffNeverExceedsMax() {
        for (attempt in 0..50) {
            val delay = MqttRepositoryImpl.calculateBackoffDelayMs(attempt)
            assertTrue("Delay $delay ms exceeds 60000ms max at attempt $attempt", delay <= 60000L)
            assertTrue("Delay $delay ms is less than 1000ms min at attempt $attempt", delay >= 1000L)
        }
    }
}

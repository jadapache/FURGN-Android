package com.furgnbi.android.data.repository

import com.furgnbi.android.domain.model.MqttConnectionState
import kotlinx.coroutines.flow.StateFlow

interface MqttRepository {
    val connectionState: StateFlow<MqttConnectionState>
    suspend fun connect()
    suspend fun disconnect()
    /**
     * Publishes a message and suspends until PUBACK is received (QoS 1).
     * Throws an exception if the broker does not acknowledge within the timeout.
     */
    suspend fun publish(topic: String, payload: String, qos: Int = 1)
}

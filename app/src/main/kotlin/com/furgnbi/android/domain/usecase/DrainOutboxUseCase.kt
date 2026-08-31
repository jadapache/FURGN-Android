package com.furgnbi.android.domain.usecase

import android.util.Log
import com.furgnbi.android.data.repository.MqttRepository
import com.furgnbi.android.data.repository.OutboxRepository
import com.furgnbi.android.di.IoDispatcher
import com.furgnbi.android.domain.model.MqttConnectionState
import com.furgnbi.android.domain.model.OutboxStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drains the outbox queue by publishing pending messages to the MQTT broker.
 *
 * For each pending message:
 *   - Attempts publish with QoS 1
 *   - Marks as SENT only upon receiving PUBACK (Correctness Property 3, Requisito 5.2)
 *   - On failure: increments retry_count; marks as FAILED after MAX_RETRIES
 *
 * Messages are preserved offline indefinitely (Property 4, Requisito 4.5).
 */
@Singleton
class DrainOutboxUseCase @Inject constructor(
    private val outboxRepository: OutboxRepository,
    private val mqttRepository: MqttRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val BATCH_SIZE = 20
        private const val MAX_RETRIES = 5
        private const val TAG = "DrainOutboxUseCase"
    }

    suspend operator fun invoke(): DrainResult = withContext(ioDispatcher) {
        if (mqttRepository.connectionState.value != MqttConnectionState.CONNECTED) {
            Log.w(TAG, "MQTT not connected, skipping drain")
            return@withContext DrainResult(sent = 0, failed = 0, skipped = true)
        }

        val pending = outboxRepository.fetchPending(limit = BATCH_SIZE)
        var sent = 0
        var failed = 0

        for (message in pending) {
            runCatching {
                // publish() is a suspend function that waits for PUBACK before returning
                mqttRepository.publish(
                    topic = message.topic,
                    payload = message.payloadJson,
                    qos = message.qos
                )
                // Only mark SENT if publish returned without exception (PUBACK received)
                outboxRepository.markAsSent(message.id)
                sent++
                Log.d(TAG, "SENT id=${message.id} topic=${message.topic}")
            }.onFailure { e ->
                Log.w(TAG, "Failed to publish id=${message.id}: ${e.message}")
                val newRetryCount = message.retryCount + 1
                if (newRetryCount >= MAX_RETRIES) {
                    outboxRepository.updateStatus(message.id, OutboxStatus.FAILED)
                    Log.e(TAG, "Message id=${message.id} permanently failed after $MAX_RETRIES attempts")
                } else {
                    outboxRepository.incrementRetryCount(message.id)
                }
                failed++
            }
        }

        Log.i(TAG, "Drain complete: sent=$sent failed=$failed")
        DrainResult(sent = sent, failed = failed, skipped = false)
    }

    data class DrainResult(
        val sent: Int,
        val failed: Int,
        val skipped: Boolean
    )
}

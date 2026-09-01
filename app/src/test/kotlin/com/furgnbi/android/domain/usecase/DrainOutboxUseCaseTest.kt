package com.furgnbi.android.domain.usecase

import com.furgnbi.android.data.repository.MqttRepository
import com.furgnbi.android.data.repository.OutboxRepository
import com.furgnbi.android.domain.model.MqttConnectionState
import com.furgnbi.android.domain.model.OutboxMessage
import com.furgnbi.android.domain.model.OutboxStatus
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
class DrainOutboxUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeOutboxRepository : OutboxRepository {
        val messages = mutableMapOf<Long, OutboxMessage>()
        var nextId = 1L

        override suspend fun enqueue(message: OutboxMessage): Long {
            val id = nextId++
            messages[id] = message.copy(id = id)
            return id
        }

        override suspend fun fetchPending(limit: Int): List<OutboxMessage> {
            return messages.values.filter { it.status == OutboxStatus.PENDING }.take(limit)
        }

        override suspend fun markAsSent(id: Long) {
            messages[id]?.let {
                messages[id] = it.copy(status = OutboxStatus.SENT)
            }
        }

        override suspend fun incrementRetryCount(id: Long) {
            messages[id]?.let {
                messages[id] = it.copy(retryCount = it.retryCount + 1)
            }
        }

        override suspend fun updateStatus(id: Long, status: OutboxStatus) {
            messages[id]?.let {
                messages[id] = it.copy(status = status)
            }
        }

        override suspend fun countPending(): Int = messages.values.count { it.status == OutboxStatus.PENDING }

        override fun observePendingCount(): Flow<Int> = flowOf(messages.values.count { it.status == OutboxStatus.PENDING })

        override suspend fun pruneSent() {
            messages.entries.removeIf { it.value.status == OutboxStatus.SENT }
        }
    }

    class FakeMqttRepository : MqttRepository {
        val _connectionState = MutableStateFlow(MqttConnectionState.CONNECTED)
        override val connectionState: StateFlow<MqttConnectionState> = _connectionState
        val published = mutableListOf<Triple<String, String, Int>>()
        var shouldFailPublish = false

        override suspend fun connect() {
            _connectionState.value = MqttConnectionState.CONNECTED
        }

        override suspend fun disconnect() {
            _connectionState.value = MqttConnectionState.DISCONNECTED
        }

        override suspend fun publish(topic: String, payload: String, qos: Int) {
            if (shouldFailPublish) {
                throw RuntimeException("Simulated broker network error")
            }
            published.add(Triple(topic, payload, qos))
        }
    }

    @Test
    fun testDrainPublishesAndMarksSentOnPubAck() = runTest(testDispatcher) {
        val outboxRepo = FakeOutboxRepository()
        val mqttRepo = FakeMqttRepository()
        val useCase = DrainOutboxUseCase(outboxRepo, mqttRepo, testDispatcher)

        outboxRepo.enqueue(OutboxMessage(id = 0, topic = "furgones/1/telemetry", payloadJson = "{}", qos = 1, retryCount = 0, status = OutboxStatus.PENDING, createdAt = 1000L))
        outboxRepo.enqueue(OutboxMessage(id = 0, topic = "furgones/1/alarms", payloadJson = "{}", qos = 1, retryCount = 0, status = OutboxStatus.PENDING, createdAt = 1001L))

        assertEquals(2, outboxRepo.countPending())

        val result = useCase()

        assertEquals(2, result.sent)
        assertEquals(0, result.failed)
        assertEquals(0, outboxRepo.countPending())
        assertEquals(2, mqttRepo.published.size)
        assertEquals(OutboxStatus.SENT, outboxRepo.messages[1L]?.status)
        assertEquals(OutboxStatus.SENT, outboxRepo.messages[2L]?.status)
    }

    @Test
    fun testDrainIncrementsRetryOnFailureAndMarksFailedAfterMaxRetries() = runTest(testDispatcher) {
        val outboxRepo = FakeOutboxRepository()
        val mqttRepo = FakeMqttRepository().apply { shouldFailPublish = true }
        val useCase = DrainOutboxUseCase(outboxRepo, mqttRepo, testDispatcher)

        outboxRepo.enqueue(OutboxMessage(id = 0, topic = "furgones/1/telemetry", payloadJson = "{}", qos = 1, retryCount = 4, status = OutboxStatus.PENDING, createdAt = 1000L))

        val result = useCase()

        assertEquals(0, result.sent)
        assertEquals(1, result.failed)
        // Reached 5 retries -> should be marked FAILED
        assertEquals(OutboxStatus.FAILED, outboxRepo.messages[1L]?.status)
    }

    @Test
    fun testDrainSkippedWhenMqttDisconnected() = runTest(testDispatcher) {
        val outboxRepo = FakeOutboxRepository()
        val mqttRepo = FakeMqttRepository().apply { _connectionState.value = MqttConnectionState.DISCONNECTED }
        val useCase = DrainOutboxUseCase(outboxRepo, mqttRepo, testDispatcher)

        outboxRepo.enqueue(OutboxMessage(id = 0, topic = "furgones/1/telemetry", payloadJson = "{}", qos = 1, retryCount = 0, status = OutboxStatus.PENDING, createdAt = 1000L))

        val result = useCase()

        assertTrue(result.skipped)
        assertEquals(0, result.sent)
        assertEquals(1, outboxRepo.countPending())
    }
}

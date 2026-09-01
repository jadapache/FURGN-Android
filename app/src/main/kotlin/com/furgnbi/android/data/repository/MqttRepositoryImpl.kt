package com.furgnbi.android.data.repository

import android.content.Context
import android.util.Log
import com.furgnbi.android.di.IoDispatcher
import com.furgnbi.android.domain.model.MqttConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlin.math.pow

@Singleton
class MqttRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appConfigRepository: AppConfigRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MqttRepository {

    companion object {
        private const val TAG = "MqttRepository"
        private const val MAX_BACKOFF_MS = 60_000L
        private const val BASE_BACKOFF_MS = 1_000L
        private const val PUBLISH_TIMEOUT_MS = 10_000L

        /**
         * Calculates exponential backoff bounded by [MAX_BACKOFF_MS] (Property 5, Requisito 6.2).
         * delay = min(2^k * 1000ms, 60000ms)
         */
        fun calculateBackoffDelayMs(attempt: Int): Long {
            if (attempt <= 0) return BASE_BACKOFF_MS
            val factor = if (attempt >= 30) Long.MAX_VALUE else (1L shl attempt)
            val computed = BASE_BACKOFF_MS * factor
            return if (computed <= 0 || computed > MAX_BACKOFF_MS) MAX_BACKOFF_MS else computed
        }
    }

    private val _connectionState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private var mqttClient: MqttAsyncClient? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var reconnectJob: Job? = null
    private var isManuallyDisconnected = false

    override suspend fun connect(): Unit = withContext(ioDispatcher) {
        if (_connectionState.value == MqttConnectionState.CONNECTED ||
            _connectionState.value == MqttConnectionState.CONNECTING) {
            return@withContext
        }

        isManuallyDisconnected = false
        performConnectWithRetry()
    }

    private suspend fun performConnectWithRetry() {
        var attempt = 0
        while (!isManuallyDisconnected && _connectionState.value != MqttConnectionState.CONNECTED) {
            try {
                _connectionState.value = MqttConnectionState.CONNECTING
                val config = appConfigRepository.getConfig()
                val brokerUrl = config.brokerUrl
                val clientId = config.mqttClientId.ifBlank { "furgn_${System.currentTimeMillis()}" }

                val client = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
                mqttClient = client

                val options = MqttConnectOptions().apply {
                    isCleanSession = false
                    keepAliveInterval = 60
                    connectionTimeout = 30
                    isAutomaticReconnect = false
                    if (config.mqttUsername.isNotBlank()) {
                        userName = config.mqttUsername
                        password = config.mqttPassword.toCharArray()
                    }
                }

                client.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.w(TAG, "MQTT Connection lost: ${cause?.message}")
                        _connectionState.value = MqttConnectionState.DISCONNECTED
                        if (!isManuallyDisconnected) {
                            triggerReconnect()
                        }
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        // Handled if subscribed
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // Delivery acknowledged
                    }
                })

                suspendCancellableCoroutine<Unit> { continuation ->
                    client.connect(options, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.i(TAG, "Connected to MQTT broker: $brokerUrl")
                            _connectionState.value = MqttConnectionState.CONNECTED
                            if (continuation.isActive) continuation.resume(Unit)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to connect to MQTT broker: ${exception?.message}")
                            _connectionState.value = MqttConnectionState.ERROR
                            if (continuation.isActive) continuation.resumeWithException(
                                exception ?: MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt())
                            )
                        }
                    })
                }

                // If connected successfully, break out of loop
                return
            } catch (e: Exception) {
                Log.w(TAG, "Connect attempt $attempt failed: ${e.message}")
                _connectionState.value = MqttConnectionState.ERROR
                attempt++
                val delayMs = calculateBackoffDelayMs(attempt)
                Log.i(TAG, "Waiting ${delayMs}ms before reconnect attempt $attempt")
                delay(delayMs)
            }
        }
    }

    private fun triggerReconnect() {
        _connectionState.value = MqttConnectionState.RECONNECTING
        reconnectJob?.cancel()
        reconnectJob = repositoryScope.launch {
            delay(1000L)
            performConnectWithRetry()
        }
    }

    override suspend fun disconnect(): Unit = withContext(ioDispatcher) {
        isManuallyDisconnected = true
        reconnectJob?.cancel()
        runCatching {
            mqttClient?.let { client ->
                if (client.isConnected) {
                    client.disconnect()
                }
                client.close()
            }
        }
        _connectionState.value = MqttConnectionState.DISCONNECTED
        mqttClient = null
    }

    override suspend fun publish(topic: String, payload: String, qos: Int): Unit = withContext(ioDispatcher) {
        val client = mqttClient
        if (client == null || !client.isConnected) {
            throw MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED.toInt())
        }

        val message = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
            this.qos = qos
            this.isRetained = false
        }

        withTimeout(PUBLISH_TIMEOUT_MS) {
            suspendCancellableCoroutine<Unit> { continuation ->
                try {
                    client.publish(topic, message, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "PUBACK received for topic: $topic")
                            if (continuation.isActive) continuation.resume(Unit)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Publish failed for topic: $topic", exception)
                            if (continuation.isActive) continuation.resumeWithException(
                                exception ?: MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt())
                            )
                        }
                    })
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        }
    }
}

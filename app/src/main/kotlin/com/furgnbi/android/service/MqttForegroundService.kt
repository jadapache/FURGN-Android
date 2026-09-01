package com.furgnbi.android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.furgnbi.android.R
import com.furgnbi.android.data.repository.AppConfigRepository
import com.furgnbi.android.data.repository.MqttRepository
import com.furgnbi.android.domain.model.MqttConnectionState
import com.furgnbi.android.domain.usecase.GetGpsLocationUseCase
import com.furgnbi.android.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MqttForegroundService : Service() {

    companion object {
        private const val TAG = "MqttForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "furgn_telemetry_channel"
        private const val CHANNEL_NAME = "Monitoreo Furgón BI"

        const val ACTION_START = "com.furgnbi.android.service.START"
        const val ACTION_STOP = "com.furgnbi.android.service.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, MqttForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MqttForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var mqttRepository: MqttRepository
    @Inject lateinit var pollingCoordinator: ModbusPollingCoordinator
    @Inject lateinit var getGpsLocationUseCase: GetGpsLocationUseCase
    @Inject lateinit var appConfigRepository: AppConfigRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification("Iniciando servicios de telemetría...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTelemetryServices()

        return START_STICKY
    }

    private fun startTelemetryServices() {
        serviceScope.launch {
            val config = appConfigRepository.getConfig()

            // 1. Start Modbus polling coordinator
            pollingCoordinator.startPolling()

            // 2. Start MQTT connection
            launch {
                mqttRepository.connect()
            }

            // 3. Start GPS tracking stream
            launch {
                getGpsLocationUseCase(config.deviceId).collect { reading ->
                    Log.d(TAG, "GPS stream tick: lat=${reading.latitude}, lon=${reading.longitude}")
                }
            }

            // 4. Observe MQTT connection state to update notification
            launch {
                mqttRepository.connectionState.collectLatest { state ->
                    val statusText = when (state) {
                        MqttConnectionState.CONNECTED -> "Conectado al broker MQTT | Polling activo"
                        MqttConnectionState.CONNECTING -> "Conectando al broker MQTT..."
                        MqttConnectionState.RECONNECTING -> "Reconectando con broker MQTT..."
                        MqttConnectionState.DISCONNECTED -> "MQTT Desconectado | Monitoreo local activo"
                        MqttConnectionState.ERROR -> "Error de red MQTT | Reintentando..."
                    }
                    val updatedNotification = createNotification(statusText)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                }
            }

            // 5. Enqueue background outbox drain worker
            DrainOutboxWorker.enqueuePeriodic(applicationContext)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente de telemetría y monitoreo Modbus/MQTT"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FURGN-BI Edge Monitor")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FurgnBi:ForegroundWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L) // 24 hours max
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingCoordinator.stopPolling()
        serviceScope.launch {
            mqttRepository.disconnect()
        }
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        Log.i(TAG, "MqttForegroundService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

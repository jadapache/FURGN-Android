package com.furgnbi.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.furgnbi.android.service.DrainOutboxWorker
import com.furgnbi.android.service.MqttForegroundService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.i(TAG, "Boot completed event received. Launching telemetry service and work scheduler...")

            // Start MQTT and polling foreground service
            MqttForegroundService.startService(context)

            // Enqueue periodic outbox drain worker
            DrainOutboxWorker.enqueuePeriodic(context)
        }
    }
}

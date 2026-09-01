package com.furgnbi.android

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.furgnbi.android.data.local.PlcParameterSeeder
import com.furgnbi.android.service.MqttForegroundService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FurgnBiApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var parameterSeeder: PlcParameterSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i("FurgnBiApplication", "Application initializing...")

        // Pre-populate PLC parameters if empty (Task 11.2)
        applicationScope.launch {
            try {
                parameterSeeder.seedIfNeeded()
            } catch (e: Exception) {
                Log.e("FurgnBiApplication", "Error seeding parameters: ${e.message}", e)
            }
        }

        // Start background telemetry foreground service
        try {
            MqttForegroundService.startService(this)
        } catch (e: Exception) {
            Log.e("FurgnBiApplication", "Error starting MqttForegroundService: ${e.message}", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

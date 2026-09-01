package com.furgnbi.android.service

import android.util.Log
import com.furgnbi.android.data.repository.AppConfigRepository
import com.furgnbi.android.di.IoDispatcher
import com.furgnbi.android.domain.usecase.EvaluateAlarmsUseCase
import com.furgnbi.android.domain.usecase.ReadTelemetryUseCase
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModbusPollingCoordinator @Inject constructor(
    private val readTelemetryUseCase: ReadTelemetryUseCase,
    private val evaluateAlarmsUseCase: EvaluateAlarmsUseCase,
    private val appConfigRepository: AppConfigRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "ModbusPollingCoordinator"
    }

    private var pollingJob: Job? = null
    private val coordinatorScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        Log.i(TAG, "Starting Modbus polling loop")

        pollingJob = coordinatorScope.launch {
            while (isActive) {
                try {
                    val config = appConfigRepository.getConfig()
                    val intervalMs = config.samplingIntervalMs.coerceAtLeast(1_000L)

                    val result = readTelemetryUseCase()
                    result.onSuccess { reading ->
                        evaluateAlarmsUseCase(reading)
                    }.onFailure { e ->
                        Log.w(TAG, "Polling cycle error: ${e.message}")
                    }

                    delay(intervalMs)
                } catch (e: CancellationException) {
                    Log.i(TAG, "Modbus polling cancelled cleanly")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error in polling loop: ${e.message}", e)
                    delay(5_000L)
                }
            }
        }
    }

    fun stopPolling() {
        Log.i(TAG, "Stopping Modbus polling loop")
        pollingJob?.cancel()
        pollingJob = null
    }

    val isPolling: Boolean
        get() = pollingJob?.isActive == true
}

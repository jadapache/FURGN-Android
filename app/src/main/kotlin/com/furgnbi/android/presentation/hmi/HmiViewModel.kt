package com.furgnbi.android.presentation.hmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.furgnbi.android.data.modbus.ModbusRepository
import com.furgnbi.android.data.repository.AlarmRepository
import com.furgnbi.android.data.repository.AppConfigRepository
import com.furgnbi.android.data.repository.GpsRepository
import com.furgnbi.android.data.repository.MqttRepository
import com.furgnbi.android.data.repository.TelemetryRepository
import com.furgnbi.android.domain.model.ModbusConnectionState
import com.furgnbi.android.domain.model.MqttConnectionState
import com.furgnbi.android.domain.model.TelemetryReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HmiUiState(
    val deviceId: String = "FURG-001-AND",
    val temperature1: Float? = null,
    val temperature2: Float? = null,
    val humidity: Float? = null,
    val voltage: Float? = null,
    val fan1On: Boolean = false,
    val fan2On: Boolean = false,
    val fan3On: Boolean = false,
    val fan4On: Boolean = false,
    val fan5On: Boolean = false,
    val fan6On: Boolean = false,
    val isGateOpen: Boolean = false,
    val isGpsAvailable: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKmh: Float? = null,
    val modbusState: ModbusConnectionState = ModbusConnectionState.DISCONNECTED,
    val mqttState: MqttConnectionState = MqttConnectionState.DISCONNECTED,
    val activeAlarmCount: Int = 0,
    val lastUpdateTimestamp: Long = 0L
)

@HiltViewModel
class HmiViewModel @Inject constructor(
    private val telemetryRepository: TelemetryRepository,
    private val alarmRepository: AlarmRepository,
    private val gpsRepository: GpsRepository,
    private val mqttRepository: MqttRepository,
    private val modbusRepository: ModbusRepository,
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HmiUiState())
    val uiState: StateFlow<HmiUiState> = _uiState.asStateFlow()

    init {
        val config = appConfigRepository.getConfig()
        val deviceId = config.deviceId
        _uiState.update { it.copy(deviceId = deviceId) }

        // 1. Observe latest telemetry reading
        viewModelScope.launch {
            telemetryRepository.observeLatest(deviceId).collect { reading ->
                updateFromTelemetry(reading)
            }
        }

        // 2. Observe GPS location & availability
        viewModelScope.launch {
            gpsRepository.observeLocation(deviceId).collect { loc ->
                _uiState.update { current ->
                    current.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedKmh = loc.speedKmh,
                        isGpsAvailable = loc.gpsAvailable
                    )
                }
            }
        }

        viewModelScope.launch {
            gpsRepository.isGpsAvailable.collect { available ->
                _uiState.update { it.copy(isGpsAvailable = available) }
            }
        }

        // 3. Observe MQTT connection state
        viewModelScope.launch {
            mqttRepository.connectionState.collect { state ->
                _uiState.update { it.copy(mqttState = state) }
            }
        }

        // 4. Observe Modbus connection state
        viewModelScope.launch {
            modbusRepository.connectionState.collect { state ->
                _uiState.update { it.copy(modbusState = state) }
            }
        }

        // 5. Observe active alarm count
        viewModelScope.launch {
            alarmRepository.observeActiveCount(deviceId).collect { count ->
                _uiState.update { it.copy(activeAlarmCount = count) }
            }
        }
    }

    fun updateFromTelemetry(reading: TelemetryReading) {
        _uiState.update { current ->
            current.copy(
                temperature1 = reading.temperatureC,
                temperature2 = reading.temperature2C,
                humidity = reading.humidityPct,
                voltage = reading.voltageV,
                fan1On = reading.fan1On,
                fan2On = reading.fan2On,
                fan3On = reading.fan3On,
                fan4On = reading.fan4On,
                fan5On = reading.fan5On,
                fan6On = reading.fan6On,
                isGateOpen = reading.gateOpen,
                lastUpdateTimestamp = reading.capturedAt
            )
        }
    }
}

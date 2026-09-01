package com.furgnbi.android.presentation.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.furgnbi.android.data.repository.AppConfigRepository
import com.furgnbi.android.domain.model.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfigUiState(
    val deviceId: String = "",
    val brokerUrl: String = "",
    val mqttUsername: String = "",
    val mqttPassword: String = "",
    val mqttClientId: String = "",
    val modbusHost: String = "",
    val modbusPort: String = "502",
    val modbusUnitId: String = "1",
    val samplingIntervalMs: String = "5000",
    val publishIntervalMs: String = "10000",
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        val config = appConfigRepository.getConfig()
        _uiState.update {
            it.copy(
                deviceId = config.deviceId,
                brokerUrl = config.brokerUrl,
                mqttUsername = config.mqttUsername,
                mqttPassword = config.mqttPassword,
                mqttClientId = config.mqttClientId,
                modbusHost = config.modbusHost,
                modbusPort = config.modbusPort.toString(),
                modbusUnitId = config.modbusUnitId.toString(),
                samplingIntervalMs = config.samplingIntervalMs.toString(),
                publishIntervalMs = config.publishIntervalMs.toString(),
                isSaved = false,
                errorMessage = null
            )
        }
    }

    fun updateDeviceId(v: String) = _uiState.update { it.copy(deviceId = v, isSaved = false) }
    fun updateBrokerUrl(v: String) = _uiState.update { it.copy(brokerUrl = v, isSaved = false) }
    fun updateMqttUsername(v: String) = _uiState.update { it.copy(mqttUsername = v, isSaved = false) }
    fun updateMqttPassword(v: String) = _uiState.update { it.copy(mqttPassword = v, isSaved = false) }
    fun updateMqttClientId(v: String) = _uiState.update { it.copy(mqttClientId = v, isSaved = false) }
    fun updateModbusHost(v: String) = _uiState.update { it.copy(modbusHost = v, isSaved = false) }
    fun updateModbusPort(v: String) = _uiState.update { it.copy(modbusPort = v, isSaved = false) }
    fun updateModbusUnitId(v: String) = _uiState.update { it.copy(modbusUnitId = v, isSaved = false) }
    fun updateSamplingInterval(v: String) = _uiState.update { it.copy(samplingIntervalMs = v, isSaved = false) }
    fun updatePublishInterval(v: String) = _uiState.update { it.copy(publishIntervalMs = v, isSaved = false) }

    fun saveConfig(): Boolean {
        val state = _uiState.value

        if (state.deviceId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El ID del dispositivo no puede estar vacío") }
            return false
        }
        if (state.brokerUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "La URL del broker no puede estar vacía") }
            return false
        }
        if (state.modbusHost.isBlank()) {
            _uiState.update { it.copy(errorMessage = "La IP del PLC no puede estar vacía") }
            return false
        }

        val modbusPort = state.modbusPort.toIntOrNull() ?: 502
        val modbusUnitId = state.modbusUnitId.toIntOrNull() ?: 1
        val samplingInterval = state.samplingIntervalMs.toLongOrNull() ?: 5000L
        val publishInterval = state.publishIntervalMs.toLongOrNull() ?: 10000L

        val config = AppConfig(
            deviceId = state.deviceId.trim(),
            brokerUrl = state.brokerUrl.trim(),
            mqttUsername = state.mqttUsername.trim(),
            mqttPassword = state.mqttPassword.trim(),
            mqttClientId = state.mqttClientId.trim(),
            modbusHost = state.modbusHost.trim(),
            modbusPort = modbusPort,
            modbusUnitId = modbusUnitId,
            samplingIntervalMs = samplingInterval,
            publishIntervalMs = publishInterval
        )

        appConfigRepository.saveConfig(config)
        _uiState.update { it.copy(isSaved = true, errorMessage = null) }
        return true
    }
}

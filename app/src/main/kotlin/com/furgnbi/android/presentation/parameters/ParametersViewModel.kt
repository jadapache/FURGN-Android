package com.furgnbi.android.presentation.parameters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.furgnbi.android.data.repository.ParameterRepository
import com.furgnbi.android.domain.model.PlcParameter
import com.furgnbi.android.domain.model.ValidationResult
import com.furgnbi.android.domain.usecase.RangeValidator
import com.furgnbi.android.domain.usecase.ReadAllParametersUseCase
import com.furgnbi.android.domain.usecase.WriteParameterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParameterUiState(
    val parameter: PlcParameter,
    val inputString: String = "",
    val validationError: String? = null,
    val isDirty: Boolean = false,
    val isWriting: Boolean = false,
    val lastWriteSuccess: Boolean? = null
)

data class ParametersScreenUiState(
    val parameters: List<ParameterUiState> = emptyList(),
    val isLoading: Boolean = false,
    val isWritingAll: Boolean = false,
    val feedbackMessage: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class ParametersViewModel @Inject constructor(
    private val parameterRepository: ParameterRepository,
    private val readAllParametersUseCase: ReadAllParametersUseCase,
    private val writeParameterUseCase: WriteParameterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParametersScreenUiState())
    val uiState: StateFlow<ParametersScreenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            parameterRepository.observeParameters().collect { list ->
                _uiState.update { current ->
                    val currentMap = current.parameters.associateBy { it.parameter.id }
                    val newItems = list.map { param ->
                        val existing = currentMap[param.id]
                        if (existing != null && existing.isDirty) {
                            existing.copy(parameter = param)
                        } else {
                            ParameterUiState(
                                parameter = param,
                                inputString = param.currentValue?.let { formatValue(it, param) } ?: ""
                            )
                        }
                    }
                    current.copy(parameters = newItems)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onInputChange(parameterId: Int, input: String) {
        _uiState.update { current ->
            val updated = current.parameters.map { item ->
                if (item.parameter.id == parameterId) {
                    val parsed = input.toDoubleOrNull()
                    val error = if (input.isBlank()) {
                        "Campo requerido"
                    } else if (parsed == null) {
                        "Valor numérico inválido"
                    } else {
                        when (val res = RangeValidator.validate(item.parameter, parsed)) {
                            is ValidationResult.Error -> res.message
                            is ValidationResult.Success -> null
                        }
                    }

                    val isDirty = parsed != null && parsed != item.parameter.currentValue
                    item.copy(
                        inputString = input,
                        validationError = error,
                        isDirty = isDirty
                    )
                } else {
                    item
                }
            }
            current.copy(parameters = updated)
        }
    }

    fun onReadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, feedbackMessage = "Leyendo 23 parámetros del PLC...") }
            val result = readAllParametersUseCase()
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    feedbackMessage = if (result.isSuccess) "Parámetros actualizados con éxito" else "Error al leer parámetros: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun onWriteParameter(parameterId: Int) {
        val item = _uiState.value.parameters.find { it.parameter.id == parameterId } ?: return
        val value = item.inputString.toDoubleOrNull() ?: return
        if (item.validationError != null || item.parameter.isReadOnly) return

        viewModelScope.launch {
            setItemWriting(parameterId, true)
            when (val result = writeParameterUseCase(item.parameter, value)) {
                is WriteParameterUseCase.WriteResult.Success -> {
                    _uiState.update { current ->
                        val updated = current.parameters.map {
                            if (it.parameter.id == parameterId) {
                                it.copy(
                                    isWriting = false,
                                    isDirty = false,
                                    lastWriteSuccess = true,
                                    validationError = null
                                )
                            } else it
                        }
                        current.copy(
                            parameters = updated,
                            feedbackMessage = "Parámetro ${item.parameter.name} escrito y confirmado"
                        )
                    }
                }
                is WriteParameterUseCase.WriteResult.ValidationError -> {
                    setItemWriting(parameterId, false)
                    _uiState.update { current ->
                        val updated = current.parameters.map {
                            if (it.parameter.id == parameterId) {
                                it.copy(validationError = result.message, lastWriteSuccess = false)
                            } else it
                        }
                        current.copy(parameters = updated)
                    }
                }
                is WriteParameterUseCase.WriteResult.WriteError -> {
                    setItemWriting(parameterId, false)
                    _uiState.update { current ->
                        val updated = current.parameters.map {
                            if (it.parameter.id == parameterId) {
                                it.copy(lastWriteSuccess = false)
                            } else it
                        }
                        current.copy(
                            parameters = updated,
                            feedbackMessage = "Error al escribir ${item.parameter.name}: ${result.message}"
                        )
                    }
                }
                is WriteParameterUseCase.WriteResult.EchoMismatch -> {
                    setItemWriting(parameterId, false)
                    _uiState.update { current ->
                        current.copy(feedbackMessage = "Discrepancia en eco: enviado ${result.sentValue}, leído ${result.echoValue}")
                    }
                }
            }
        }
    }

    fun onSaveAll() {
        val dirtyItems = _uiState.value.parameters.filter { it.isDirty && it.validationError == null && !it.parameter.isReadOnly }
        if (dirtyItems.isEmpty()) {
            _uiState.update { it.copy(feedbackMessage = "No hay cambios pendientes válidos para guardar") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isWritingAll = true, feedbackMessage = "Guardando ${dirtyItems.size} parámetros...") }
            var successCount = 0
            for (item in dirtyItems) {
                val value = item.inputString.toDoubleOrNull() ?: continue
                val res = writeParameterUseCase(item.parameter, value)
                if (res is WriteParameterUseCase.WriteResult.Success) {
                    successCount++
                }
            }
            _uiState.update { current ->
                current.copy(
                    isWritingAll = false,
                    feedbackMessage = "Se guardaron $successCount de ${dirtyItems.size} parámetros exitosamente"
                )
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    private fun setItemWriting(parameterId: Int, isWriting: Boolean) {
        _uiState.update { current ->
            val updated = current.parameters.map {
                if (it.parameter.id == parameterId) it.copy(isWriting = isWriting) else it
            }
            current.copy(parameters = updated)
        }
    }

    private fun formatValue(value: Double, param: PlcParameter): String {
        return if (param.registerCount == 1 && value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format("%.2f", value)
        }
    }
}

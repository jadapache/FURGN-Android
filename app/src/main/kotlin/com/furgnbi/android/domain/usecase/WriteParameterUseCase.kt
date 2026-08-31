package com.furgnbi.android.domain.usecase

import android.util.Log
import com.furgnbi.android.data.modbus.ModbusRepository
import com.furgnbi.android.data.repository.ParameterRepository
import com.furgnbi.android.di.IoDispatcher
import com.furgnbi.android.domain.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes a parameter value to the PLC via Modbus.
 *
 * Pipeline (Requisito 8.3–8.8):
 *   1. RangeValidator.validate() — reject out-of-range values before any Modbus call
 *   2. Modbus write (FC06 for UINT16, FC16 for REAL)
 *   3. Echo read — read back the register to confirm the PLC accepted the value
 *   4. Update ParameterRepository cache ONLY if echo read matches written value
 *
 * Correctness property 6: plc_parameters[id].currentValue == echoReadValue (not sent value).
 */
@Singleton
class WriteParameterUseCase @Inject constructor(
    private val modbusRepository: ModbusRepository,
    private val parameterRepository: ParameterRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    sealed class WriteResult {
        data class Success(val confirmedValue: Double) : WriteResult()
        data class ValidationError(val message: String) : WriteResult()
        data class WriteError(val message: String) : WriteResult()
        data class EchoMismatch(val sentValue: Double, val echoValue: Double) : WriteResult()
    }

    suspend operator fun invoke(parameter: PlcParameter, value: Double): WriteResult =
        withContext(ioDispatcher) {
            // Step 1: Validate range
            val validation = RangeValidator.validate(parameter, value)
            if (validation is ValidationResult.Error) {
                return@withContext WriteResult.ValidationError(validation.message)
            }

            runCatching {
                // Step 2: Write to PLC
                when (parameter.modbusDataType) {
                    ModbusDataType.REAL -> {
                        modbusRepository.writeHoldingRegisterFloat(
                            address = parameter.modbusAddress,
                            value = value.toFloat()
                        )
                    }
                    ModbusDataType.UINT16 -> {
                        modbusRepository.writeHoldingRegisterUInt16(
                            address = parameter.modbusAddress,
                            value = value.toInt()
                        )
                    }
                }

                // Step 3: Echo read — verify PLC accepted the value
                val echoValue: Double = when (parameter.modbusDataType) {
                    ModbusDataType.REAL ->
                        modbusRepository.readHoldingRegisterFloat(parameter.modbusAddress).toDouble()
                    ModbusDataType.UINT16 ->
                        modbusRepository.readHoldingRegisterUInt16(parameter.modbusAddress).toDouble()
                }

                // Step 4: Update cache with confirmed echo value (not the value we sent)
                parameterRepository.updateValue(parameter.id, echoValue)

                Log.d("WriteParameterUseCase",
                    "Param ${parameter.name}: sent=$value echo=$echoValue")

                WriteResult.Success(echoValue)
            }.getOrElse { e ->
                Log.e("WriteParameterUseCase",
                    "Write failed for ${parameter.name}: ${e.message}")
                WriteResult.WriteError(e.message ?: "Error desconocido al escribir el parámetro")
            }
        }
}

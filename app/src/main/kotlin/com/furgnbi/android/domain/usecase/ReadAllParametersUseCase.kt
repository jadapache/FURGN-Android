package com.furgnbi.android.domain.usecase

import android.util.Log
import com.furgnbi.android.data.modbus.ModbusRepository
import com.furgnbi.android.data.repository.ParameterRepository
import com.furgnbi.android.di.IoDispatcher
import com.furgnbi.android.domain.model.ModbusDataType
import com.furgnbi.android.domain.model.PlcParameter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadAllParametersUseCase @Inject constructor(
    private val modbusRepository: ModbusRepository,
    private val parameterRepository: ParameterRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "ReadAllParamsUseCase"
    }

    suspend operator fun invoke(): Result<List<PlcParameter>> = withContext(ioDispatcher) {
        runCatching {
            val parameters = parameterRepository.getAllParameters()
            if (parameters.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val updatedList = mutableListOf<PlcParameter>()
            val now = System.currentTimeMillis()

            for (param in parameters) {
                try {
                    val value: Double = when (param.modbusDataType) {
                        ModbusDataType.REAL -> {
                            modbusRepository.readHoldingRegisterFloat(param.modbusAddress).toDouble()
                        }
                        ModbusDataType.UINT16 -> {
                            modbusRepository.readHoldingRegisterUInt16(param.modbusAddress).toDouble()
                        }
                    }

                    val updatedParam = param.copy(
                        currentValue = value,
                        lastReadAt = now
                    )
                    parameterRepository.updateParameter(updatedParam)
                    updatedList.add(updatedParam)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed reading param ${param.name} at ${param.modbusAddress}: ${e.message}")
                    updatedList.add(param)
                }
            }

            Log.i(TAG, "Finished reading ${updatedList.size} PLC parameters")
            updatedList
        }
    }
}

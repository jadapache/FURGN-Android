package com.furgnbi.android.domain.usecase

import com.furgnbi.android.domain.model.PlcParameter
import com.furgnbi.android.domain.model.ValidationResult

/**
 * Validates a candidate value against a PlcParameter's declared range and read-only flag.
 *
 * Correctness guarantee (Requisito 8.3, 8.4):
 * - Returns ValidationResult.Error for ANY value strictly outside [min, max].
 * - Returns ValidationResult.Error for read-only parameters regardless of value.
 * - Returns ValidationResult.Success only when value is within [min, max] and parameter is writable.
 */
object RangeValidator {

    fun validate(parameter: PlcParameter, value: Double): ValidationResult {
        if (parameter.isReadOnly) {
            return ValidationResult.Error(
                "El parámetro '${parameter.name}' es de solo lectura y no puede ser modificado."
            )
        }

        if (value < parameter.minValue) {
            return ValidationResult.Error(
                "El valor $value es menor que el mínimo permitido (${parameter.minValue} ${parameter.unit})."
            )
        }

        if (value > parameter.maxValue) {
            return ValidationResult.Error(
                "El valor $value supera el máximo permitido (${parameter.maxValue} ${parameter.unit})."
            )
        }

        return ValidationResult.Success
    }

    /**
     * Returns true if the value is valid (within range and parameter is writable).
     */
    fun isValid(parameter: PlcParameter, value: Double): Boolean =
        validate(parameter, value) is ValidationResult.Success
}

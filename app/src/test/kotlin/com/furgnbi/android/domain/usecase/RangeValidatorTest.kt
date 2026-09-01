package com.furgnbi.android.domain.usecase

import com.furgnbi.android.domain.model.ModbusDataType
import com.furgnbi.android.domain.model.PlcParameter
import com.furgnbi.android.domain.model.ValidationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RangeValidatorTest {

    private val sampleParameter = PlcParameter(
        id = 1,
        name = "Setpoint Temp Zona 1",
        modbusAddress = 40001,
        modbusDataType = ModbusDataType.REAL,
        registerCount = 2,
        minValue = -30.0,
        maxValue = 30.0,
        unit = "°C",
        defaultValue = -18.0,
        isReadOnly = false
    )

    private val readOnlyParameter = PlcParameter(
        id = 23,
        name = "Versión Firmware",
        modbusAddress = 40035,
        modbusDataType = ModbusDataType.UINT16,
        registerCount = 1,
        minValue = 0.0,
        maxValue = 9999.0,
        unit = "v",
        defaultValue = 100.0,
        isReadOnly = true
    )

    @Test
    fun testValidValuesAccepted() {
        assertTrue(RangeValidator.validate(sampleParameter, -30.0) is ValidationResult.Success)
        assertTrue(RangeValidator.validate(sampleParameter, 0.0) is ValidationResult.Success)
        assertTrue(RangeValidator.validate(sampleParameter, 30.0) is ValidationResult.Success)
        assertTrue(RangeValidator.validate(sampleParameter, -18.0) is ValidationResult.Success)
    }

    @Test
    fun testOutOfRangeRejected() {
        assertTrue(RangeValidator.validate(sampleParameter, -30.1) is ValidationResult.Error)
        assertTrue(RangeValidator.validate(sampleParameter, -100.0) is ValidationResult.Error)
        assertTrue(RangeValidator.validate(sampleParameter, 30.001) is ValidationResult.Error)
        assertTrue(RangeValidator.validate(sampleParameter, 50.0) is ValidationResult.Error)
    }

    @Test
    fun testReadOnlyParameterAlwaysRejected() {
        val result = RangeValidator.validate(readOnlyParameter, 100.0)
        assertTrue(result is ValidationResult.Error)
        assertFalse(RangeValidator.isValid(readOnlyParameter, 100.0))
    }

    @Test
    fun testRandomPropertyTesting1000Iterations() {
        val random = Random(123)
        for (i in 0 until 1000) {
            val min = random.nextDouble(-100.0, 0.0)
            val max = random.nextDouble(1.0, 100.0)
            val param = sampleParameter.copy(minValue = min, maxValue = max, isReadOnly = false)

            // Value inside
            val insideVal = random.nextDouble(min, max)
            assertTrue("Inside value $insideVal should be valid for [$min, $max]", RangeValidator.isValid(param, insideVal))

            // Value below min
            val belowVal = min - random.nextDouble(0.01, 50.0)
            assertFalse("Below value $belowVal should be invalid for [$min, $max]", RangeValidator.isValid(param, belowVal))

            // Value above max
            val aboveVal = max + random.nextDouble(0.01, 50.0)
            assertFalse("Above value $aboveVal should be invalid for [$min, $max]", RangeValidator.isValid(param, aboveVal))
        }
    }
}

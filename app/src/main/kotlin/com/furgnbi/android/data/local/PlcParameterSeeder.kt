package com.furgnbi.android.data.local

// Nota de arquitectura (Gap 3): PlcParameterSeeder reside en data/local/ porque su única
// responsabilidad es insertar datos iniciales en Room. No se comunica directamente con el
// PLC vía Modbus, por lo que data/modbus/ no sería semánticamente correcto.
// El seeder es invocado desde FurgnBiApplication.onCreate() antes del primer ciclo de polling.

import android.util.Log
import com.furgnbi.android.data.local.dao.PlcParameterDao
import com.furgnbi.android.data.local.entity.PlcParameterEntity
import com.furgnbi.android.domain.model.ModbusDataType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlcParameterSeeder @Inject constructor(
    private val plcParameterDao: PlcParameterDao
) {
    companion object {
        private const val TAG = "PlcParameterSeeder"

        val DEFAULT_PARAMETERS: List<PlcParameterEntity> = listOf(
            // Parámetros 1 y 2: rango avícola (10–45 °C) para transporte de pollitos de un día
            // (Gap 4: corregido de rango frigorífico -30/30 °C al rango del blueprint)
            PlcParameterEntity(
                id = 1,
                name = "Setpoint Temp Zona 1",
                modbusAddress = 40001,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = 10.0,
                maxValue = 45.0,
                unit = "°C",
                defaultValue = 25.0,
                currentValue = 25.0,
                isReadOnly = false,
                description = "Temperatura objetivo zona 1 — rango avícola (pollitos 1 día)"
            ),
            PlcParameterEntity(
                id = 2,
                name = "Setpoint Temp Zona 2",
                modbusAddress = 40003,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = 10.0,
                maxValue = 45.0,
                unit = "°C",
                defaultValue = 25.0,
                currentValue = 25.0,
                isReadOnly = false,
                description = "Temperatura objetivo zona 2 — rango avícola (pollitos 1 día)"
            ),
            PlcParameterEntity(
                id = 3,
                name = "Setpoint Humedad",
                modbusAddress = 40005,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = 0.0,
                maxValue = 100.0,
                unit = "%",
                defaultValue = 85.0,
                currentValue = 85.0,
                isReadOnly = false,
                description = "Humedad relativa objetivo"
            ),
            PlcParameterEntity(
                id = 4,
                name = "Diferencial Temp",
                modbusAddress = 40007,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = 0.5,
                maxValue = 10.0,
                unit = "°C",
                defaultValue = 2.0,
                currentValue = 2.0,
                isReadOnly = false,
                description = "Histéresis para encendido/apagado del compresor"
            ),
            PlcParameterEntity(
                id = 5,
                name = "Tiempo Descongelamiento",
                modbusAddress = 40009,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 5.0,
                maxValue = 120.0,
                unit = "min",
                defaultValue = 30.0,
                currentValue = 30.0,
                isReadOnly = false,
                description = "Duración del ciclo de defrost"
            ),
            PlcParameterEntity(
                id = 6,
                name = "Intervalo Descongelamiento",
                modbusAddress = 40010,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 1.0,
                maxValue = 24.0,
                unit = "h",
                defaultValue = 6.0,
                currentValue = 6.0,
                isReadOnly = false,
                description = "Frecuencia de ciclos de defrost"
            ),
            PlcParameterEntity(
                id = 7,
                name = "Umbral Voltaje Bajo",
                modbusAddress = 40011,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = 10.0,
                maxValue = 30.0,
                unit = "V",
                defaultValue = 22.0,
                currentValue = 22.0,
                isReadOnly = false,
                description = "Voltaje mínimo de batería antes de alarma"
            ),
            PlcParameterEntity(
                id = 8,
                name = "Umbral Voltaje Alto",
                modbusAddress = 40013,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = 20.0,
                maxValue = 40.0,
                unit = "V",
                defaultValue = 28.5,
                currentValue = 28.5,
                isReadOnly = false,
                description = "Voltaje máximo de alternador antes de alarma"
            ),
            PlcParameterEntity(
                id = 9,
                name = "Retardo Encendido Compresor",
                modbusAddress = 40015,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 0.0,
                maxValue = 600.0,
                unit = "s",
                defaultValue = 60.0,
                currentValue = 60.0,
                isReadOnly = false,
                description = "Protección anti-reciclaje del compresor"
            ),
            PlcParameterEntity(
                id = 10,
                name = "Retardo Apagado Ventilador",
                modbusAddress = 40016,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 0.0,
                maxValue = 300.0,
                unit = "s",
                defaultValue = 30.0,
                currentValue = 30.0,
                isReadOnly = false,
                description = "Post-enfriamiento de evaporador"
            ),
            PlcParameterEntity(
                id = 11,
                name = "Calibración Sensor T1",
                modbusAddress = 40017,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = -5.0,
                maxValue = 5.0,
                unit = "°C",
                defaultValue = 0.0,
                currentValue = 0.0,
                isReadOnly = false,
                description = "Offset de calibración para sonda 1"
            ),
            PlcParameterEntity(
                id = 12,
                name = "Calibración Sensor T2",
                modbusAddress = 40019,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = -5.0,
                maxValue = 5.0,
                unit = "°C",
                defaultValue = 0.0,
                currentValue = 0.0,
                isReadOnly = false,
                description = "Offset de calibración para sonda 2"
            ),
            PlcParameterEntity(
                id = 13,
                name = "Calibración Humedad",
                modbusAddress = 40021,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = -10.0,
                maxValue = 10.0,
                unit = "%",
                defaultValue = 0.0,
                currentValue = 0.0,
                isReadOnly = false,
                description = "Offset de calibración sensor humedad"
            ),
            PlcParameterEntity(
                id = 14,
                name = "Velocidad Ventilador 1",
                modbusAddress = 40023,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 0.0,
                maxValue = 100.0,
                unit = "%",
                defaultValue = 100.0,
                currentValue = 100.0,
                isReadOnly = false,
                description = "Porcentaje de velocidad ventilador primario"
            ),
            PlcParameterEntity(
                id = 15,
                name = "Velocidad Ventilador 2",
                modbusAddress = 40024,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 0.0,
                maxValue = 100.0,
                unit = "%",
                defaultValue = 100.0,
                currentValue = 100.0,
                isReadOnly = false,
                description = "Porcentaje de velocidad ventilador secundario"
            ),
            PlcParameterEntity(
                id = 16,
                name = "Modo Operación",
                modbusAddress = 40025,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 0.0,
                maxValue = 1.0,
                unit = "",
                defaultValue = 0.0,
                currentValue = 0.0,
                isReadOnly = false,
                description = "0 = Automático, 1 = Manual"
            ),
            PlcParameterEntity(
                id = 17,
                name = "Alarma Temp Baja",
                modbusAddress = 40026,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = -40.0,
                maxValue = 0.0,
                unit = "°C",
                defaultValue = -25.0,
                currentValue = -25.0,
                isReadOnly = false,
                description = "Umbral para disparo de alarma temperatura baja"
            ),
            PlcParameterEntity(
                id = 18,
                name = "Alarma Temp Alta",
                modbusAddress = 40028,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = 0.0,
                maxValue = 50.0,
                unit = "°C",
                defaultValue = 35.0,
                currentValue = 35.0,
                isReadOnly = false,
                description = "Umbral para disparo de alarma temperatura alta"
            ),
            PlcParameterEntity(
                id = 19,
                name = "Alarma Humedad Alta",
                modbusAddress = 40030,
                modbusDataType = ModbusDataType.REAL.name,
                registerCount = 2,
                minValue = 50.0,
                maxValue = 100.0,
                unit = "%",
                defaultValue = 80.0,
                currentValue = 80.0,
                isReadOnly = false,
                description = "Umbral de humedad para alarma de condensación"
            ),
            PlcParameterEntity(
                id = 20,
                name = "Tiempo Alarma Puerta",
                modbusAddress = 40032,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 10.0,
                maxValue = 600.0,
                unit = "s",
                defaultValue = 120.0,
                currentValue = 120.0,
                isReadOnly = false,
                description = "Segundos con compuerta abierta antes de alarma"
            ),
            PlcParameterEntity(
                id = 21,
                name = "Habilitación Modbus TCP",
                modbusAddress = 40033,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 0.0,
                maxValue = 1.0,
                unit = "",
                defaultValue = 1.0,
                currentValue = 1.0,
                isReadOnly = false,
                description = "1 = Habilitado, 0 = Deshabilitado"
            ),
            PlcParameterEntity(
                id = 22,
                name = "Intervalo Registro Local",
                modbusAddress = 40034,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 1.0,
                maxValue = 3600.0,
                unit = "s",
                defaultValue = 5.0,
                currentValue = 5.0,
                isReadOnly = false,
                description = "Intervalo de muestreo y almacenamiento local"
            ),
            PlcParameterEntity(
                id = 23,
                name = "Versión Firmware PLC",
                modbusAddress = 40035,
                modbusDataType = ModbusDataType.UINT16.name,
                registerCount = 1,
                minValue = 0.0,
                maxValue = 9999.0,
                unit = "v",
                defaultValue = 100.0,
                currentValue = 100.0,
                isReadOnly = true,
                description = "Versión de firmware del PLC (Solo lectura)"
            )
        )
    }

    suspend fun seedIfNeeded() {
        val count = plcParameterDao.count()
        if (count == 0) {
            Log.i(TAG, "Seeding ${DEFAULT_PARAMETERS.size} PLC parameters...")
            plcParameterDao.insertAll(DEFAULT_PARAMETERS)
            Log.i(TAG, "PLC parameters seeded successfully.")
        }
    }
}

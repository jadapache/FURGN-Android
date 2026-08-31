package com.furgnbi.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.furgnbi.android.domain.model.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AppConfigRepository {
    fun getConfig(): AppConfig
    fun saveConfig(config: AppConfig)
    fun getValue(key: String): String?
    fun setValue(key: String, value: String)
}

@Singleton
class AppConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppConfigRepository {

    companion object {
        private const val PREFS_FILE = "furgn_bi_config"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_BROKER_URL = "broker_url"
        private const val KEY_MQTT_USERNAME = "mqtt_username"
        private const val KEY_MQTT_PASSWORD = "mqtt_password"
        private const val KEY_MQTT_CLIENT_ID = "mqtt_client_id"
        private const val KEY_MODBUS_HOST = "modbus_host"
        private const val KEY_MODBUS_PORT = "modbus_port"
        private const val KEY_MODBUS_UNIT_ID = "modbus_unit_id"
        private const val KEY_SAMPLING_INTERVAL = "sampling_interval_ms"
        private const val KEY_PUBLISH_INTERVAL = "publish_interval_ms"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val keyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_FILE,
                keyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to plain SharedPreferences in test/emulator environments
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    override fun getConfig(): AppConfig = AppConfig(
        deviceId = prefs.getString(KEY_DEVICE_ID, "FURG-001-AND") ?: "FURG-001-AND",
        brokerUrl = prefs.getString(KEY_BROKER_URL, "tcp://broker.furgnbi.com:1883") ?: "tcp://broker.furgnbi.com:1883",
        mqttUsername = prefs.getString(KEY_MQTT_USERNAME, "") ?: "",
        mqttPassword = prefs.getString(KEY_MQTT_PASSWORD, "") ?: "",
        mqttClientId = prefs.getString(KEY_MQTT_CLIENT_ID, "android_${System.currentTimeMillis()}") ?: "android_${System.currentTimeMillis()}",
        modbusHost = prefs.getString(KEY_MODBUS_HOST, "192.168.1.100") ?: "192.168.1.100",
        modbusPort = prefs.getInt(KEY_MODBUS_PORT, 502),
        modbusUnitId = prefs.getInt(KEY_MODBUS_UNIT_ID, 1),
        samplingIntervalMs = prefs.getLong(KEY_SAMPLING_INTERVAL, 5_000L),
        publishIntervalMs = prefs.getLong(KEY_PUBLISH_INTERVAL, 10_000L)
    )

    override fun saveConfig(config: AppConfig) {
        prefs.edit().apply {
            putString(KEY_DEVICE_ID, config.deviceId)
            putString(KEY_BROKER_URL, config.brokerUrl)
            putString(KEY_MQTT_USERNAME, config.mqttUsername)
            putString(KEY_MQTT_PASSWORD, config.mqttPassword)
            putString(KEY_MQTT_CLIENT_ID, config.mqttClientId)
            putString(KEY_MODBUS_HOST, config.modbusHost)
            putInt(KEY_MODBUS_PORT, config.modbusPort)
            putInt(KEY_MODBUS_UNIT_ID, config.modbusUnitId)
            putLong(KEY_SAMPLING_INTERVAL, config.samplingIntervalMs)
            putLong(KEY_PUBLISH_INTERVAL, config.publishIntervalMs)
            apply()
        }
    }

    override fun getValue(key: String): String? = prefs.getString(key, null)

    override fun setValue(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}

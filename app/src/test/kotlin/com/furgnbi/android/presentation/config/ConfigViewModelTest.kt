package com.furgnbi.android.presentation.config

import com.furgnbi.android.data.repository.AppConfigRepository
import com.furgnbi.android.domain.model.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigViewModelTest {

    class FakeAppConfigRepository : AppConfigRepository {
        var storedConfig = AppConfig()

        override fun getConfig(): AppConfig = storedConfig
        override fun saveConfig(config: AppConfig) { storedConfig = config }
        override fun getValue(key: String): String? = null
        override fun setValue(key: String, value: String) {}
    }

    @Test
    fun testLoadAndSaveConfig() {
        val repo = FakeAppConfigRepository()
        val vm = ConfigViewModel(repo)

        assertEquals("FURG-001-AND", vm.uiState.value.deviceId)

        vm.updateDeviceId("TRUCK-999")
        vm.updateModbusHost("10.0.0.50")
        vm.updateBrokerUrl("ssl://mqtt.mybroker.com:8883")

        val saved = vm.saveConfig()
        assertTrue(saved)
        assertTrue(vm.uiState.value.isSaved)
        assertEquals("TRUCK-999", repo.storedConfig.deviceId)
        assertEquals("10.0.0.50", repo.storedConfig.modbusHost)
        assertEquals("ssl://mqtt.mybroker.com:8883", repo.storedConfig.brokerUrl)
    }

    @Test
    fun testValidationBlocksBlankFields() {
        val repo = FakeAppConfigRepository()
        val vm = ConfigViewModel(repo)

        vm.updateDeviceId("")
        val saved = vm.saveConfig()

        assertFalse(saved)
        assertFalse(vm.uiState.value.isSaved)
        assertTrue(vm.uiState.value.errorMessage != null)
    }
}

package com.furgnbi.android.presentation.hmi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.furgnbi.android.presentation.hmi.components.StatusBar
import com.furgnbi.android.presentation.hmi.components.TelemetryGaugesRow
import com.furgnbi.android.presentation.hmi.components.TruckCanvas
import com.furgnbi.android.presentation.theme.*

@Composable
fun HmiScreen(
    viewModel: HmiViewModel,
    onNavigateToParameters: () -> Unit,
    onNavigateToConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            StatusBar(
                deviceId = state.deviceId,
                modbusState = state.modbusState,
                mqttState = state.mqttState,
                isGpsAvailable = state.isGpsAvailable,
                latitude = state.latitude,
                longitude = state.longitude,
                speedKmh = state.speedKmh,
                alarmCount = state.activeAlarmCount,
                onAlarmsClick = onNavigateToParameters,
                onConfigClick = onNavigateToConfig
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceDark,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MICRO 820 • Modbus TCP Port 502 • QoS 1",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onNavigateToParameters,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = BackgroundDark
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Parametrización PLC",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToConfig,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Configuración",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Gauges for real-time sensor metrics
            TelemetryGaugesRow(
                temperature1 = state.temperature1,
                temperature2 = state.temperature2,
                humidity = state.humidity,
                voltage = state.voltage
            )

            // 2. Interactive Truck Visualizer with Fans and Door indicator
            TruckCanvas(
                fan1 = state.fan1On,
                fan2 = state.fan2On,
                fan3 = state.fan3On,
                fan4 = state.fan4On,
                fan5 = state.fan5On,
                fan6 = state.fan6On,
                gateOpen = state.isGateOpen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
